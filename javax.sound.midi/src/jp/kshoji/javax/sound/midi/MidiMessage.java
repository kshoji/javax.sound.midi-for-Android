package jp.kshoji.javax.sound.midi;

/**
 * Abstract class for MIDI Message
 *
 * @author K.Shoji
 */
public abstract class MidiMessage implements Cloneable {
	protected byte[] data;
    protected int length;

    /**
     * Constructor with the raw data
     *
     * @param data the raw data
     */
	protected MidiMessage(byte[] data) {
		this.data = data;

        if (data == null) {
            length = 0;
        } else {
            length = data.length;
        }
	}

	/**
     * Constructor with the raw data, and its length
	 *
	 * @param data the raw data
	 * @param length unused parameter. Use always data.length
	 * @throws InvalidMidiDataException
	 */
	protected void setMessage(byte[] data, int length) throws InvalidMidiDataException {
		if (this.data == null || this.data.length != data.length) {
			this.data = new byte[data.length];
		}
        this.length = data.length;
		System.arraycopy(data, 0, this.data, 0, data.length);
	}

    /**
     * Get the message source data
     *
     * @return the message source data
     */
	public byte[] getMessage() {
		if (data == null) {
			return null;
		}
		byte[] resultArray = new byte[data.length];
		System.arraycopy(data, 0, resultArray, 0, data.length);
		return resultArray;
	}

    /**
     * Get the status of the {@link MidiMessage}
     *
     * @return the status
     */
	public int getStatus() {
		if (data != null && data.length > 0) {
			return (data[0] & 0xff);
		}
		return 0;
	}

    /**
     * Get the length of the {@link MidiMessage}
     *
     * @return the length
     */
	public int getLength() {
		if (data == null) {
			return 0;
		}
		return data.length;
	}

    /**
     * Convert the byte array to the hex dumped string
     *
     * @param src the byte array
     * @return hex dumped string
     */
	static String toHexString(byte[] src) {
        if (src == null) {
            return "null";
        }

		StringBuilder buffer = new StringBuilder();
		buffer.append("[");
		boolean needComma = false;
		for (byte srcByte : src) {
			if (needComma) {
				buffer.append(", ");
			}
			buffer.append(String.format("%02x", srcByte & 0xff));
			needComma = true;
		}
		buffer.append("]");
		
		return buffer.toString();
	}
	
	@Override
	public String toString() {
		return getClass().getName() + ":" + toHexString(data);
	}

    /**
     * Clone the object
     *
     * @return the clone of this object instance
     */
	public abstract Object clone();
}
