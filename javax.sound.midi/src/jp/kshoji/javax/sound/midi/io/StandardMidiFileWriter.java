package jp.kshoji.javax.sound.midi.io;

import android.support.annotation.NonNull;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import jp.kshoji.javax.sound.midi.MetaMessage;
import jp.kshoji.javax.sound.midi.MidiEvent;
import jp.kshoji.javax.sound.midi.MidiFileFormat;
import jp.kshoji.javax.sound.midi.Sequence;
import jp.kshoji.javax.sound.midi.Track;
import jp.kshoji.javax.sound.midi.spi.MidiFileWriter;

/**
 * The implementation SMF writer
 *
 * @author K.Shoji
 */
public class StandardMidiFileWriter extends MidiFileWriter {

    /**
     * Represents OutputStream for MIDI Data
     *
     * @author K.Shoji
     */
    static class MidiDataOutputStream extends DataOutputStream {

        /**
         * Constructor
         *
         * @param outputStream the source stream
         */
		public MidiDataOutputStream(@NonNull OutputStream outputStream) {
			super(outputStream);
		}

        /**
         * Convert the specified value into the value for MIDI data
         *
         * @param value the original value
         * @return the raw data to write
         */
		private static int getValueToWrite(int value) {
			int result = value & 0x7f;
			int currentValue = value;

			while ((currentValue >>= 7) != 0) {
				result <<= 8;
				result |= ((currentValue & 0x7f) | 0x80);
			}
			return result;
		}

        /**
         * Get the data length for the specified value
         *
         * @param value the value
         * @return the data length
         */
		public static int variableLengthIntLength(int value) {
			int valueToWrite = getValueToWrite(value);

			int length = 0;
			while (true) {
				length++;
				
				if ((valueToWrite & 0x80) != 0) {
					valueToWrite >>>= 8;
				} else {
					break;
				}
			}

			return length;
		}

        /**
         * Write the specified value to the OutputStream
         *
         * @param value the value
         * @throws IOException
         */
		public synchronized void writeVariableLengthInt(int value) throws IOException {
			int valueToWrite = getValueToWrite(value);

			while (true) {
				writeByte(valueToWrite & 0xff);

				if ((valueToWrite & 0x80) != 0) {
					valueToWrite >>>= 8;
				} else {
					break;
				}
			}
		}
	}
	
	@NonNull
    @Override
	public int[] getMidiFileTypes() {
		return new int[] { 0, 1 };
	}

	@NonNull
    @Override
	public int[] getMidiFileTypes(@NonNull Sequence sequence) {
		if (sequence.getTracks().length > 1) {
			return new int[] { 1 };
		} else {
			return new int[] { 0, 1 };
		}
	}

	@Override
	public int write(@NonNull Sequence sequence, int fileType, @NonNull File file) throws IOException {
		FileOutputStream fileOutputStream = new FileOutputStream(file);
		int written = write(sequence, fileType, fileOutputStream);
		fileOutputStream.close();
		return written;
	}

	@Override
	public int write(@NonNull Sequence sequence, int fileType, @NonNull OutputStream outputStream) throws IOException {
		MidiDataOutputStream midiDataOutputStream = new MidiDataOutputStream(outputStream);

		Track[] tracks = sequence.getTracks();
		midiDataOutputStream.writeInt(MidiFileFormat.HEADER_MThd);
		midiDataOutputStream.writeInt(6);
		midiDataOutputStream.writeShort(fileType);
		midiDataOutputStream.writeShort(tracks.length);
		
		float divisionType = sequence.getDivisionType();
		int resolution = sequence.getResolution();
		int division = 0;
		if (divisionType == Sequence.PPQ) {
			division = resolution & 0x7fff;
		} else if (divisionType == Sequence.SMPTE_24) {
			division = (24 << 8) * -1;
			division += (resolution & 0xff);
		} else if (divisionType == Sequence.SMPTE_25) {
			division = (25 << 8) * -1;
			division += (resolution & 0xff);
		} else if (divisionType == Sequence.SMPTE_30DROP) {
			division = (29 << 8) * -1;
			division += (resolution & 0xff);
		} else if (divisionType == Sequence.SMPTE_30) {
			division = (30 << 8) * -1;
			division += (resolution & 0xff);
		}
		midiDataOutputStream.writeShort(division);
		
		int length = 0;
		for (int i = 0; i < tracks.length; i++) {
			length += writeTrack(tracks[i], midiDataOutputStream);
		}
		
		midiDataOutputStream.close();
		return length + 14;
	}

	/**
	 * Write {@link Track} data into {@link MidiDataOutputStream}
	 * 
	 * @param track the track
	 * @param midiDataOutputStream the OutputStream
	 * @return written byte length
	 * @throws IOException
	 */
	private static int writeTrack(@NonNull Track track, @NonNull MidiDataOutputStream midiDataOutputStream) throws IOException {
		int eventCount = track.size();
        int trackLength = 0;
        long lastTick = 0;
        boolean hasEndOfTrack = false;
        MidiEvent midiEvent = null;

        // track header
        midiDataOutputStream.writeInt(MidiFileFormat.HEADER_MTrk);

		// calculate the track length
		for (int i = 0; i < eventCount; i++) {
            midiEvent = track.get(i);
			long tick = midiEvent.getTick();
			trackLength += MidiDataOutputStream.variableLengthIntLength((int) (tick - lastTick));
			lastTick = tick;

			trackLength += midiEvent.getMessage().getLength();
		}

        // process End of Track message
        if (midiEvent != null && (midiEvent.getMessage() instanceof MetaMessage) && //
            ((MetaMessage)midiEvent.getMessage()).getType() == MetaMessage.TYPE_END_OF_TRACK) {
            hasEndOfTrack = true;
        } else {
            trackLength += 4; // End of Track
        }
        midiDataOutputStream.writeInt(trackLength);

        // write the track data
		lastTick = 0;
		for (int i = 0; i < eventCount; i++) {
            midiEvent = track.get(i);
            long tick = midiEvent.getTick();
			midiDataOutputStream.writeVariableLengthInt((int) (tick - lastTick));
			lastTick = tick;
			
			midiDataOutputStream.write(midiEvent.getMessage().getMessage(), 0, midiEvent.getMessage().getLength());
        }

        // write End of Track message if not found.
        if (!hasEndOfTrack) {
            midiDataOutputStream.writeVariableLengthInt(0);
            midiDataOutputStream.writeByte(MetaMessage.META);
            midiDataOutputStream.writeByte(MetaMessage.TYPE_END_OF_TRACK);
            midiDataOutputStream.writeVariableLengthInt(0);
        }

		return trackLength + 4;
	}
}
