package jp.kshoji.javax.sound.midi.listener;

import android.support.annotation.NonNull;

import jp.kshoji.javax.sound.midi.MidiDevice;

/**
 * Listener for MIDI detached events
 */
public interface OnMidiDeviceDetachedListener {

    /**
     * MIDI device has been detached
     *
     * @param midiDevice detached MIDI device
     */
    void onMidiDeviceDetached(@NonNull MidiDevice.Info midiDeviceInfo);
}
