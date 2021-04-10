package com.github.rowak.nanoleafapi.util;

import java.io.IOException;

import org.json.JSONObject;

import com.github.rowak.nanoleafapi.NanoleafDevice;
import com.github.rowak.nanoleafapi.NanoleafException;

import okhttp3.OkHttpClient;
import okhttp3.Response;

public class Setup {
	/**
	 * Creates a unique authentication token that exists until it is destroyed
	 * using the <code>destroyAccessToken()</code> method.
	 * @param host  the hostname of the controller
	 * @param port  the port of the controller (default=16021)
	 * @param apiLevel  the current version of the OpenAPI (for example: /api/v1/)
	 * @return a unique authentication token
	 * @throws NanoleafException  if the response status code not 2xx
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static String createAccessToken(String host, int port) throws NanoleafException, IOException {
		OkHttpClient client = new OkHttpClient();
		String url = String.format("http://%s:%d/api/%s/new",
				host, port, NanoleafDevice.API_LEVEL);
		Response resp = HttpUtil.postHttpSync(client, url, null);
		int status = resp.code();
		String body = resp.body().string();
		if (status == HttpUtil.OK) {
			JSONObject json = new JSONObject(body);
			return json.getString("auth_token");
		}
		else {
			NanoleafException.checkStatusCode(status);
			return null;
		}
	}
	
	/**
	 * Permanently destroys an authentication token.
	 * @param host  the hostname of the Aurora controller
	 * @param port  the port of the Aurora controller (default=16021)
	 * @param accessToken  a unique authentication token
	 * @throws NanoleafException  if the access token is invalid
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static void destroyAccessToken(String host, int port, String accessToken)
			throws NanoleafException, IOException {
		OkHttpClient client = new OkHttpClient();
		String url = String.format("http://%s:%d/api/%s/%s",
				host, port, NanoleafDevice.API_LEVEL, accessToken);
		Response resp = HttpUtil.deleteHttpSync(client, url);
		int status = resp.code();
		NanoleafException.checkStatusCode(status);
	}
}
