package jp.kshoji.javax.sound.midi;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jp.kshoji.javax.sound.midi.MidiDevice.Info;
import jp.kshoji.javax.sound.midi.impl.SequencerImpl;
import jp.kshoji.javax.sound.midi.io.StandardMidiFileReader;
import jp.kshoji.javax.sound.midi.io.StandardMidiFileWriter;

/**
 * MidiSystem porting for Android
 *
 * @author K.Shoji
 */
public final class MidiSystem {
	static final Set<MidiDevice> midiDevices = new HashSet<MidiDevice>();

    /**
     * Add a {@link jp.kshoji.javax.sound.midi.MidiDevice} to the {@link jp.kshoji.javax.sound.midi.MidiSystem}
     *
     * @param midiDevice the device to add
     */
    public static void addMidiDevice(@NonNull MidiDevice midiDevice) {
        synchronized (midiDevices) {
            midiDevices.add(midiDevice);
        }
    }

    /**
     * Remove a {@link jp.kshoji.javax.sound.midi.MidiDevice} from the {@link jp.kshoji.javax.sound.midi.MidiSystem}
     *
     * @param midiDevice the device to remove
     */
    public static void removeMidiDevice(@NonNull MidiDevice midiDevice) {
        synchronized (midiDevices) {
            midiDevices.remove(midiDevice);
        }
    }

    /**
	 * Utilities for {@link MidiSystem}
	 *
	 * @author K.Shoji
	 */
	public static class MidiSystemUtils {
		/**
		 * Get currently connected {@link Receiver}s
		 *
		 * @return currently connected {@link Receiver}s
		 * @throws MidiUnavailableException
		 */
        @NonNull
        public static List<Receiver> getReceivers() throws MidiUnavailableException {
			List<Receiver> result = new ArrayList<Receiver>();
			Info[] midiDeviceInfos = MidiSystem.getMidiDeviceInfo();
			for (Info midiDeviceInfo : midiDeviceInfos) {
				result.addAll(MidiSystem.getMidiDevice(midiDeviceInfo).getReceivers());
			}

			return result;
		}

		/**
		 * Get currently connected {@link Transmitter}s
		 *
		 * @return currently connected {@link Transmitter}s
		 * @throws MidiUnavailableException
		 */
        @NonNull
        public static List<Transmitter> getTransmitters() throws MidiUnavailableException {
			List<Transmitter> result = new ArrayList<Transmitter>();
			Info[] midiDeviceInfos = MidiSystem.getMidiDeviceInfo();
			for (Info midiDeviceInfo : midiDeviceInfos) {
				result.addAll(MidiSystem.getMidiDevice(midiDeviceInfo).getTransmitters());
			}

			return result;
		}
	}

    /**
     * Private Constructor; this class can't be instantiated.
     */
	private MidiSystem() {
	}

	/**
	 * Get all connected {@link MidiDevice.Info} as array
	 *
	 * @return device information
	 */
    @NonNull
    public static MidiDevice.Info[] getMidiDeviceInfo() {
		List<MidiDevice.Info> result = new ArrayList<MidiDevice.Info>();
		synchronized (midiDevices) {
            for (MidiDevice device : midiDevices) {
                result.add(device.getDeviceInfo());
            }
		}
		return result.toArray(new MidiDevice.Info[result.size()]);
	}

	/**
	 * Get {@link MidiDevice} by device information
	 *
	 * @param info the device information
	 * @return {@link MidiDevice}
	 * @throws MidiUnavailableException
	 * @throws IllegalArgumentException if the device not found.
	 */
    @NonNull
    public static MidiDevice getMidiDevice(@NonNull MidiDevice.Info info) throws MidiUnavailableException, IllegalArgumentException {
        synchronized (midiDevices) {
            for (MidiDevice midiDevice : midiDevices) {
                if (info.equals(midiDevice.getDeviceInfo())) {
                    return midiDevice;
                }
            }
		}

		throw new IllegalArgumentException("Requested device not installed: " + info);
	}

	/**
	 * Get the first detected Receiver
	 *
	 * @return {@link Receiver}
	 * @throws MidiUnavailableException
	 */
    @Nullable
    public static Receiver getReceiver() throws MidiUnavailableException {
        synchronized (midiDevices) {
            for (MidiDevice midiDevice : midiDevices) {
                Receiver receiver = midiDevice.getReceiver();
                if (receiver != null) {
                    return receiver;
                }
            }
		}
		return null;
	}

	/**
	 * Get the first detected Transmitter
	 *
	 * @return {@link Transmitter}
	 * @throws MidiUnavailableException
	 */
    @Nullable
    public static Transmitter getTransmitter() throws MidiUnavailableException {
        synchronized (midiDevices) {
            for (MidiDevice midiDevice : midiDevices) {
                Transmitter transmitter = midiDevice.getTransmitter();
                if (transmitter != null) {
                    return transmitter;
                }
            }
		}
		return null;
	}

	/**
	 * Get a {@link Sequence} from the specified File.
	 *
	 * @param file the SMF
	 * @return the {@link Sequence}
	 * @throws InvalidMidiDataException
	 * @throws IOException
	 */
    @NonNull
    public static Sequence getSequence(@NonNull File file) throws InvalidMidiDataException, IOException {
		StandardMidiFileReader standardMidiFileReader = new StandardMidiFileReader();
		return standardMidiFileReader.getSequence(file);
	}

	/**
	 * Get a {@link Sequence} from the specified input stream.
	 *
	 * @param stream the input stream of SMF
     * @return the {@link Sequence}
	 * @throws InvalidMidiDataException
	 * @throws IOException
	 */
    @NonNull
    public static Sequence getSequence(@NonNull InputStream stream) throws InvalidMidiDataException, IOException {
		StandardMidiFileReader standardMidiFileReader = new StandardMidiFileReader();
		return standardMidiFileReader.getSequence(stream);
	}

	/**
	 * Get a {@link Sequence} from the specified URL.
     *
	 * @param url the URL of SMF
     * @return the {@link Sequence}
	 * @throws InvalidMidiDataException
	 * @throws IOException
	 */
    @NonNull
    public static Sequence getSequence(@NonNull URL url) throws InvalidMidiDataException, IOException {
		StandardMidiFileReader standardMidiFileReader = new StandardMidiFileReader();
		return standardMidiFileReader.getSequence(url);
	}

	/**
	 * Get the default {@link Sequencer}, connected to a default device.
	 *
	 * @return {@link Sequencer} must call the {@link Sequencer#open()} method.
	 * @throws MidiUnavailableException
	 */
    @NonNull
    public static Sequencer getSequencer() throws MidiUnavailableException {
		return new SequencerImpl();
	}

	/**
	 * Get the default {@link Sequencer}, optionally connected to a default device.
	 *
	 * @param connected ignored
	 * @return {@link Sequencer} must call the {@link Sequencer#open()} method.
	 * @throws MidiUnavailableException
	 */
    @NonNull
    public static Sequencer getSequencer(boolean connected) throws MidiUnavailableException {
		return new SequencerImpl();
	}

    /**
     * Obtain {@link jp.kshoji.javax.sound.midi.Soundbank} from File<br />
     * not implemented.
     *
     * @param file the Soundbank file
     * @return {@link jp.kshoji.javax.sound.midi.Soundbank}
     * @throws InvalidMidiDataException
     * @throws IOException
     */
    @NonNull
    public static Soundbank getSoundbank(@NonNull File file) throws InvalidMidiDataException, IOException {
        throw new UnsupportedOperationException("not implemented.");
    }

    /**
     * Obtain {@link jp.kshoji.javax.sound.midi.Soundbank} from InputStream<br />
     * not implemented.
     *
     * @param stream the input stream of Soundbank
     * @return {@link jp.kshoji.javax.sound.midi.Soundbank}
     * @throws InvalidMidiDataException
     * @throws IOException
     */
    @NonNull
    public static Soundbank getSoundbank(@NonNull InputStream stream) throws InvalidMidiDataException, IOException {
        throw new UnsupportedOperationException("not implemented.");
    }

    /**
     * Obtain {@link jp.kshoji.javax.sound.midi.Soundbank} from URL<br />
     * not implemented.
     *
     * @param url the URL of Soundbank
     * @return {@link jp.kshoji.javax.sound.midi.Soundbank}
     * @throws InvalidMidiDataException
     * @throws IOException
     */
    @NonNull
    public static Soundbank getSoundbank(@NonNull URL url) throws InvalidMidiDataException, IOException {
        throw new UnsupportedOperationException("not implemented.");
    }

    private static final Set<Synthesizer> synthesizers = new HashSet<Synthesizer>();

    /**
     * Obtain {@link jp.kshoji.javax.sound.midi.Synthesizer} registered by {@link #registerSynthesizer(Synthesizer)}
     *
     * @return a Synthesizer, null if instance has not registered
     * @throws MidiUnavailableException
     */
    @Nullable
    public static Synthesizer getSynthesizer() throws MidiUnavailableException {
        if (synthesizers.size() == 0) {
            return null;
        }

        for (Synthesizer synthesizer : synthesizers) {
            // returns the first one
            return synthesizer;
        }

        return null;
    }

    /**
     * Register the {@link jp.kshoji.javax.sound.midi.Synthesizer} instance to the {@link MidiSystem}.
     *
     * @param synthesizer the {@link jp.kshoji.javax.sound.midi.Synthesizer} instance
     */
    public static void registerSynthesizer(@NonNull Synthesizer synthesizer) {
        synthesizers.add(synthesizer);
    }

	/**
	 * Get the {@link MidiFileFormat} information of the specified File.
	 * 
	 * @param file the SMF
	 * @return the {@link MidiFileFormat} information
	 * @throws InvalidMidiDataException
	 * @throws IOException
	 */
    @NonNull
    public static MidiFileFormat getMidiFileFormat(@NonNull File file) throws InvalidMidiDataException, IOException {
		StandardMidiFileReader standardMidiFileReader = new StandardMidiFileReader();
		return standardMidiFileReader.getMidiFileFormat(file);
	}

	/**
	 * Get the {@link MidiFileFormat} information in the specified input stream.
	 * 
	 * @param stream the the input stream of SMF
     * @return the {@link MidiFileFormat} information
	 * @throws InvalidMidiDataException
	 * @throws IOException
	 */
    @NonNull
    public static MidiFileFormat getMidiFileFormat(@NonNull InputStream stream) throws InvalidMidiDataException, IOException {
		StandardMidiFileReader standardMidiFileReader = new StandardMidiFileReader();
		return standardMidiFileReader.getMidiFileFormat(stream);
	}

	/**
	 * Get the {@link MidiFileFormat} information in the specified URL.
	 * 
	 * @param url the URL of SMF
     * @return the {@link MidiFileFormat} information
	 * @throws InvalidMidiDataException
	 * @throws IOException
	 */
    @NonNull
    public static MidiFileFormat getMidiFileFormat(@NonNull URL url) throws InvalidMidiDataException, IOException {
		StandardMidiFileReader standardMidiFileReader = new StandardMidiFileReader();
		return standardMidiFileReader.getMidiFileFormat(url);
	}

	/**
	 * Get the set of SMF types that the library can write
	 * 
	 * @return the set of SMF types
	 */
    @NonNull
    public static int[] getMidiFileTypes() {
		StandardMidiFileWriter standardMidiFileWriter = new StandardMidiFileWriter();
		return standardMidiFileWriter.getMidiFileTypes();
	}

	/**
	 * Get the set of SMF types that the library can write from the {@link Sequence}
	 * 
	 * @param sequence the {@link Sequence}
	 * @return the set of SMF types
	 */
    @NonNull
    public static int[] getMidiFileTypes(@NonNull Sequence sequence) {
		StandardMidiFileWriter standardMidiFileWriter = new StandardMidiFileWriter();
		return standardMidiFileWriter.getMidiFileTypes(sequence);
	}
	
	/**
	 * Check if the specified SMF fileType is available
	 * 
	 * @param fileType the fileType of SMF
	 * @return true if the fileType is available
	 */
	public static boolean isFileTypeSupported(int fileType) {
		StandardMidiFileWriter standardMidiFileWriter = new StandardMidiFileWriter();
		return standardMidiFileWriter.isFileTypeSupported(fileType);
	}

	/**
	 * Check if the specified SMF fileType is available from the {@link Sequence}
	 * 
	 * @param fileType the fileType of {@link Sequence}
	 * @param sequence the {@link Sequence}
     * @return true if the fileType is available
	 */
	public static boolean isFileTypeSupported(int fileType, @NonNull Sequence sequence) {
		StandardMidiFileWriter standardMidiFileWriter = new StandardMidiFileWriter();
		return standardMidiFileWriter.isFileTypeSupported(fileType, sequence);
	}

	/**
	 * Write sequence to the specified {@link File} as SMF
	 * 
	 * @param sequence the {@link Sequence}
	 * @param fileType the fileType of {@link Sequence}
	 * @param file the {@link File} to write
	 * @return the file length
	 * @throws IOException
	 */
    public static int write(@NonNull Sequence sequence, int fileType, @NonNull File file) throws IOException {
		StandardMidiFileWriter standardMidiFileWriter = new StandardMidiFileWriter();
		return standardMidiFileWriter.write(sequence, fileType, file);
	}

	/**
	 * Write sequence to the specified {@link OutputStream} as SMF
	 *
     * @param sequence the {@link Sequence}
     * @param fileType the fileType of {@link Sequence}
	 * @param outputStream the {@link OutputStream} to write
     * @return the file length
	 * @throws IOException
	 */
    public static int write(@NonNull Sequence sequence, int fileType, @NonNull OutputStream outputStream) throws IOException {
		StandardMidiFileWriter standardMidiFileWriter = new StandardMidiFileWriter();
		return standardMidiFileWriter.write(sequence, fileType, outputStream);
	}
}
