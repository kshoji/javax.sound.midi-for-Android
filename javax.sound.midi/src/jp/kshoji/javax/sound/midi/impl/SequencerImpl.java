package jp.kshoji.javax.sound.midi.impl;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.SparseArray;
import android.util.SparseBooleanArray;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jp.kshoji.javax.sound.midi.ControllerEventListener;
import jp.kshoji.javax.sound.midi.InvalidMidiDataException;
import jp.kshoji.javax.sound.midi.MetaEventListener;
import jp.kshoji.javax.sound.midi.MetaMessage;
import jp.kshoji.javax.sound.midi.MidiEvent;
import jp.kshoji.javax.sound.midi.MidiMessage;
import jp.kshoji.javax.sound.midi.MidiSystem.MidiSystemUtils;
import jp.kshoji.javax.sound.midi.MidiUnavailableException;
import jp.kshoji.javax.sound.midi.Receiver;
import jp.kshoji.javax.sound.midi.Sequence;
import jp.kshoji.javax.sound.midi.Sequencer;
import jp.kshoji.javax.sound.midi.ShortMessage;
import jp.kshoji.javax.sound.midi.SysexMessage;
import jp.kshoji.javax.sound.midi.Track;
import jp.kshoji.javax.sound.midi.Track.TrackUtils;
import jp.kshoji.javax.sound.midi.Transmitter;
import jp.kshoji.javax.sound.midi.io.StandardMidiFileReader;

/**
 * {@link jp.kshoji.javax.sound.midi.Sequencer} implementation
 *
 * @author K.Shoji
 */
public class SequencerImpl implements Sequencer {
    private static final SyncMode[] MASTER_SYNC_MODES = new SyncMode[]{SyncMode.INTERNAL_CLOCK};
    private static final SyncMode[] SLAVE_SYNC_MODES = new SyncMode[]{SyncMode.NO_SYNC};

    final List<Transmitter> transmitters = new ArrayList<Transmitter>();
    final List<Receiver> receivers = new ArrayList<Receiver>();
    final Set<MetaEventListener> metaEventListeners = new HashSet<MetaEventListener>();
    final SparseArray<Set<ControllerEventListener>> controllerEventListenerMap = new SparseArray<Set<ControllerEventListener>>();
    final Map<Track, Set<Integer>> recordEnable = new HashMap<Track, Set<Integer>>();
    SequencerThread sequencerThread;
    Sequence sequence = null;
    private volatile boolean isOpen = false;
    private int loopCount = 0;
    private long loopStartPoint = 0;
    private long loopEndPoint = -1;
    volatile float tempoFactor = 1.0f;
    private SyncMode masterSyncMode = SyncMode.INTERNAL_CLOCK;
    private SyncMode slaveSyncMode = SyncMode.NO_SYNC;
    private final SparseBooleanArray trackMute = new SparseBooleanArray();
    private final SparseBooleanArray trackSolo = new SparseBooleanArray();
    private float tempoInBPM = 120f;

    volatile boolean isRunning = false;
    volatile boolean isRecording = false;

    /**
     * Thread for this Sequencer
     *
     * @author K.Shoji
     */
    private class SequencerThread extends Thread {
        private long tickPosition = 0;

        // recording
        long recordingStartedTime;
        long recordStartedTick;
        Track recordingTrack;

        // playing
        Track playingTrack = null;
        long tickPositionSetTime;
        long runningStoppedTime;
        boolean needRefreshPlayingTrack = false;

        /**
         * Constructor
         */
        SequencerThread() {
        }

        /**
         * Get current tick position
         */
        long getTickPosition() {
            if (isRunning) {
                // running
                return (long) (tickPosition + ((System.currentTimeMillis() - tickPositionSetTime) * 1000f * getTicksPerMicrosecond()));
            } else {
                // stopping
                return (long) (tickPosition + ((runningStoppedTime - tickPositionSetTime) * 1000f * getTicksPerMicrosecond()));
            }
        }

        /**
         * Set current tick position
         *
         * @param tick current tick position
         */
        public void setTickPosition(long tick) {
            tickPosition = tick;
            if (isRunning) {
                tickPositionSetTime = System.currentTimeMillis();
            }
        }

        /**
         * Start recording
         */
        void startRecording() {
            if (isRecording) {
                // already recording
                return;
            }

            recordingTrack = sequence.createTrack();
            recordingStartedTime = System.currentTimeMillis();
            recordStartedTick = getTickPosition();
            isRecording = true;
        }

        /**
         * Stop recording
         */
        void stopRecording() {
            if (isRecording == false) {
                // already stopped
                return;
            }

            long recordEndedTime = System.currentTimeMillis();
            isRecording = false;

            for (Track track : sequence.getTracks()) {
                Set<Integer> recordEnableChannels = recordEnable.get(track);

                // remove events while recorded time
                Set<MidiEvent> eventToRemoval = new HashSet<MidiEvent>();
                for (int trackIndex = 0; trackIndex < track.size(); trackIndex++) {
                    MidiEvent midiEvent = track.get(trackIndex);
                    if (isRecordable(recordEnableChannels, midiEvent) && //
                            midiEvent.getTick() >= recordingStartedTime && midiEvent.getTick() <= recordEndedTime) { // recorded time
                        eventToRemoval.add(midiEvent);
                    }
                }

                for (MidiEvent event : eventToRemoval) {
                    track.remove(event);
                }

                // add recorded events
                for (int eventIndex = 0; eventIndex < recordingTrack.size(); eventIndex++) {
                    if (isRecordable(recordEnableChannels, recordingTrack.get(eventIndex))) {
                        track.add(recordingTrack.get(eventIndex));
                    }
                }

                TrackUtils.sortEvents(track);
            }

            // refresh playingTrack
            needRefreshPlayingTrack = true;
        }

        /**
         * Start playing
         */
        void startPlaying() {
            if (isRunning) {
                // already playing
                return;
            }

            tickPosition = getLoopStartPoint();
            tickPositionSetTime = System.currentTimeMillis();
            isRunning = true;

            synchronized (this) {
                notifyAll();
            }
        }

        /**
         * Stop playing
         */
        void stopPlaying() {
            if (isRunning == false) {
                // already stopping
                synchronized (this) {
                    notifyAll();
                }
                interrupt();
                return;
            }

            isRunning = false;
            runningStoppedTime = System.currentTimeMillis();

            // force stop sleeping
            synchronized (this) {
                notifyAll();
            }
            interrupt();
        }

        /**
         * Process the specified {@link MidiMessage} and fire events to registered event listeners.
         *
         * @param message the {@link MidiMessage}
         */
        void fireEventListeners(@NonNull MidiMessage message) {
            if (message instanceof MetaMessage) {
                synchronized (metaEventListeners) {
                    try {
                        for (MetaEventListener metaEventListener : metaEventListeners) {
                            metaEventListener.meta((MetaMessage) message);
                        }
                    } catch (ConcurrentModificationException ignored) {
                        // FIXME why this exception will be thrown? ... ignore it.
                    }
                }
            } else if (message instanceof ShortMessage) {
                ShortMessage shortMessage = (ShortMessage) message;
                if (shortMessage.getCommand() == ShortMessage.CONTROL_CHANGE) {
                    synchronized (controllerEventListenerMap) {
                        try {
                            Set<ControllerEventListener> eventListeners = controllerEventListenerMap.get(shortMessage.getData1());
                            if (eventListeners != null) {
                                for (ControllerEventListener eventListener : eventListeners) {
                                    eventListener.controlChange(shortMessage);
                                }
                            }
                        } catch (ConcurrentModificationException cme) {
                            // ignore exception
                        }
                    }
                }
            }
        }

        @Override
        public void run() {
            super.run();

            refreshPlayingTrack();

            // recording
            Receiver midiEventRecordingReceiver = new Receiver() {
                @Override
                public void send(@NonNull MidiMessage message, long timeStamp) {
                    if (isRecording) {
                        recordingTrack.add(new MidiEvent(message, (long) (recordStartedTick + ((System.currentTimeMillis() - recordingStartedTime) * 1000f * getTicksPerMicrosecond()))));
                    }

                    fireEventListeners(message);
                }

                @Override
                public void close() {
                    // do nothing
                }
            };

            synchronized (transmitters) {
                for (Transmitter transmitter : transmitters) {
                    // receive from all transmitters
                    transmitter.setReceiver(midiEventRecordingReceiver);
                }
            }

            // playing
            while (isOpen) {
                synchronized (this) {
                    try {
                        // wait for being notified
                        while (!isRunning && isOpen) {
                            this.wait();
                        }
                    } catch (InterruptedException e) {
                        // ignore exception
                    }
                }

                if (playingTrack == null) {
                    if (needRefreshPlayingTrack) {
                        refreshPlayingTrack();
                    }

                    if (playingTrack == null) {
                        continue;
                    }
                }

                // process looping
                for (int loop = 0; loop < getLoopCount() + 1; loop = (getLoopCount() == LOOP_CONTINUOUSLY ? loop : loop + 1)) {
                    if (needRefreshPlayingTrack) {
                        refreshPlayingTrack();
                    }

                    for (int i = 0; i < playingTrack.size(); i++) {
                        MidiEvent midiEvent = playingTrack.get(i);
                        MidiMessage midiMessage = midiEvent.getMessage();

                        if (needRefreshPlayingTrack) {
                            // skip to lastTick
                            if (midiEvent.getTick() < tickPosition) {
                                if (midiMessage instanceof MetaMessage) {
                                    // process tempo change message
                                    MetaMessage metaMessage = (MetaMessage) midiMessage;
                                    if (processTempoChange(metaMessage) == false) {
                                        // not tempo message, process the event
                                        synchronized (receivers) {
                                            for (Receiver receiver : receivers) {
                                                receiver.send(midiMessage, 0);
                                            }
                                        }
                                    }
                                    continue;
                                } else if (midiMessage instanceof SysexMessage) {
                                    // process system messages
                                    for (Receiver receiver : receivers) {
                                        receiver.send(midiMessage, 0);
                                    }
                                } else if (midiMessage instanceof ShortMessage) {
                                    // process control change / program change messages
                                    ShortMessage shortMessage = (ShortMessage) midiMessage;
                                    switch (shortMessage.getCommand()) {
                                        case ShortMessage.NOTE_ON:
                                        case ShortMessage.NOTE_OFF:
                                            break;
                                        default:
                                            synchronized (receivers) {
                                                for (Receiver receiver : receivers) {
                                                    receiver.send(midiMessage, 0);
                                                }
                                            }
                                            break;
                                    }
                                }

                                continue;
                            } else {
                                // refresh playingTrack completed
                                needRefreshPlayingTrack = false;
                            }
                        }

                        if (midiEvent.getTick() < getLoopStartPoint() || (getLoopEndPoint() != -1 && midiEvent.getTick() > getLoopEndPoint())) {
                            // outer loop
                            tickPosition = midiEvent.getTick();
                            tickPositionSetTime = System.currentTimeMillis();
                            continue;
                        }

                        try {
                            long sleepLength = (long) ((1.0f / getTicksPerMicrosecond()) * (midiEvent.getTick() - tickPosition) / 1000f / getTempoFactor());
                            if (sleepLength > 0) {
                                sleep(sleepLength);
                            }
                            tickPosition = midiEvent.getTick();
                            tickPositionSetTime = System.currentTimeMillis();
                        } catch (InterruptedException e) {
                            // ignore exception
                        }

                        if (isRunning == false) {
                            break;
                        }

                        if (needRefreshPlayingTrack) {
                            break;
                        }

                        // process tempo change message
                        if (midiMessage instanceof MetaMessage) {
                            MetaMessage metaMessage = (MetaMessage) midiMessage;
                            if (processTempoChange(metaMessage)) {
                                fireEventListeners(midiMessage);

                                // do not send tempo message to the receivers.
                                continue;
                            }
                        }

                        // send MIDI events
                        synchronized (receivers) {
                            for (Receiver receiver : receivers) {
                                receiver.send(midiMessage, 0);
                            }
                        }

                        fireEventListeners(midiMessage);
                    }
                }

                // loop end
                isRunning = false;
                runningStoppedTime = System.currentTimeMillis();
            }
        }

        /**
         * Process the tempo change events
         *
         * @param metaMessage the {@link MetaMessage}
         * @return true if the tempo changed
         */
        private boolean processTempoChange(@NonNull MetaMessage metaMessage) {
            if (metaMessage.getLength() == 6 && metaMessage.getStatus() == MetaMessage.META) {
                byte[] message = metaMessage.getMessage();
                if (message != null && (message[1] & 0xff) == MetaMessage.TYPE_TEMPO && message[2] == 3) {
                    int tempo = (message[5] & 0xff) | //
                            ((message[4] & 0xff) << 8) | //
                            ((message[3] & 0xff) << 16);

                    setTempoInMPQ(tempo);
                    return true;
                }
            }
            return false;
        }

        /**
         * Merge current sequence's track to play
         */
        private void refreshPlayingTrack() {
            if (sequence == null) {
                return;
            }

            Track[] tracks = sequence.getTracks();
            if (tracks.length > 0) {
                try {
                    // at first, merge all track into one track
                    playingTrack = TrackUtils.mergeSequenceToTrack(SequencerImpl.this, recordEnable);
                } catch (InvalidMidiDataException e) {
                    // ignore exception
                }
            }
        }

        /**
         * Check if the event can be recorded
         *
         * @param recordEnableChannels the channel IDs that are able to record.
         * @param midiEvent the {@link MidiEvent}
         * @return true if the event can be recorded
         */
        private boolean isRecordable(@Nullable Set<Integer> recordEnableChannels, @NonNull MidiEvent midiEvent) {
            if (recordEnableChannels == null) {
                return false;
            }
            
            if (recordEnableChannels.contains(Integer.valueOf(-1))) {
                return true;
            }

            int status = midiEvent.getMessage().getStatus();
            switch (status & ShortMessage.MASK_EVENT) {
                // channel messages
                case ShortMessage.NOTE_OFF:
                case ShortMessage.NOTE_ON:
                case ShortMessage.POLY_PRESSURE:
                case ShortMessage.CONTROL_CHANGE:
                case ShortMessage.PROGRAM_CHANGE:
                case ShortMessage.CHANNEL_PRESSURE:
                case ShortMessage.PITCH_BEND:
                    // recorded Track and channel
                    return recordEnableChannels.contains(Integer.valueOf(status & ShortMessage.MASK_CHANNEL));
                // exclusive messages
                default:
                    return true;
            }
        }
    }

    /**
     * Constructor
     */
    public SequencerImpl() {
    }

    @NonNull
    @Override
    public Info getDeviceInfo() {
        return new Info("Sequencer", "jp.kshoji", "Android MIDI Sequencer", "0.1");
    }

    @Override
    public void open() throws MidiUnavailableException {
        // open devices
        synchronized (receivers) {
            receivers.clear();
            receivers.addAll(MidiSystemUtils.getReceivers());
        }

        synchronized (transmitters) {
            transmitters.clear();
            transmitters.addAll(MidiSystemUtils.getTransmitters());
        }

        if (sequencerThread == null) {
            sequencerThread = new SequencerThread();
            sequencerThread.setName("MidiSequencer_" + sequencerThread.getId());
            try {
                sequencerThread.start();
            } catch (IllegalThreadStateException e) {
                // maybe already started
            }
        }

        isOpen = true;
        synchronized (sequencerThread) {
            sequencerThread.notifyAll();
        }
    }

    @Override
    public void close() {
        // FIXME frequently calling 'close and open' causes app freeze(can't stop playing)

        // close devices
        synchronized (receivers) {
            for (Receiver receiver : receivers) {
                receiver.close();
            }
            receivers.clear();
        }

        synchronized (transmitters) {
            for (Transmitter transmitter : transmitters) {
                transmitter.close();
            }
            transmitters.clear();
        }

        if (sequencerThread != null) {
            sequencerThread.stopPlaying();
            sequencerThread.stopRecording();
            isOpen = false;
            sequencerThread = null;
        }

        synchronized (metaEventListeners) {
            metaEventListeners.clear();
        }

        synchronized (controllerEventListenerMap) {
            controllerEventListenerMap.clear();
        }
    }

    @Override
    public boolean isOpen() {
        return isOpen;
    }

    @Override
    public int getMaxReceivers() {
        synchronized (receivers) {
            return receivers.size();
        }
    }

    @Override
    public int getMaxTransmitters() {
        synchronized (transmitters) {
            return transmitters.size();
        }
    }

    @Override
    public Receiver getReceiver() throws MidiUnavailableException {
        synchronized (receivers) {
            if (receivers.size() > 0) {
                return receivers.get(0);
            } else {
                return null;
            }
        }
    }

    @NonNull
    @Override
    public List<Receiver> getReceivers() {
        synchronized (receivers) {
            return Collections.unmodifiableList(receivers);
        }
    }

    @Override
    public Transmitter getTransmitter() throws MidiUnavailableException {
        synchronized (transmitters) {
            if (transmitters.size() > 0) {
                return transmitters.get(0);
            } else {
                return null;
            }
        }
    }

    @NonNull
    @Override
    public List<Transmitter> getTransmitters() {
        synchronized (transmitters) {
            return Collections.unmodifiableList(transmitters);
        }
    }

    @NonNull
    @Override
    public int[] addControllerEventListener(@NonNull ControllerEventListener listener, @NonNull int[] controllers) {
        synchronized (controllerEventListenerMap) {
            for (int controllerId : controllers) {
                Set<ControllerEventListener> listeners = controllerEventListenerMap.get(controllerId);
                if (listeners == null) {
                    listeners = new HashSet<ControllerEventListener>();
                }
                listeners.add(listener);
                controllerEventListenerMap.put(controllerId, listeners);
            }
            return controllers;
        }
    }

    @NonNull
    @Override
    public int[] removeControllerEventListener(@NonNull ControllerEventListener listener, @NonNull int[] controllers) {
        synchronized (controllerEventListenerMap) {
            List<Integer> resultList = new ArrayList<Integer>();
            for (int controllerId : controllers) {
                Set<ControllerEventListener> listeners = controllerEventListenerMap.get(controllerId);
                if (listeners != null && listeners.contains(listener)) {
                    listeners.remove(listener);
                } else {
                    // remaining controller id
                    resultList.add(controllerId);
                }
                controllerEventListenerMap.put(controllerId, listeners);
            }

            // returns currently registered controller ids for the argument specified listener
            int[] resultPrimitiveArray = new int[resultList.size()];
            for (int i = 0; i < resultPrimitiveArray.length; i++) {
                Integer resultValue = resultList.get(i);
                if (resultValue == null) {
                    continue;
                }

                resultPrimitiveArray[i] = resultValue;
            }
            return resultPrimitiveArray;
        }
    }

    @Override
    public boolean addMetaEventListener(@NonNull MetaEventListener listener) {
        // return true if registered successfully
        synchronized (metaEventListeners) {
            return metaEventListeners.add(listener);
        }
    }

    @Override
    public void removeMetaEventListener(@NonNull MetaEventListener listener) {
        synchronized (metaEventListeners) {
            metaEventListeners.remove(listener);
        }
    }

    @Override
    public int getLoopCount() {
        return loopCount;
    }

    @Override
    public void setLoopCount(int count) {
        if (count != LOOP_CONTINUOUSLY && count < 0) {
            throw new IllegalArgumentException("Invalid loop count value:" + count);
        }
        loopCount = count;
    }

    @Override
    public long getLoopStartPoint() {
        return loopStartPoint;
    }

    @Override
    public void setLoopStartPoint(long tick) {
        if (tick > getTickLength() || (loopEndPoint != -1 && tick > loopEndPoint) || tick < 0) {
            throw new IllegalArgumentException("Invalid loop start point value:" + tick);
        }
        loopStartPoint = tick;
    }

    @Override
    public long getLoopEndPoint() {
        return loopEndPoint;
    }

    @Override
    public void setLoopEndPoint(long tick) {
        if (tick > getTickLength() || (tick != -1 && loopStartPoint > tick) || tick < -1) {
            throw new IllegalArgumentException("Invalid loop end point value:" + tick);
        }
        loopEndPoint = tick;
    }

    @NonNull
    @Override
    public SyncMode getMasterSyncMode() {
        return masterSyncMode;
    }

    @Override
    public void setMasterSyncMode(@NonNull SyncMode sync) {
        for (SyncMode availableMode : getMasterSyncModes()) {
            if (availableMode == sync) {
                masterSyncMode = sync;
            }
        }
    }

    @NonNull
    @Override
    public SyncMode[] getMasterSyncModes() {
        return MASTER_SYNC_MODES;
    }

    @Override
    public long getMicrosecondPosition() {
        return (long) (getTickPosition() / getTicksPerMicrosecond());
    }

    @Override
    public void setMicrosecondPosition(long microseconds) {
        setTickPosition((long) (getTicksPerMicrosecond() * microseconds));
    }

    /**
     * convert parameter from microseconds to tick
     *
     * @return ticks per microsecond, NaN: sequence is null
     */
    float getTicksPerMicrosecond() {
        if (sequence == null) {
            return Float.NaN;
        }

        float ticksPerMicrosecond;
        if (sequence.getDivisionType() == Sequence.PPQ) {
            // PPQ : tempoInBPM / 60f * resolution / 1000000 ticks per microsecond
            ticksPerMicrosecond = tempoInBPM / 60f * sequence.getResolution() / 1000000f;
        } else {
            // SMPTE : divisionType * resolution / 1000000 ticks per microsecond
            ticksPerMicrosecond = sequence.getDivisionType() * sequence.getResolution() / 1000000f;
        }

        return ticksPerMicrosecond;
    }

    @Override
    public long getMicrosecondLength() {
        return sequence.getMicrosecondLength();
    }

    @Override
    public Sequence getSequence() {
        return sequence;
    }

    @Override
    public void setSequence(@NonNull InputStream stream) throws IOException, InvalidMidiDataException {
        setSequence(new StandardMidiFileReader().getSequence(stream));
    }

    @Override
    public void setSequence(@Nullable Sequence sequence) throws InvalidMidiDataException {
        this.sequence = sequence;

        if (sequence != null) {
            sequencerThread.needRefreshPlayingTrack = true;
        }
    }

    @NonNull
    @Override
    public SyncMode getSlaveSyncMode() {
        return slaveSyncMode;
    }

    @Override
    public void setSlaveSyncMode(@NonNull SyncMode sync) {
        for (SyncMode availableMode : getSlaveSyncModes()) {
            if (availableMode == sync) {
                slaveSyncMode = sync;
            }
        }
    }

    @NonNull
    @Override
    public SyncMode[] getSlaveSyncModes() {
        return SLAVE_SYNC_MODES;
    }

    @Override
    public float getTempoFactor() {
        return tempoFactor;
    }

    @Override
    public void setTempoFactor(float factor) {
        if (factor <= 0f) {
            throw new IllegalArgumentException("The tempo factor must be larger than 0f.");
        }

        tempoFactor = factor;
    }

    @Override
    public float getTempoInBPM() {
        return tempoInBPM;
    }

    @Override
    public void setTempoInBPM(float bpm) {
        tempoInBPM = bpm;
    }

    @Override
    public float getTempoInMPQ() {
        return 60000000f / tempoInBPM;
    }

    @Override
    public void setTempoInMPQ(float mpq) {
        tempoInBPM = 60000000f / mpq;
    }

    @Override
    public long getTickLength() {
        if (sequence == null) {
            return 0;
        }
        return sequence.getTickLength();
    }

    @Override
    public long getTickPosition() {
        return sequencerThread.getTickPosition();
    }

    @Override
    public void setTickPosition(long tick) {
        sequencerThread.setTickPosition(tick);
    }

    @Override
    public boolean getTrackMute(int track) {
        return trackMute.get(track);
    }

    @Override
    public void setTrackMute(int track, boolean mute) {
        trackMute.put(track, mute);
    }

    @Override
    public boolean getTrackSolo(int track) {
        return trackSolo.get(track);
    }

    @Override
    public void setTrackSolo(int track, boolean solo) {
        trackSolo.put(track, solo);
    }

    @Override
    public void recordDisable(@Nullable Track track) {
        if (track == null) {
            // disable all track
            recordEnable.clear();
        } else {
            // disable specified track
            Set<Integer> trackRecordEnable = recordEnable.get(track);
            if (trackRecordEnable != null) {
                recordEnable.put(track, null);
            }
        }
    }

    @Override
    public void recordEnable(@NonNull Track track, int channel) {
        Set<Integer> trackRecordEnable = recordEnable.get(track);
        if (trackRecordEnable == null) {
            trackRecordEnable = new HashSet<Integer>();
        }

        if (channel == -1) {
            for (int i = 0; i < 16; i++) {
                // record to the all channels
                trackRecordEnable.add(i);
            }
            recordEnable.put(track, trackRecordEnable);
        } else if (channel >= 0 && channel < 16) {
            trackRecordEnable.add(channel);
            recordEnable.put(track, trackRecordEnable);
        }
    }

    @Override
    public void startRecording() {
        // start playing AND recording
        sequencerThread.startRecording();
        sequencerThread.startPlaying();
    }

    @Override
    public boolean isRecording() {
        return isRecording;
    }

    @Override
    public void stopRecording() {
        // stop recording
        sequencerThread.stopRecording();
    }

    @Override
    public void start() {
        // start playing
        sequencerThread.startPlaying();
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }

    @Override
    public void stop() {
        // stop playing AND recording
        sequencerThread.stopRecording();
        sequencerThread.stopPlaying();
    }
}
