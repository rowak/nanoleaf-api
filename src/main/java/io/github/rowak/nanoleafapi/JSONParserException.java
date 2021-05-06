package io.github.rowak.nanoleafapi;

/**
 * An exception that occurs when a JSON parsing error occurs.
 */
public class JSONParserException extends RuntimeException {

	/**
	 * Creates a new JSON parser exception with a message.
	 * 
	 * @param message   the message to send
	 */
	public JSONParserException(String message) {
		super(message);
	}
}
