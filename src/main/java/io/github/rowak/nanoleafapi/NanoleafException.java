package io.github.rowak.nanoleafapi;

/**
 * A NanoleafException indicates that a request to a Nanoleaf device failed. This is almost
 * always at the fault of the user. For example, a 422 (Unprocessable Entity) error code will
 * usually result if arguments are supplied to a request that are out of the allowed range.
 * See {@link NanoleafCallback} for more details on the reasons for this exception being thrown.
 */
public class NanoleafException extends Exception {
	
	private final static int[] ERROR_CODES = {400, 401, 403, 404, 422, 500};
	
	private int code;
	
	/**
	 * Creates a NanoleafException with no message.
	 */
	public NanoleafException() {}
	
	/**
	 * Creates a NanoleafException from its HTTP status code.
	 * 
	 * @param code   an HTTP status code
	 */
	public NanoleafException(int code) {
		super(getCodeMessage(code));
		this.code = code;
	}
	
	/**
	 * Gets the error code for this exception.
	 * 
	 * @return   the error code
	 */
	public int getCode() {
		return code;
	}
	
	/**
	 * Checks for error status codes.
	 * 
	 * @param code                 the response status code
	 * @throws NanoleafException   If the code matches an error status code
	 */
	public static void checkStatusCode(int code) throws NanoleafException {
		for (int e : ERROR_CODES) {
			if (code == e) {
				throw new NanoleafException(code);
			}
		}
	}
	
	/**
	 * Gets the error message for an error code.
	 * 
	 * @param code   the error code
	 * @return       an error message
	 */
	public static String getCodeMessage(int code) {
		switch (code) {
			case 400: return "400 (Bad request)";
			case 401: return "401 (Unauthorized)";
			case 403: return "403 (Forbidden)";
			case 404: return "404 (Not found)";
			case 422: return "422 (Unprocessable entity)";
			case 500: return "500 (Internal Server Error)";
			default:  return String.format("%d (Unknown)", code);
		}
	}
}
