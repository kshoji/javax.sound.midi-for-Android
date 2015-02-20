package jp.kshoji.javax.sound.midi;

/**
 * Interface for MIDI Synthesizer
 *
 * @author K.Shoji
 */
public interface Synthesizer extends MidiDevice {

    /**
     * Get the all of {@link MidiChannel}s
     *
     * @return the array of MidiChannel
     */
    MidiChannel[] getChannels();

    /**
     * Get the latency in microseconds
     *
     * @return the latency in microseconds
     */
    long getLatency();

    /**
     * Get the maximum count of polyphony
     *
     * @return the maximum count of polyphony
     */
    int getMaxPolyphony();

    /**
     * Get the current {@link VoiceStatus} of the Synthesizer
     *
     * @return the array of VoiceStatus
     */
    VoiceStatus[] getVoiceStatus();

    /**
     * Get the default {@link Soundbank}
     *
     * @return the Soundbank
     */
    Soundbank getDefaultSoundbank();

    /**
     * Check if the specified {@link Soundbank} is supported
     *
     * @param soundbank the Soundbank
     * @return true if the Soundbank is supported
     */
    boolean isSoundbankSupported(Soundbank soundbank);

    /**
     * Get the all available {@link Instrument}s
     *
     * @return the array of Instrument
     */
    Instrument[] getAvailableInstruments();

    /**
     * Get the all loaded {@link Instrument}s
     *
     * @return the array of Instrument
     */
    Instrument[] getLoadedInstruments();

    /**
     * Remap an Instrument
     *
     * @param from to be replaced
     * @param to the new Instrument
     * @return true if succeed to remap
     */
    boolean remapInstrument(Instrument from, Instrument to);

    /**
     * Load all instruments belongs specified {@link Soundbank}
     *
     * @param soundbank the Soundbank
     * @return true if succeed to load
     */
    boolean loadAllInstruments(Soundbank soundbank);

    /**
     * Unload all instruments belongs specified {@link Soundbank}
     *
     * @param soundbank the Soundbank
     */
    void unloadAllInstruments(Soundbank soundbank);

    /**
     * Load the specified {@link Instrument}
     *
     * @param instrument the instrument
     * @return true if succeed to load
     */
    boolean loadInstrument(Instrument instrument);

    /**
     * Unload the specified {@link Instrument}
     *
     * @param instrument the instrument
     */
    void unloadInstrument(Instrument instrument);

    /**
     * Load all instruments belongs specified {@link Soundbank} and {@link Patch}es
     *
     * @param soundbank the the Soundbank
     * @param patchList the array of Patch
     * @return true if succeed to load
     */
    boolean loadInstruments(Soundbank soundbank, Patch[] patchList);

    /**
     * Unload all instruments belongs specified {@link Soundbank} and {@link Patch}es
     *
     * @param soundbank the the Soundbank
     * @param patchList the array of Patch
     */
    void unloadInstruments(Soundbank soundbank, Patch[] patchList);
}
