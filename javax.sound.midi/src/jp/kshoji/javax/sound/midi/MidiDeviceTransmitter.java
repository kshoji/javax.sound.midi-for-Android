package jp.kshoji.javax.sound.midi;

/**
 * Interface for {@link MidiDevice} transmitter.
 *
 * @author K.Shoji
 */
public interface MidiDeviceTransmitter extends Transmitter {

    /**
     * Get the {@link jp.kshoji.javax.sound.midi.MidiDevice} associated with this instance.
     *
     * @return the {@link jp.kshoji.javax.sound.midi.MidiDevice} associated with this instance.
     */
    MidiDevice getMidiDevice();
}
