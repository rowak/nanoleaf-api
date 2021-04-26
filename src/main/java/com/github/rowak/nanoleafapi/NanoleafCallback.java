package com.github.rowak.nanoleafapi;

import com.github.rowak.nanoleafapi.util.HttpUtil;

/**
 * The main callback interface for asynchronous requests.
 * @param <T>   the type that is being requested/returned
 */
public interface NanoleafCallback<T> {
	
	/** An internal API error, or the device returned an impossible (but http-allowable) response.
	 *  This status code indicates that the returned data in the callback object is invalid.
	 *  For example, if a method is expecting an integer and the device returns a string that
	 *  can't be parsed into an integer, a FAILURE status code will be returned instead of the
	 *  status code from the device, and the data will be set to 0. **/
	public static final int FAILURE = 0;
	
	/** The request was successful. The device returned 200 (OK) or 204 (No Content). **/
	public static final int SUCCESS = 1;
	
	/** HTTP 200 (OK); for successful requests with content. **/
	public static final int OK = HttpUtil.OK;
	
	/** HTTP 204 (No Content); for successful requests with no content. **/
	public static final int NO_CONTENT = HttpUtil.NO_CONTENT;
	
	/** HTTP 400 (Bad Request); for malformed requests. **/
	public static final int BAD_REQUEST = HttpUtil.BAD_REQUEST;
	
	/** HTTP 401 (Unauthorized); for missing/invalid access tokens. **/
	public static final int UNAUTHORIZED = HttpUtil.UNAUTHORIZED;
	
	/** HTTP 403 (Forbidden) **/
	public static final int FORBIDDEN = HttpUtil.FORBIDDEN;
	
	/** HTTP 404 (Resource Not Found); for non-existing endpoints, or non-existing
	 * resources requested from arguments. **/
	public static final int NOT_FOUND = HttpUtil.NOT_FOUND;
	
	/** HTTP 422 (Unprocessable Entity); for invalid arguments. **/
	public static final int UNPROCESSABLE_ENTITY = HttpUtil.UNPROCESSABLE_ENTITY;
	
	/** HTTP 500 (Internal Server Error); unknown what causes this. **/
	public static final int INTERNAL_SERVER_ERROR = HttpUtil.INTERNAL_SERVER_ERROR;
	
	/**
	 * Signals that an asynchronous request has completed.
	 * Possible status response codes include:
	 * <ul>
	 * <li>{@link NanoleafCallback#SUCCESS}</li>
	 * <li>{@link NanoleafCallback#FAILURE}</li>
	 * <li>{@link NanoleafCallback#OK}</li>
	 * <li>{@link NanoleafCallback#NO_CONTENT}</li>
	 * <li>{@link NanoleafCallback#BAD_REQUEST}</li>
	 * <li>{@link NanoleafCallback#UNAUTHORIZED}</li>
	 * <li>{@link NanoleafCallback#FORBIDDEN}</li>
	 * <li>{@link NanoleafCallback#NOT_FOUND}</li>
	 * <li>{@link NanoleafCallback#UNPROCESSABLE_ENTITY}</li>
	 * <li>{@link NanoleafCallback#INTERNAL_SERVER_ERROR}</li>
	 * </ul>
	 * 
	 * @param status   the response status (see above status codes)
	 * @param data     the response data; only set when the status code is SUCCESS (1),
	 * 				   otherwise null
	 * @param device   the device that has completed
	 */
	public void onCompleted(int status, T data, NanoleafDevice device);
}
