package jp.kshoji.javax.sound.midi.interapp;

import android.content.Context;
import android.media.midi.MidiDevice;
import android.media.midi.MidiDeviceInfo;
import android.media.midi.MidiManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jp.kshoji.javax.sound.midi.MidiSystem;
import jp.kshoji.javax.sound.midi.MidiUnavailableException;

/**
 * {@link jp.kshoji.javax.sound.midi.MidiSystem} for Inter App MIDI
 *
 * @author K.Shoji
 */
public final class InterAppMidiSystem {
    private MidiManager midiManager;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Map<MidiDeviceInfo, InterAppMidiDevice> openedDeviceMap;
    private final Map<MidiDeviceInfo, InterAppMidiSynthesizer> midiSynthesizerMap;
    private Thread connectionWatcher;
    private volatile boolean connectionWatcherEnabled;

    /**
     * Constructor
     *
     * @param context the context
     */
    public InterAppMidiSystem(@NonNull Context context) {
        openedDeviceMap = new HashMap<>();
        midiSynthesizerMap = new HashMap<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            midiManager = (MidiManager) context.getSystemService(Context.MIDI_SERVICE);
            if (midiManager != null) {
                connectionWatcher = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        connectionWatcherEnabled = true;
                        while (connectionWatcherEnabled) {
                            Set<MidiDeviceInfo> devices;
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                devices = midiManager.getDevicesForTransport(MidiManager.TRANSPORT_MIDI_BYTE_STREAM);
                            } else {
                                devices = new HashSet<>();
                                Collections.addAll(devices, midiManager.getDevices());
                            }

                            // detect opened
                            for (MidiDeviceInfo device : devices) {
                                openMidiDevice(device);
                            }

                            // detect closed
                            for (MidiDeviceInfo connectedDevice : openedDeviceMap.keySet()) {
                                if (!devices.contains(connectedDevice)) {
                                    InterAppMidiDevice removed = openedDeviceMap.remove(connectedDevice);
                                    if (removed != null) {
                                        removed.close();
                                        MidiSystem.removeMidiDevice(removed);
                                        synchronized (midiSynthesizerMap) {
                                            InterAppMidiSynthesizer existingSynthesizer = midiSynthesizerMap.remove(connectedDevice);
                                            if (existingSynthesizer != null) {
                                                MidiSystem.removeSynthesizer(existingSynthesizer);
                                            }
                                        }
                                    }
                                }
                            }

                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException ignored) {
                            }
                        }
                    }
                });
                connectionWatcher.start();
            }
        }
    }

    /**
     * Terminates MIDI system
     */
    public void terminate() {
        if (midiManager != null) {
            connectionWatcherEnabled = false;
            if (connectionWatcher != null) {
                connectionWatcher.interrupt();
                connectionWatcher = null;
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (MidiDeviceInfo connectedDevice : openedDeviceMap.keySet()) {
                InterAppMidiDevice removed = openedDeviceMap.remove(connectedDevice);
                if (removed != null) {
                    removed.close();
                    MidiSystem.removeMidiDevice(removed);
                    synchronized (midiSynthesizerMap) {
                        InterAppMidiSynthesizer existingSynthesizer = midiSynthesizerMap.remove(connectedDevice);
                        if (existingSynthesizer != null) {
                            MidiSystem.removeSynthesizer(existingSynthesizer);
                        }
                    }
                }
            }
        }
    }

    private void openMidiDevice(final MidiDeviceInfo device) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (device.getType() == MidiDeviceInfo.TYPE_VIRTUAL) {
                if (openedDeviceMap.containsKey(device)) {
                    return;
                }

                midiManager.openDevice(device, new MidiManager.OnDeviceOpenedListener() {
                    @Override
                    public void onDeviceOpened(MidiDevice midiDevice) {
                        if (midiDevice == null) {
                            return;
                        }

                        MidiDeviceInfo deviceInfo = midiDevice.getInfo();
                        InterAppMidiDevice interAppMidiDevice = new InterAppMidiDevice(midiDevice);
                        openedDeviceMap.put(deviceInfo, interAppMidiDevice);
                        MidiSystem.addMidiDevice(interAppMidiDevice);

                        synchronized (midiSynthesizerMap) {
                            InterAppMidiSynthesizer existingSynthesizer = midiSynthesizerMap.get(deviceInfo);
                            if (existingSynthesizer == null) {
                                InterAppMidiSynthesizer synthesizer = new InterAppMidiSynthesizer(interAppMidiDevice);
                                midiSynthesizerMap.put(deviceInfo, synthesizer);
                            } else {
                                try {
                                    existingSynthesizer.setReceiver(interAppMidiDevice.getReceiver());
                                } catch (MidiUnavailableException ignored) {
                                    existingSynthesizer.setReceiver(null);
                                }
                            }
                        }
                    }
                }, handler);
            }
        }
    }
}
