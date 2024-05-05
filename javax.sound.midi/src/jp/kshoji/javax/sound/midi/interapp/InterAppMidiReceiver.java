package jp.kshoji.javax.sound.midi.interapp;

import android.media.midi.MidiInputPort;
import android.media.midi.MidiReceiver;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.io.IOException;
import java.util.Arrays;

import jp.kshoji.javax.sound.midi.MidiMessage;
import jp.kshoji.javax.sound.midi.Receiver;

/**
 * {@link jp.kshoji.javax.sound.midi.Receiver} implementation
 *
 * @author K.Shoji
 */
@RequiresApi(api = Build.VERSION_CODES.M)
public class InterAppMidiReceiver extends MidiReceiver {
    InterAppReceiver interAppReceiver;

    /**
     * Receiver used internally
     */
    private static class InterAppReceiver implements Receiver {
        MidiReceiver midiReceiver;

        /**
         * Constructor
         *
         * @param midiReceiver the MIDI Receiver
         */
        @RequiresApi(api = Build.VERSION_CODES.M)
        InterAppReceiver(MidiReceiver midiReceiver) {
            this.midiReceiver = midiReceiver;
        }

        @Override
        public void send(@NonNull MidiMessage message, long timeStamp) {
            try {
                byte[] midiMessage = message.getMessage();
                if (midiMessage != null) {
                    midiReceiver.onSend(midiMessage, 0, midiMessage.length, timeStamp);
                }
            } catch (IOException ignored) {
            }
        }

        private void send(byte[] midiMessage, long timeStamp) {
            try {
                if (midiMessage != null) {
                    midiReceiver.onSend(midiMessage, 0, midiMessage.length, timeStamp);
                }
            } catch (IOException ignored) {
            }
        }

        @Override
        public void close() {
        }

        private void flush() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    midiReceiver.onFlush();
                } catch (IOException ignored) {
                }
            }
        }
    }

    /**
     * Constructor
     *
     * @param midiInputPort the MIDI input port
     */
    public InterAppMidiReceiver(MidiInputPort midiInputPort) {
        this.interAppReceiver = new InterAppReceiver(midiInputPort);
    }

    @Override
    public void onSend(byte[] midiMessage, int offset, int count, long timestamp) throws IOException {
        Log.i("InterAppMidiReceiver", "onSend message: " + Arrays.toString(midiMessage));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            interAppReceiver.send(midiMessage,  timestamp);
        }
    }

    @Override
    public void onFlush() {
        interAppReceiver.flush();
    }

    /**
     * Obtains the Receiver instance
     * @return the Receiver instance
     */
    public Receiver getMidiReceiver() {
        return interAppReceiver;
    }
}
