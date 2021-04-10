package com.github.rowak.nanoleafapi;

public class NanoleafException extends Exception {
	public NanoleafException() {}
	
	public NanoleafException(String message) {
		super(message);
	}
	
	/**
	 * Checks for error status codes.
	 * @param code  the response status code
	 * @throws NanoleafException  if <code>code</code> matches an error status code
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
