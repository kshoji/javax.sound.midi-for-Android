package jp.kshoji.javax.sound.midi.interapp;

import android.media.midi.MidiDeviceInfo;
import android.media.midi.MidiInputPort;
import android.media.midi.MidiOutputPort;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jp.kshoji.javax.sound.midi.MidiDevice;
import jp.kshoji.javax.sound.midi.MidiUnavailableException;
import jp.kshoji.javax.sound.midi.Receiver;
import jp.kshoji.javax.sound.midi.Transmitter;

/**
 * {@link jp.kshoji.javax.sound.midi.MidiDevice} implementation
 *
 * @author K.Shoji
 */
public class InterAppMidiDevice implements MidiDevice {
    private final MidiDeviceInfo midiDeviceInfo;
    private final List<InterAppMidiReceiver> receivers = new ArrayList<>();
    private final List<InterAppMidiTransmitter> transmitters = new ArrayList<>();
    private boolean isOpened;

    /**
     * Constructor
     *
     * @param midiDevice the MIDI device
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public InterAppMidiDevice(android.media.midi.MidiDevice midiDevice) {
        midiDeviceInfo = midiDevice.getInfo();
        for (int i = 0; i < midiDeviceInfo.getInputPortCount(); i++) {
            // MidiInputPort: used for MIDI sending
            MidiInputPort midiInputPort = midiDevice.openInputPort(i);
            if (midiInputPort != null) {
                receivers.add(new InterAppMidiReceiver(midiInputPort));
            }
        }

        for (int i = 0; i < midiDeviceInfo.getOutputPortCount(); i++) {
            // MidiOutputPort: used for MIDI receiving
            MidiOutputPort midiOutputPort = midiDevice.openOutputPort(i);
            if (midiOutputPort != null) {
                transmitters.add(new InterAppMidiTransmitter(midiOutputPort));
            }
        }
    }

    @NonNull
    @Override
    public Info getDeviceInfo() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            Bundle properties = midiDeviceInfo.getProperties();
            String name = properties.getString(MidiDeviceInfo.PROPERTY_NAME);
            if (name == null) {
                name = "(null)";
            }
            String vendor = properties.getString(MidiDeviceInfo.PROPERTY_MANUFACTURER);
            if (vendor == null) {
                vendor = "(null)";
            }
            String product = properties.getString(MidiDeviceInfo.PROPERTY_PRODUCT);
            if (product == null) {
                product = "(null)";
            }
            String version = properties.getString(MidiDeviceInfo.PROPERTY_VERSION);
            if (version == null) {
                version = "(null)";
            }
            return new MidiDevice.Info(name, vendor, product, version);
        }

        return new MidiDevice.Info("(null)", "(null)", "(null)", "(null)");
    }

    @Override
    public void open() throws MidiUnavailableException {
        if (isOpened) {
            return;
        }

        synchronized (transmitters) {
            for (InterAppMidiTransmitter transmitter : transmitters) {
                if (transmitter != null) {
                    transmitter.open();
                }
            }
        }

        isOpened = true;
    }

    @Override
    public void close() {
        if (!isOpened) {
            return;
        }

        synchronized (transmitters) {
            for (InterAppMidiTransmitter transmitter : transmitters) {
                if (transmitter != null) {
                    transmitter.close();
                }
            }
        }

        synchronized (receivers) {
            for (InterAppMidiReceiver receiver : receivers) {
                if (receiver != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        receiver.onFlush();
                    }
                }
            }
        }

        isOpened = false;
    }

    @Override
    public boolean isOpen() {
        return isOpened;
    }

    @Override
    public long getMicrosecondPosition() {
        return -1;
    }

    @Override
    public int getMaxReceivers() {
        return receivers.size();
    }

    @Override
    public int getMaxTransmitters() {
        return transmitters.size();
    }

    @NonNull
    @Override
    public Receiver getReceiver() throws MidiUnavailableException {
        synchronized (receivers) {
            for (InterAppMidiReceiver receiver : receivers) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    return receiver.getMidiReceiver();
                }
            }
        }

        throw new MidiUnavailableException("Receiver not found");
    }

    @NonNull
    @Override
    public List<Receiver> getReceivers() {
        final List<Receiver> result = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            synchronized (receivers) {
                for (InterAppMidiReceiver receiver : receivers) {
                    if (receiver != null) {
                        result.add(receiver.getMidiReceiver());
                    }
                }
            }
        }
        return Collections.unmodifiableList(result);
    }

    @NonNull
    @Override
    public Transmitter getTransmitter() throws MidiUnavailableException {
        synchronized (transmitters) {
            for (InterAppMidiTransmitter transmitter : transmitters) {
                if (transmitter != null) {
                    return transmitter;
                }
            }
        }

        throw new MidiUnavailableException("Transmitter not found");
    }

    @NonNull
    @Override
    public List<Transmitter> getTransmitters() {
        final List<Transmitter> result = new ArrayList<>();
        synchronized (transmitters) {
            for (InterAppMidiTransmitter transmitter : transmitters) {
                if (transmitter != null) {
                    result.add(transmitter);
                }
            }
        }
        return Collections.unmodifiableList(result);
    }
}
