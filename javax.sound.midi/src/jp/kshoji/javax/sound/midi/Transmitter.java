package jp.kshoji.javax.sound.midi;

/**
 * Interface for MIDI Transmitter.
 * 
 * @author K.Shoji
 */
public interface Transmitter {

	/**
	 * Set the {@link Receiver} for this {@link Transmitter}
     *
	 * @param receiver the Receiver
	 */
	void setReceiver(Receiver receiver);

	/**
	 * Get the {@link Receiver} for this {@link Transmitter}
     *
	 * @return the Receiver
	 */
	Receiver getReceiver();

	/**
	 * Close this {@link Transmitter}
	 */
	void close();
}
