package jp.kshoji.javax.sound.midi;

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
    static void addMidiDevice(MidiDevice midiDevice) {
        synchronized (midiDevices) {
            midiDevices.add(midiDevice);
        }
    }

    /**
     * Remove a {@link jp.kshoji.javax.sound.midi.MidiDevice} from the {@link jp.kshoji.javax.sound.midi.MidiSystem}
     *
     * @param midiDevice the device to remove
     */
    static void removeMidiDevice(MidiDevice midiDevice) {
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
		 * @return
		 * @throws MidiUnavailableException
		 */
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
		 * @return
		 * @throws MidiUnavailableException
		 */
		public static List<Transmitter> getTransmitters() throws MidiUnavailableException {
			List<Transmitter> result = new ArrayList<Transmitter>();
			Info[] midiDeviceInfos = MidiSystem.getMidiDeviceInfo();
			for (Info midiDeviceInfo : midiDeviceInfos) {
				result.addAll(MidiSystem.getMidiDevice(midiDeviceInfo).getTransmitters());
			}

			return result;
		}
	}

	private MidiSystem() {
	}

	/**
	 * get all connected {@link MidiDevice.Info} as array
	 *
	 * @return device information
	 */
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
	 * get {@link MidiDevice} by device information
	 *
	 * @param info
	 * @return {@link MidiDevice}
	 * @throws MidiUnavailableException
	 * @throws IllegalArgumentException if the device not found.
	 */
	public static MidiDevice getMidiDevice(MidiDevice.Info info) throws MidiUnavailableException, IllegalArgumentException {
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
	 * get the first detected Receiver
	 *
	 * @return {@link Receiver}
	 * @throws MidiUnavailableException
	 */
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
	 * get the first detected Transmitter
	 *
	 * @return {@link Transmitter}
	 * @throws MidiUnavailableException
	 */
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
	 * get a {@link Sequence} from the specified File.
	 *
	 * @param file
	 * @return
	 * @throws InvalidMidiDataException
	 * @throws IOException
	 */
	public static Sequence getSequence(File file) throws InvalidMidiDataException, IOException {
		StandardMidiFileReader standardMidiFileReader = new StandardMidiFileReader();
		return standardMidiFileReader.getSequence(file);
	}

	/**
	 * get a {@link Sequence} from the specified input stream.
	 *
	 * @param stream
	 * @return
	 * @throws InvalidMidiDataException
	 * @throws IOException
	 */
	public static Sequence getSequence(InputStream stream) throws InvalidMidiDataException, IOException {
		StandardMidiFileReader standardMidiFileReader = new StandardMidiFileReader();
		return standardMidiFileReader.getSequence(stream);
	}

	/**
	 * get a {@link Sequence} from the specified URL.
	 * @param url
	 * @return
	 * @throws InvalidMidiDataException
	 * @throws IOException
	 */
	public static Sequence	getSequence(URL url) throws InvalidMidiDataException, IOException {
		StandardMidiFileReader standardMidiFileReader = new StandardMidiFileReader();
		return standardMidiFileReader.getSequence(url);
	}

	/**
	 * get the default {@link Sequencer}, connected to a default device.
	 *
	 * @return {@link Sequencer} must call the {@link Sequencer#open()} method.
	 * @throws MidiUnavailableException
	 */
	public static Sequencer	getSequencer() throws MidiUnavailableException {
		return new SequencerImpl();
	}

	/**
	 * get the default {@link Sequencer}, optionally connected to a default device.
	 *
	 * @param connected ignored
	 * @return {@link Sequencer} must call the {@link Sequencer#open()} method.
	 * @throws MidiUnavailableException
	 */
	public static Sequencer	getSequencer(boolean connected) throws MidiUnavailableException {
		return new SequencerImpl();
	}

    /**
     * Obtain {@link jp.kshoji.javax.sound.midi.Soundbank} from File<br />
     * not implemented.
     *
     * @param file
     * @return
     * @throws InvalidMidiDataException
     * @throws IOException
     */
    public static Soundbank getSoundbank(File file) throws InvalidMidiDataException, IOException {
        throw new UnsupportedOperationException("not implemented.");
    }

    /**
     * Obtain {@link jp.kshoji.javax.sound.midi.Soundbank} from InputStream<br />
     * not implemented.
     *
     * @param stream
     * @return
     * @throws InvalidMidiDataException
     * @throws IOException
     */
    public static Soundbank getSoundbank(InputStream stream) throws InvalidMidiDataException, IOException {
        throw new UnsupportedOperationException("not implemented.");
    }

    /**
     * Obtain {@link jp.kshoji.javax.sound.midi.Soundbank} from URL<br />
     * not implemented.
     *
     * @param url
     * @return
     * @throws InvalidMidiDataException
     * @throws IOException
     */
    public static Soundbank getSoundbank(URL url) throws InvalidMidiDataException, IOException {
        throw new UnsupportedOperationException("not implemented.");
    }

    private static final Set<Synthesizer> synthesizers = new HashSet<Synthesizer>();

    /**
     * Obtains {@link jp.kshoji.javax.sound.midi.Synthesizer} registered by {@link #registerSynthesizer(Synthesizer)}
     * @return a Synthesizer, null if instance has not registered
     * @throws MidiUnavailableException
     */
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
     * Registers a {@link jp.kshoji.javax.sound.midi.Synthesizer} instance.
     * @param synthesizer
     */
    public static void registerSynthesizer(Synthesizer synthesizer) {
        if (synthesizer != null) {
            synthesizers.add(synthesizer);
        }
    }

	/**
	 * get the {@link MidiFileFormat} information of the specified File.
	 * 
	 * @param file
	 * @return
	 * @throws InvalidMidiDataException
	 * @throws IOException
	 */
	public static MidiFileFormat getMidiFileFormat(File file) throws InvalidMidiDataException, IOException {
		StandardMidiFileReader standardMidiFileReader = new StandardMidiFileReader();
		return standardMidiFileReader.getMidiFileFormat(file);
	}

	/**
	 * get the {@link MidiFileFormat} information in the specified input stream.
	 * 
	 * @param stream
	 * @return
	 * @throws InvalidMidiDataException
	 * @throws IOException
	 */
	public static MidiFileFormat getMidiFileFormat(InputStream stream) throws InvalidMidiDataException, IOException {
		StandardMidiFileReader standardMidiFileReader = new StandardMidiFileReader();
		return standardMidiFileReader.getMidiFileFormat(stream);
	}

	/**
	 * get the {@link MidiFileFormat} information in the specified URL.
	 * 
	 * @param url
	 * @return
	 * @throws InvalidMidiDataException
	 * @throws IOException
	 */
	public static MidiFileFormat getMidiFileFormat(URL url) throws InvalidMidiDataException, IOException {
		StandardMidiFileReader standardMidiFileReader = new StandardMidiFileReader();
		return standardMidiFileReader.getMidiFileFormat(url);
	}

	/**
	 * get the set of SMF types that the library can write
	 * 
	 * @return
	 */
	public static int[] getMidiFileTypes() {
		StandardMidiFileWriter standardMidiFileWriter = new StandardMidiFileWriter();
		return standardMidiFileWriter.getMidiFileTypes();
	}

	/**
	 * get the set of SMF types that the library can write from the {@link Sequence}
	 * 
	 * @param sequence
	 * @return
	 */
	public static int[] getMidiFileTypes(Sequence sequence) {
		StandardMidiFileWriter standardMidiFileWriter = new StandardMidiFileWriter();
		return standardMidiFileWriter.getMidiFileTypes(sequence);
	}
	
	/**
	 * check if the specified SMF fileType is available
	 * 
	 * @param fileType
	 * @return
	 */
	public static boolean isFileTypeSupported(int fileType) {
		StandardMidiFileWriter standardMidiFileWriter = new StandardMidiFileWriter();
		return standardMidiFileWriter.isFileTypeSupported(fileType);
	}

	/**
	 * check if the specified SMF fileType is available from the {@link Sequence}
	 * 
	 * @param fileType
	 * @param sequence
	 * @return
	 */
	public static boolean isFileTypeSupported(int fileType, Sequence sequence) {
		StandardMidiFileWriter standardMidiFileWriter = new StandardMidiFileWriter();
		return standardMidiFileWriter.isFileTypeSupported(fileType, sequence);
	}

	/**
	 * write sequence to the specified {@link File} as SMF
	 * 
	 * @param sequence
	 * @param fileType
	 * @param file
	 * @return
	 * @throws IOException
	 */
    public static int write(Sequence sequence, int fileType, File file) throws IOException {
		StandardMidiFileWriter standardMidiFileWriter = new StandardMidiFileWriter();
		return standardMidiFileWriter.write(sequence, fileType, file);
	}

	/**
	 * write sequence to the specified {@link OutputStream} as SMF
	 * 
	 * @param sequence
	 * @param fileType
	 * @param outputStream
	 * @return
	 * @throws IOException
	 */
    public static int write(Sequence sequence, int fileType, OutputStream outputStream) throws IOException {
		StandardMidiFileWriter standardMidiFileWriter = new StandardMidiFileWriter();
		return standardMidiFileWriter.write(sequence, fileType, outputStream);
	}
}
