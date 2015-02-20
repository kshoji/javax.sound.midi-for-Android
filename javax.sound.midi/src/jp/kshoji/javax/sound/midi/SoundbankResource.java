package jp.kshoji.javax.sound.midi;

/**
 * Interface for MIDI Soundbank resource
 *
 * @author K.Shoji
 */
public abstract class SoundbankResource {
    private final Soundbank soundbank;
    private final String name;
    private final Class<?> dataClass;

    /**
     * Constructor
     *
     * @param soundbank the Soundbank
     * @param name the name of {@link SoundbankResource}
     * @param dataClass the class of data
     */
    protected SoundbankResource(Soundbank soundbank, String name, Class<?> dataClass) {
        this.soundbank = soundbank;
        this.name = name;
        this.dataClass = dataClass;
    }

    /**
     * Get the data of {@link SoundbankResource}
     *
     * @return the data
     */
    public abstract Object getData();

    /**
     * Get the class of data(obtained by {@link #getData()}
     *
     * @return the class
     */
    public Class<?> getDataClass() {
        return dataClass;
    }

    /**
     * Get the name of {@link SoundbankResource}
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the {@link Soundbank}
     *
     * @return the Soundbank
     */
    public Soundbank getSoundbank() {
        return soundbank;
    }
}
