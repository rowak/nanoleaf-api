package io.github.rowak.nanoleafapi;

import java.io.IOException;

import okhttp3.OkHttpClient;

/**
 * The Canvas class is one of the three main concrete classes for creating a NanoleafDevice.
 * This class should only be used to connect to physical Canvas devices. For other devices
 * such as the Aurora or Hexagons, use the Aurora and Shapes classes, respectively.
 */
public class Canvas extends NanoleafDevice {
	
	/**
	 * Creates a new instance of the Canvas controller.
	 * 
	 * @param hostname             the hostname of the controller
	 * @param port                 the port of the controller (default=16021)
	 * @param accessToken          a unique authentication token
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public Canvas(String hostname, int port, String accessToken)
			throws NanoleafException, IOException {
		
		super(hostname, port, accessToken);
	}
	
	/**
	 * Creates a new instance of the Canvas controller.
	 * 
	 * @param hostname             the hostname of the controller
	 * @param accessToken          a unique authentication token
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public Canvas(String hostname, String accessToken)
			throws NanoleafException, IOException {
		super(hostname, DEFAULT_PORT, accessToken);
	}
	
	/**
	 * Asynchronously creates a new instance of the Canvas.
	 * 
	 * @param hostname             the hostname of the Canvas
	 * @param port                 the port of the Canvas (default=16021)
	 * @param accessToken          a unique authentication token
	 * @param callback             the callback that is called when the device has
	 *                             been initialized
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public Canvas(String hostname, int port, String accessToken, NanoleafCallback<Canvas> callback) {
		super(hostname, port, accessToken, callback);
	}
	
	/**
	 * Asynchronously creates a new instance of the Canvas
	 * 
	 * @param hostname             the hostname of the Canvas
	 * @param accessToken          a unique authentication token
	 * @param callback             the callback that is called when the device has
	 *                             been initialized
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public Canvas(String hostname, String accessToken, NanoleafCallback<Canvas> callback) {
		super(hostname, DEFAULT_PORT, accessToken, callback);
	}
	
	protected Canvas(String hostname, int port, String accessToken, OkHttpClient client)
			throws NanoleafException, IOException {
		super(hostname, port, accessToken, client);
	}
}
