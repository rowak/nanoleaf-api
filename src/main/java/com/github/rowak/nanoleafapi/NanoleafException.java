package com.github.rowak.nanoleafapi;

/**
 * A NanoleafException indicates that a request to a Nanoleaf device failed. This is almost
 * always at the fault of the user. For example, a 422 (Unprocessable Entity) error code will
 * usually result if arguments are supplied to a request that are out of the allowed range.
 * See {@link NanoleafCallback} for more details on the reasons for this exception being thrown.
 */
public class NanoleafException extends Exception {
	
	/**
	 * Creates a NanoleafException with no message.
	 */
	public NanoleafException() {}
	
	/**
	 * Creates a NanoleafException with a message.
	 * @param message   the message to add to the exception
	 */
	public NanoleafException(String message) {
		super(message);
	}
	
	/**
	 * Checks for error status codes.
	 * @param code                 the response status code
	 * @throws NanoleafException   If <code>code</code> matches an error status code
	 */
	public static void checkStatusCode(int code) throws NanoleafException {
		switch (code) {
			case 400:
				throw new NanoleafException("400 (Bad request)");
			case 401:
				throw new NanoleafException("401 (Unauthorized)");
			case 403:
				throw new NanoleafException("403 (Forbidden)");
			case 404:
				throw new NanoleafException("404 (Not found)");
			case 422:
				throw new NanoleafException("422 (Unprocessable entity)");
			case 500:
				throw new NanoleafException("500 (Internal Server Error)");
		}
	}
}
