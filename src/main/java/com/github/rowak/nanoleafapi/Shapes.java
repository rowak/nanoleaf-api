package com.github.rowak.nanoleafapi;

import java.io.IOException;

import okhttp3.OkHttpClient;

/**
 * The Shapes class is one of the three main concrete classes for creating a NanoleafDevice.
 * This class should only be used to connect to physical Hexagons, Triangles, or Mini
 * Triangles (Nanoleaf Shapes) devices. For other devices such as the Canvas or Hexagons, use
 * the Aurora and Canvas classes, respectively.
 */
public class Shapes extends NanoleafDevice {

	/**
	 * Creates a new instance of the Shapes controller.
	 * @param hostname  the hostname of the controller
	 * @param port  the port of the controller (default=16021)
	 * @param accessToken  a unique authentication token
	 * @throws NanoleafException  if the access token is invalid
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public Shapes(String hostname, int port, String accessToken)
			throws NanoleafException, IOException {
		
		super(hostname, port, accessToken);
	}
	
	/**
	 * Creates a new instance of the Shapes controller.
	 * @param hostname  the hostname of the controller
	 * @param accessToken  a unique authentication token
	 * @throws NanoleafException  if the access token is invalid
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public Shapes(String hostname, String accessToken)
			throws NanoleafException, IOException {
		super(hostname, DEFAULT_PORT, accessToken);
	}
	
	/**
	 * Asynchronously creates a new instance of the Shapes.
	 * 
	 * @param hostname             the hostname of the Shapes
	 * @param port                 the port of the Shapes (default=16021)
	 * @param accessToken          a unique authentication token
	 * @param callback             the callback that is called when the device has
	 *                             been initialized
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public Shapes(String hostname, int port, String accessToken, NanoleafCallback<Shapes> callback) {
		super(hostname, port, accessToken, callback);
	}
	
	/**
	 * Asynchronously creates a new instance of the Shapes.
	 * 
	 * @param hostname             the hostname of the Shapes
	 * @param accessToken          a unique authentication token
	 * @param callback             the callback that is called when the device has
	 *                             been initialized
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public Shapes(String hostname, String accessToken, NanoleafCallback<Shapes> callback) {
		super(hostname, DEFAULT_PORT, accessToken, callback);
	}
	
	protected Shapes(String hostname, int port, String accessToken, OkHttpClient client)
			throws NanoleafException, IOException {
		super(hostname, port, accessToken, client);
	}
}
