package jp.kshoji.javax.sound.midi;

/**
 * Represents MIDI Patch
 *
 * @author K.Shoji
 */
public class Patch {
    private final int bank;
    private final int program;

    /**
     * Constructor
     *
     * @param bank the bank of {@link jp.kshoji.javax.sound.midi.Patch}
     * @param program the program of {@link jp.kshoji.javax.sound.midi.Patch}
     */
    public Patch(int bank, int program) {
        this.bank = bank;
        this.program = program;
    }

    /**
     * Get the bank of {@link jp.kshoji.javax.sound.midi.Patch}
     *
     * @return the bank of {@link jp.kshoji.javax.sound.midi.Patch}
     */
    public int getBank() {
        return bank;
    }

    /**
     * Get the program of {@link jp.kshoji.javax.sound.midi.Patch}
     *
     * @return the program of {@link jp.kshoji.javax.sound.midi.Patch}
     */
    public int getProgram() {
        return program;
    }
}
