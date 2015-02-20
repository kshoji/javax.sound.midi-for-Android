package jp.kshoji.javax.sound.midi;

/**
 * Interface for MIDI Soundbank
 *
 * @author K.Shoji
 */
public interface Soundbank {

    /**
     * Get the description string
     *
     * @return the description
     */
    String getDescription();

    /**
     * Get the vendor string
     *
     * @return the vendor
     */
    String getVendor();

    /**
     * Get the version string
     *
     * @return the version
     */
    String getVersion();

    /**
     * Get the {@link Instrument}
     *
     * @param patch the {@link Patch}
     * @return {@link Instrument} matches with patch
     */
    Instrument getInstrument(Patch patch);

    /**
     * Get all of {@link Instrument}s
     *
     * @return the array of {@link Instrument}s
     */
    Instrument[] getInstruments();

    /**
     * Get the name of Soundbank
     *
     * @return the name of Soundbank
     */
    String getName();

    /**
     * Get all of {@link SoundbankResource}s
     * @return the array of {@link SoundbankResource}s
     */
    SoundbankResource[] getResources();
}
