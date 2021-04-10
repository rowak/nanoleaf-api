package com.github.rowak.nanoleafapi;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class Canvas extends NanoleafDevice {
	
	/**
	 * Creates a new instance of the Canvas controller.
	 * @param hostname  the hostname of the controller
	 * @param port  the port of the controller (default=16021)
	 * @param accessToken  a unique authentication token
	 * @throws NanoleafException  if the access token is invalid
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public Canvas(String hostname, int port, String accessToken)
			throws NanoleafException, IOException, ExecutionException, InterruptedException {
		
		super(hostname, port, accessToken);
	}
	
	/**
	 * Creates a new instance of the Canvas controller.
	 * @param hostname  the hostname of the controller
	 * @param accessToken  a unique authentication token
	 * @throws NanoleafException  if the access token is invalid
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public Canvas(String hostname, String accessToken)
			throws NanoleafException, IOException, ExecutionException, InterruptedException {
		super(hostname, DEFAULT_PORT, accessToken);
	}
}
