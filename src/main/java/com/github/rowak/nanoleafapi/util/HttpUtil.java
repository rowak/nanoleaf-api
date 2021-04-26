package com.github.rowak.nanoleafapi.util;

import java.io.IOException;

import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Provides simplified synchronous and asynchronous HTTP methods for use
 * with an existing OkHttpClient.
 * 
 * It is possible to use temporary clients,
 * but this is not recommended. Asynchronous requests may cause the program
 * to hang on normal termination, but according to the OkHttpClient documentation,
 * the threads causing this will eventually terminate. Alternatively, the
 * HTTP client can be forcefully shut down.
 */
public class HttpUtil {
	
	public static final int OK = 200;
	public static final int NO_CONTENT = 204;
	public static final int BAD_REQUEST = 400;
	public static final int UNAUTHORIZED = 401;
	public static final int FORBIDDEN = 403;
	public static final int NOT_FOUND = 404;
	public static final int UNPROCESSABLE_ENTITY = 422;
	public static final int INTERNAL_SERVER_ERROR = 500;
	
	/**
	 * Synchronous HTTP GET request.
	 * @param client         an existing HTTP client
	 * @param url            the request url
	 * @return               an HTTP response
	 * @throws IOException   if an HTTP exception occurs
	 */
	public static Response getHttpSync(OkHttpClient client, String url) throws IOException {
		okhttp3.Request request = new okhttp3.Request.Builder()
				.url(url)
				.get()
				.build();
		okhttp3.Response resp = client.newCall(request).execute();
		return resp;
	}
	
	/**
	 * Synchronous HTTP POST request.
	 * @param client         an existing HTTP client
	 * @param url            the request url
	 * @param data           the data to send
	 * @return               an HTTP response
	 * @throws IOException   if an HTTP exception occurs
	 */
	public static Response postHttpSync(OkHttpClient client, String url, String data) throws IOException {
		okhttp3.Request request = new okhttp3.Request.Builder()
				.url(url)
				.post(RequestBody.create(data != null ? data.getBytes() : new byte[0]))
				.build();
		okhttp3.Response resp = client.newCall(request).execute();
		return resp;
	}
	
	/**
	 * Synchronous HTTP PUT request.
	 * @param client         an existing HTTP client
	 * @param url            the request url
	 * @param data           the data to send
	 * @return               an HTTP response
	 * @throws IOException   if an HTTP exception occurs
	 */
	public static Response putHttpSync(OkHttpClient client, String url, String data) throws IOException {
		okhttp3.Request request = new okhttp3.Request.Builder()
				.url(url)
				.put(RequestBody.create(data != null ? data.getBytes() : new byte[0]))
				.build();
		okhttp3.Response resp = client.newCall(request).execute();
		return resp;
	}
	
	/**
	 * Synchronous HTTP DELETE request.
	 * @param client         an existing HTTP client
	 * @param url            the request url
	 * @return               an HTTP response
	 * @throws IOException   if an HTTP exception occurs
	 */
	public static Response deleteHttpSync(OkHttpClient client, String url) throws IOException {
		okhttp3.Request request = new okhttp3.Request.Builder()
				.url(url)
				.delete()
				.build();
		okhttp3.Response resp = client.newCall(request).execute();
		return resp;
	}
	
	/**
	 * Asynchronous HTTP GET request.
	 * @param client     an existing HTTP client
	 * @param url        the request url
	 * @param callback   a callback for handling responses and errors
	 */
	public static void getHttpAsync(OkHttpClient client, String url, Callback callback) {
		Request req = new Request.Builder()
				.url(url)
				.get()
				.build();
		client.newCall(req).enqueue(callback);
	}
	
	/**
	 * Asynchronous HTTP POST request.
	 * @param client     an existing HTTP client
	 * @param url        the request url
	 * @param data       the data to send
	 * @param callback   a callback for handling responses and errors
	 */
	public static void postHttpAsync(OkHttpClient client, String url, String data, Callback callback) {
		Request req = new Request.Builder()
				.url(url)
				.post(RequestBody.create(data != null ? data.getBytes() : new byte[0]))
				.build();
		client.newCall(req).enqueue(callback);
	}
	
	/**
	 * Asynchronous HTTP PUT request.
	 * @param client     an existing HTTP client
	 * @param url        the request url
	 * @param data       the data to send
	 * @param callback   a callback for handling responses and errors
	 */
	public static void putHttpAsync(OkHttpClient client, String url, String data, Callback callback) {
		Request req = new Request.Builder()
				.url(url)
				.put(RequestBody.create(data != null ? data.getBytes() : new byte[0]))
				.build();
		client.newCall(req).enqueue(callback);
	}
	
	/**
	 * Asynchronous HTTP DELETE request.
	 * @param client     an existing HTTP client
	 * @param url        the request url
	 * @param callback   a callback for handling responses and errors
	 */
	public static void deleteHttpAsync(OkHttpClient client, String url, Callback callback) {
		Request req = new Request.Builder()
				.url(url)
				.delete()
				.build();
		client.newCall(req).enqueue(callback);
	}
}
