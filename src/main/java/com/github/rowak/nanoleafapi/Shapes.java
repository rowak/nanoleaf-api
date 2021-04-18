package com.github.rowak.nanoleafapi;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import com.here.oksse.OkSse;
import com.here.oksse.ServerSentEvent;

import okhttp3.Request;

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
			throws NanoleafException, IOException, ExecutionException, InterruptedException {
		
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
			throws NanoleafException, IOException, ExecutionException, InterruptedException {
		super(hostname, DEFAULT_PORT, accessToken);
	}
}
