package jp.kshoji.javax.sound.midi.listener;

import android.support.annotation.NonNull;

import jp.kshoji.javax.sound.midi.MidiDevice;

/**
 * Listener for MIDI attached events
 */
public interface OnMidiDeviceAttachedListener {

    /**
     * MIDI device has been attached
     *
     * @param midiDevice attached MIDI device
     */
    void onMidiDeviceAttached(@NonNull MidiDevice.Info midiDeviceInfo);
}
