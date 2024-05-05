package jp.kshoji.javax.sound.midi.interapp;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.List;

import jp.kshoji.javax.sound.midi.Instrument;
import jp.kshoji.javax.sound.midi.MidiChannel;
import jp.kshoji.javax.sound.midi.MidiUnavailableException;
import jp.kshoji.javax.sound.midi.Patch;
import jp.kshoji.javax.sound.midi.Receiver;
import jp.kshoji.javax.sound.midi.Soundbank;
import jp.kshoji.javax.sound.midi.Synthesizer;
import jp.kshoji.javax.sound.midi.Transmitter;
import jp.kshoji.javax.sound.midi.VoiceStatus;
import jp.kshoji.javax.sound.midi.impl.MidiChannelImpl;

/**
 * {@link jp.kshoji.javax.sound.midi.Synthesizer} implementation
 *
 * @author K.Shoji
 */
public class InterAppMidiSynthesizer implements Synthesizer {
    final InterAppMidiDevice interAppMidiDevice;
    private MidiChannel[] channels;
    private VoiceStatus[] voiceStatuses;

    /**
     * Constructor
     *
     * @param interAppMidiDevice the device
     */
    public InterAppMidiSynthesizer(final InterAppMidiDevice interAppMidiDevice) {
        this.interAppMidiDevice = interAppMidiDevice;

        Receiver receiver = null;
        try {
            receiver = this.interAppMidiDevice.getReceiver();
        } catch (final MidiUnavailableException ignored) {
        }

        setReceiver(receiver);
    }

    @NonNull
    @Override
    public Info getDeviceInfo() {
        return interAppMidiDevice.getDeviceInfo();
    }

    @Override
    public void open() throws MidiUnavailableException {
        interAppMidiDevice.open();
    }

    @Override
    public void close() {
        interAppMidiDevice.close();
    }

    @Override
    public boolean isOpen() {
        return interAppMidiDevice.isOpen();
    }

    @Override
    public long getMicrosecondPosition() {
        return -1;
    }

    @Override
    public int getMaxReceivers() {
        return interAppMidiDevice.getMaxReceivers();
    }

    @Override
    public int getMaxTransmitters() {
        return interAppMidiDevice.getMaxTransmitters();
    }

    @NonNull
    @Override
    public Receiver getReceiver() throws MidiUnavailableException {
        return interAppMidiDevice.getReceiver();
    }

    @NonNull
    @Override
    public List<Receiver> getReceivers() {
        return interAppMidiDevice.getReceivers();
    }

    @NonNull
    @Override
    public Transmitter getTransmitter() throws MidiUnavailableException {
        return interAppMidiDevice.getTransmitter();
    }

    @NonNull
    @Override
    public List<Transmitter> getTransmitters() {
        return interAppMidiDevice.getTransmitters();
    }

    public void setReceiver(final Receiver receiver) {
        if (receiver == null) {
            // empty
            channels = new MidiChannel[0];
            voiceStatuses = new VoiceStatus[0];
        } else {
            // 16 channels
            voiceStatuses = new VoiceStatus[16];
            channels = new MidiChannel[16];
            for (int channel = 0; channel < 16; channel++) {
                voiceStatuses[channel] = new VoiceStatus();
                channels[channel] = new MidiChannelImpl(channel, receiver, voiceStatuses[channel]);
            }
        }
    }

    @NonNull
    @Override
    public MidiChannel[] getChannels() {
        return channels;
    }

    @Override
    public long getLatency() {
        return 0;
    }

    @Override
    public int getMaxPolyphony() {
        return 127;
    }

    @NonNull
    @Override
    public VoiceStatus[] getVoiceStatus() {
        return voiceStatuses;
    }

    @Nullable
    @Override
    public Soundbank getDefaultSoundbank() {
        return null;
    }

    @Override
    public boolean isSoundbankSupported(@NonNull Soundbank soundbank) {
        return false;
    }

    @NonNull
    @Override
    public Instrument[] getAvailableInstruments() {
        return new Instrument[0];
    }

    @NonNull
    @Override
    public Instrument[] getLoadedInstruments() {
        return new Instrument[0];
    }

    @Override
    public boolean remapInstrument(@NonNull Instrument from, @NonNull Instrument to) {
        return false;
    }

    @Override
    public boolean loadAllInstruments(@NonNull Soundbank soundbank) {
        return false;
    }

    @Override
    public void unloadAllInstruments(@NonNull Soundbank soundbank) {

    }

    @Override
    public boolean loadInstrument(@NonNull Instrument instrument) {
        return false;
    }

    @Override
    public void unloadInstrument(@NonNull Instrument instrument) {

    }

    @Override
    public boolean loadInstruments(@NonNull Soundbank soundbank, @NonNull Patch[] patchList) {
        return false;
    }

    @Override
    public void unloadInstruments(@NonNull Soundbank soundbank, @NonNull Patch[] patchList) {

    }
}
