package jp.kshoji.javax.sound.midi.interapp;

import android.media.midi.MidiOutputPort;
import android.media.midi.MidiReceiver;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import jp.kshoji.javax.sound.midi.InvalidMidiDataException;
import jp.kshoji.javax.sound.midi.Receiver;
import jp.kshoji.javax.sound.midi.ShortMessage;
import jp.kshoji.javax.sound.midi.SysexMessage;
import jp.kshoji.javax.sound.midi.Transmitter;

/**
 * {@link Transmitter} implementation
 *
 * @author K.Shoji
 */
public class InterAppMidiTransmitter implements Transmitter {
    private final MidiOutputPort midiOutputPort;
    private Receiver receiver;
    private final InterAppMidiReceiver midiReceiver;

    /**
     * Receiver used internally
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    private static class InterAppMidiReceiver extends MidiReceiver {
        private Receiver receiver;
        private final ByteArrayOutputStream sysexStream = new ByteArrayOutputStream();
        private boolean sysexReceiving = false;

        public void setReceiver(Receiver receiver) {
            this.receiver = receiver;
        }

        @Override
        public void onSend(byte[] message, int offset, int count, long timestamp) throws IOException {
            byte[] midiData = new byte[count];
            System.arraycopy(message, offset, midiData, 0, count);

            for (int i = 0; i < midiData.length;) {
                if (sysexReceiving) {
                    for (; i < midiData.length; i++) {
                        sysexStream.write(midiData[i]);
                        if ((midiData[i] & 0xff) == 0xf7) {
                            sysexReceiving = false;
                            break;
                        }
                    }

                    if (!sysexReceiving) {
                        // finished to read whole sysex
                        try {
                            if (receiver != null) {
                                byte[] sysexStreamByteArray = sysexStream.toByteArray();
                                SysexMessage sysexMessage = new SysexMessage(sysexStreamByteArray, sysexStreamByteArray.length);
                                receiver.send(sysexMessage, timestamp);
                            }
                        } catch (InvalidMidiDataException ignored) {
                        }
                        sysexStream.reset();
                    }

                    continue;
                }

                switch (midiData[i] & 0xf0) {
                    case 0x80:
                        if (midiData.length >= i + 3) {
                            try {
                                if (receiver != null) {
                                    ShortMessage shortMessage = new ShortMessage(ShortMessage.NOTE_OFF, midiData[i] & 0xf, midiData[i + 1], midiData[i + 2]);
                                    receiver.send(shortMessage, timestamp);
                                }
                            } catch (InvalidMidiDataException ignored) {
                            }
                        }
                        i += 3;
                        break;
                    case 0x90:
                        if (midiData.length >= i + 3) {
                            try {
                                if (receiver != null) {
                                    ShortMessage shortMessage = new ShortMessage(ShortMessage.NOTE_ON, midiData[i] & 0xf, midiData[i + 1], midiData[i + 2]);
                                    receiver.send(shortMessage, timestamp);
                                }
                            } catch (InvalidMidiDataException ignored) {
                            }
                        }
                        i += 3;
                        break;
                    case 0xa0: // Polyphonic Aftertouch
                        if (midiData.length >= i + 3) {
                            try {
                                if (receiver != null) {
                                    ShortMessage shortMessage = new ShortMessage(ShortMessage.POLY_PRESSURE, midiData[i] & 0xf, midiData[i + 1], midiData[i + 2]);
                                    receiver.send(shortMessage, timestamp);
                                }
                            } catch (InvalidMidiDataException ignored) {
                            }
                        }
                        i += 3;
                        break;
                    case 0xb0: // Control Change
                        if (midiData.length >= i + 3) {
                            try {
                                if (receiver != null) {
                                    ShortMessage shortMessage = new ShortMessage(ShortMessage.CONTROL_CHANGE, midiData[i] & 0xf, midiData[i + 1], midiData[i + 2]);
                                    receiver.send(shortMessage, timestamp);
                                }
                            } catch (InvalidMidiDataException ignored) {
                            }
                        }
                        i += 3;
                        break;
                    case 0xc0: // Program Change
                        if (midiData.length >= i + 2) {
                            try {
                                if (receiver != null) {
                                    ShortMessage shortMessage = new ShortMessage(ShortMessage.PROGRAM_CHANGE, midiData[i] & 0xf, midiData[i + 1], 0);
                                    receiver.send(shortMessage, timestamp);
                                }
                            } catch (InvalidMidiDataException ignored) {
                            }
                        }
                        i += 2;
                        break;
                    case 0xd0: // Channel Aftertouch
                        if (midiData.length >= i + 2) {
                            try {
                                if (receiver != null) {
                                    ShortMessage shortMessage = new ShortMessage(ShortMessage.CHANNEL_PRESSURE, midiData[i] & 0xf, midiData[i + 1], 0);
                                    receiver.send(shortMessage, timestamp);
                                }
                            } catch (InvalidMidiDataException ignored) {
                            }
                        }
                        i += 2;
                        break;
                    case 0xe0: // Pitch Wheel
                        if (midiData.length >= i + 3) {
                            try {
                                if (receiver != null) {
                                    ShortMessage shortMessage = new ShortMessage(ShortMessage.PITCH_BEND, midiData[i] & 0xf, midiData[i + 1], midiData[i + 2]);
                                    receiver.send(shortMessage, timestamp);
                                }
                            } catch (InvalidMidiDataException ignored) {
                            }
                        }
                        i += 3;
                        break;
                    case 0xf0:
                        switch (midiData[i] & 0xff) {
                            case 0xf0: // Sysex
                            {
                                sysexReceiving = true;
                                sysexStream.reset();
                                for (; i < midiData.length; i++) {
                                    sysexStream.write(midiData[i]);
                                    if ((midiData[i] & 0xff) == 0xf7) {
                                        sysexReceiving = false;
                                        break;
                                    }
                                }

                                if (!sysexReceiving) {
                                    // finished to read whole sysex
                                    try {
                                        if (receiver != null) {
                                            byte[] sysexStreamByteArray = sysexStream.toByteArray();
                                            SysexMessage sysexMessage = new SysexMessage(sysexStreamByteArray, sysexStreamByteArray.length);
                                            receiver.send(sysexMessage, timestamp);
                                        }
                                    } catch (InvalidMidiDataException ignored) {
                                    }
                                    sysexStream.reset();
                                }
                            }
                            break;
                            case 0xf1: // Time Code Quarter Frame
                                if (midiData.length >= i + 2) {
                                    try {
                                        if (receiver != null) {
                                            ShortMessage shortMessage = new ShortMessage(ShortMessage.MIDI_TIME_CODE, midiData[i + 1], 0);
                                            receiver.send(shortMessage, timestamp);
                                        }
                                    } catch (InvalidMidiDataException ignored) {
                                    }
                                }
                                i += 2;
                                break;
                            case 0xf2: // Song Position Pointer
                                if (midiData.length >= i + 3) {
                                    try {
                                        if (receiver != null) {
                                            ShortMessage shortMessage = new ShortMessage(ShortMessage.SONG_POSITION_POINTER, midiData[i + 1], midiData[i + 2]);
                                            receiver.send(shortMessage, timestamp);
                                        }
                                    } catch (InvalidMidiDataException ignored) {
                                    }
                                }
                                i += 3;
                                break;
                            case 0xf3: // Song Select
                                if (midiData.length >= i + 2) {
                                    try {
                                        if (receiver != null) {
                                            ShortMessage shortMessage = new ShortMessage(ShortMessage.SONG_SELECT, midiData[i + 1], 0);
                                            receiver.send(shortMessage, timestamp);
                                        }
                                    } catch (InvalidMidiDataException ignored) {
                                    }
                                }
                                i += 2;
                                break;
                            case 0xf6: // Tune Request
                                try {
                                    if (receiver != null) {
                                        ShortMessage shortMessage = new ShortMessage(ShortMessage.TUNE_REQUEST, 0, 0);
                                        receiver.send(shortMessage, timestamp);
                                    }
                                } catch (InvalidMidiDataException ignored) {
                                }
                                i++;
                                break;
                            case 0xf8: // Timing Clock
                                try {
                                    if (receiver != null) {
                                        ShortMessage shortMessage = new ShortMessage(ShortMessage.TIMING_CLOCK, 0, 0);
                                        receiver.send(shortMessage, timestamp);
                                    }
                                } catch (InvalidMidiDataException ignored) {
                                }
                                i++;
                                break;
                            case 0xfa: // Start
                                try {
                                    if (receiver != null) {
                                        ShortMessage shortMessage = new ShortMessage(ShortMessage.START, 0, 0);
                                        receiver.send(shortMessage, timestamp);
                                    }
                                } catch (InvalidMidiDataException ignored) {
                                }
                                i++;
                                break;
                            case 0xfb: // Continue
                                try {
                                    if (receiver != null) {
                                        ShortMessage shortMessage = new ShortMessage(ShortMessage.CONTINUE, 0, 0);
                                        receiver.send(shortMessage, timestamp);
                                    }
                                } catch (InvalidMidiDataException ignored) {
                                }
                                i++;
                                break;
                            case 0xfc: // Stop
                                try {
                                    if (receiver != null) {
                                        ShortMessage shortMessage = new ShortMessage(ShortMessage.STOP, 0, 0);
                                        receiver.send(shortMessage, timestamp);
                                    }
                                } catch (InvalidMidiDataException ignored) {
                                }
                                i++;
                                break;
                            case 0xfe: // Active Sensing
                                try {
                                    if (receiver != null) {
                                        ShortMessage shortMessage = new ShortMessage(ShortMessage.ACTIVE_SENSING, 0, 0);
                                        receiver.send(shortMessage, timestamp);
                                    }
                                } catch (InvalidMidiDataException ignored) {
                                }
                                i++;
                                break;
                            case 0xff: // Reset
                                try {
                                    if (receiver != null) {
                                        ShortMessage shortMessage = new ShortMessage(ShortMessage.SYSTEM_RESET, 0, 0);
                                        receiver.send(shortMessage, timestamp);
                                    }
                                } catch (InvalidMidiDataException ignored) {
                                }
                                i++;
                                break;

                            default:
                                i++;
                                break;
                        }
                        break;

                    default:
                        i++;
                        break;
                }
            }
        }
    }

    public InterAppMidiTransmitter(MidiOutputPort midiOutputPort) {
        this.midiOutputPort = midiOutputPort;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            midiReceiver = new InterAppMidiReceiver();
        } else {
            midiReceiver = null;
        }
    }

    @Override
    public void setReceiver(@Nullable Receiver receiver) {
        this.receiver = receiver;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (this.midiReceiver != null) {
                this.midiReceiver.setReceiver(receiver);
            }
        }
    }

    @Nullable
    @Override
    public Receiver getReceiver() {
        return receiver;
    }

    public void open() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (midiReceiver != null) {
                midiOutputPort.onConnect(midiReceiver);
            }
        }
    }

    @Override
    public void close() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (midiReceiver != null) {
                midiOutputPort.onDisconnect(midiReceiver);
            }
            try {
                midiOutputPort.close();
            } catch (IOException ignored) {
            }
        }
    }
}
