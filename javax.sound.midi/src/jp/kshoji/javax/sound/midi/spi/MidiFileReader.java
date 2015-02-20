package jp.kshoji.javax.sound.midi.spi;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import jp.kshoji.javax.sound.midi.InvalidMidiDataException;
import jp.kshoji.javax.sound.midi.MidiFileFormat;
import jp.kshoji.javax.sound.midi.Sequence;

/**
 * Abstract class for MIDI File Reader
 *
 * @author K.Shoji
 */
public abstract class MidiFileReader {

    /**
     * Get the {@link MidiFileFormat} from the specified file
     *
     * @param file the file
     * @return MidiFileFormat
     * @throws InvalidMidiDataException
     * @throws IOException
     */
	public abstract MidiFileFormat getMidiFileFormat(File file) throws InvalidMidiDataException, IOException;

    /**
     * Get the {@link MidiFileFormat} from the specified stream
     *
     * @param stream the InputStream
     * @return MidiFileFormat
     * @throws InvalidMidiDataException
     * @throws IOException
     */
	public abstract MidiFileFormat getMidiFileFormat(InputStream stream) throws InvalidMidiDataException, IOException;

    /**
     * Get the {@link MidiFileFormat} from the specified URL
     *
     * @param url the URL
     * @return MidiFileFormat
     * @throws InvalidMidiDataException
     * @throws IOException
     */
	public abstract MidiFileFormat getMidiFileFormat(URL url) throws InvalidMidiDataException, IOException;

    /**
     * Get the {@link Sequence} from the specified file
     *
     * @param file the file
     * @return Sequence
     * @throws InvalidMidiDataException
     * @throws IOException
     */
	public abstract Sequence getSequence(File file) throws InvalidMidiDataException, IOException;

    /**
     * Get the {@link Sequence} from the specified stream
     *
     * @param stream the InputStream
     * @return Sequence
     * @throws InvalidMidiDataException
     * @throws IOException
     */
	public abstract Sequence getSequence(InputStream stream) throws InvalidMidiDataException, IOException;

    /**
     * Get the {@link Sequence} from the specified URL
     *
     * @param url the URL
     * @return Sequence
     * @throws InvalidMidiDataException
     * @throws IOException
     */
	public abstract Sequence getSequence(URL url) throws InvalidMidiDataException, IOException;

}
