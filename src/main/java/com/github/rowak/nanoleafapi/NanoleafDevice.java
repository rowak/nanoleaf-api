package com.github.rowak.nanoleafapi;

import org.json.JSONArray;
import org.json.JSONObject;

import com.github.rowak.nanoleafapi.event.NanoleafEventListener;
import com.github.rowak.nanoleafapi.schedule.Schedule;
import com.github.rowak.nanoleafapi.util.HttpUtil;
import com.here.oksse.OkSse;
import com.here.oksse.ServerSentEvent;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.awt.Point;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * The primary class in the API. Contains methods and other
 * classes for accessing and manipulating a Nanoleaf device.
 */
public abstract class NanoleafDevice {
	
	/** The API level for interfacing with the Open API (the latest version is v1) */
	public static final String API_LEVEL = "v1";
	
	/** The default port used by all Nanoleaf devices */
	public static final int DEFAULT_PORT = 16021;
	
	/** Internal HTTP client for communications with the Nanoleaf device */
	private OkHttpClient client;
	
	/** Internal SSE clients record (for resource cleanup) */
	private List<ServerSentEvent> sse;
	
	private String hostname, accessToken;
	private int port;
	
	/** This information is very unlikely to change, so it is cached */
	private String name;
	private String serialNumber;
	private String manufacturer;
	private String model;
	
	/**
	 * The address of the Nanoleaf device for streaming mode only.
	 */
	protected InetSocketAddress externalAddress;
	
	/**
	 * A generic creation method for instantiating a NanoleafDevice object, without requiring
	 * prior knowledge of the device *type*.
	 * 
	 * @param hostname             the hostname of the controller
	 * @param port                 the port of the controller (default=16021)
	 * @param accessToken          a unique authentication token
	 * @return                     a new Nanoleaf device
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public static NanoleafDevice createDevice(String hostname, int port, String accessToken)
			throws NanoleafException, IOException {
		OkHttpClient client = new OkHttpClient.Builder().readTimeout(0, TimeUnit.SECONDS).build();
		Response resp = HttpUtil.getHttpSync(client, getURL("", hostname, port, accessToken));
		NanoleafException.checkStatusCode(resp.code());
		JSONObject controllerInfo = new JSONObject(resp.body().string());
		if (controllerInfo.has("name")) {
			return createDeviceFromName(controllerInfo.getString("name"), hostname, port, accessToken, null);
		}
		return null;
	}
	
	/**
	 * An asynchronous and generic creation method for instantiating a NanoleafDevice
	 * object, without requiring prior knowledge of the device *type*.
	 * 
	 * @param hostname             the hostname of the controller
	 * @param port                 the port of the controller (default=16021)
	 * @param accessToken          a unique authentication token
	 * @param callback             the callback that is called when the device has
	 *                             been initialized
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public static void createDeviceAsync(String hostname, int port, String accessToken, NanoleafCallback<NanoleafDevice> callback)
			throws NanoleafException, IOException {
		if (callback == null) {
			/* If the callback is not defined, it is impossible for the HTTP client to be
			   closed, which will cause the application to hang */
			return;
		}
		final OkHttpClient client = new OkHttpClient.Builder().readTimeout(0, TimeUnit.SECONDS).build();
		HttpUtil.getHttpAsync(client, getURL("", hostname, port, accessToken), new Callback() {
			@Override
			public void onFailure(Call call, IOException e) {
				if (callback != null) {
					callback.onCompleted(NanoleafCallback.FAILURE, null, null);
				}
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException {
				if (callback != null) {
					int code = response.code();
					if (code != 200 && code != 204) {
						callback.onCompleted(NanoleafCallback.FAILURE, null, null);
					}
					JSONObject controllerInfo = new JSONObject(response.body().string());
					String name = controllerInfo.getString("name");
					NanoleafDevice newDevice = null;
					try {
						newDevice = createDeviceFromName(name, hostname, port, accessToken, client);
						callback.onCompleted(NanoleafCallback.SUCCESS, newDevice, null);
					}
					catch (Exception e) {
						e.printStackTrace();
						callback.onCompleted(NanoleafCallback.FAILURE, null, null);
					}
				}
			}
		});
	}
	
	/* Attempts to create the appropriate device type using the Nanoleaf device's internal name
	   and supplies a default HTTP client. A null client will create a new client */
	private static NanoleafDevice createDeviceFromName(String name,
			String hostname, int port, String accessToken, OkHttpClient client)
					throws NanoleafException, IOException {
		name = name.toLowerCase();
		if (name.contains("aurora") || name.contains("light panels")) {
			return new Aurora(hostname, port, accessToken, client);
		}
		else if (name.contains("canvas")) {
			return new Canvas(hostname, port, accessToken, client);
		}
		else if (name.contains("shapes")) {
			return new Shapes(hostname, port, accessToken, client);
		}
		return null;
	}
	
	// Generic constructor (with port)
	protected NanoleafDevice(String hostname, int port, String accessToken)
			throws NanoleafException, IOException {
		init(hostname, port, accessToken, null);
	}
	
	// Generic async constructor (with port)
	protected NanoleafDevice(String hostname, int port, String accessToken, NanoleafCallback<? extends NanoleafDevice> callback) {
		initAsync(hostname, port, accessToken, callback);
	}
	
	// Generic constructor (no port)
	protected NanoleafDevice(String hostname, String accessToken)
			throws NanoleafException, IOException {
		init(hostname, DEFAULT_PORT, accessToken, null);
	}
	
	// Generic async constructor (no port)
	protected NanoleafDevice(String hostname, String accessToken, NanoleafCallback<? extends NanoleafDevice> callback) {
		initAsync(hostname, DEFAULT_PORT, accessToken, callback);
	}
	
	// Generic constructor (with optional client)
	protected NanoleafDevice(String hostname, int port, String accessToken, OkHttpClient client)
			throws NanoleafException, IOException {
		init(hostname, port, accessToken, client);
	}
	
	// Initializes the device and caches basic info
	private void init(String hostname, int port, String accessToken, OkHttpClient client)
			throws NanoleafException, IOException {
		this.hostname = hostname;
		this.port = port;
		this.accessToken = accessToken;
		
		if (client == null) {
			this.client = new OkHttpClient.Builder().readTimeout(0, TimeUnit.SECONDS).build();
		}
		else {
			this.client = client;
		}
		String body = get(getURL(""));
		JSONObject controllerInfo = new JSONObject(body);
		this.name = controllerInfo.getString("name");
		this.serialNumber = controllerInfo.getString("serialNo");
		this.manufacturer = controllerInfo.getString("manufacturer");
		this.model = controllerInfo.getString("model");
		
		sse = new ArrayList<ServerSentEvent>();
	}
	
	// Asynchronously initializes the device and caches basic info
	private void initAsync(String hostname, int port, String accessToken, NanoleafCallback<? extends NanoleafDevice> callback) {
		client = new OkHttpClient.Builder().readTimeout(0, TimeUnit.SECONDS).build();
		sse = new ArrayList<ServerSentEvent>();
		getAsync(getURL(""), (status, data, device) -> {
			JSONObject controllerInfo = new JSONObject(data);
			this.name = controllerInfo.getString("name");
			this.serialNumber = controllerInfo.getString("serialNo");
			this.manufacturer = controllerInfo.getString("manufacturer");
			this.model = controllerInfo.getString("model");
		});
	}
	
	/**
	 *<p>Force a shutdown of the internal HTTP client.</p>
	 * 
	 * <p><b>Note:</b> This method only has effect if asynchronous calls have
	 * been made. The HTTP client will eventually shut down on its own, but
	 * the application will likely hang until it does (unless this method
	 * is called).</p>
	 */
	public void closeAsync() {
		client.dispatcher().executorService().shutdown();
	}
	
	/**
	 * Closes all event listeners that have been opened with
	 * {@link NanoleafDevice#registerTouchEventListener}.
	 */
	public void closeEventListener() {
		for (ServerSentEvent s : sse) {
			s.close();
		}
	}
	
	/**
	 * Returns the device's hostname.
	 * 
	 * @return  the hostname for this device
	 */
	public String getHostname() {
		return this.hostname;
	}
	
	/**
	 * Returns the port that the device is listening on.
	 * 
	 * @return  the port for this device
	 */
	public int getPort() {
		return this.port;
	}
	
	/**
	 * Returns the access token that was used to initialize the device object.
	 * 
	 * @return  the access token for this device
	 */
	public String getAccessToken() {
		return this.accessToken;
	}
	
	/**
	 * <p>Returns the unique name of the device controller.</p>
	 * 
	 * <p>Note that this method does not communicate with the device, since
	 * the name is retrieved at creation time and is almost always unwritable.</p>
	 * 
	 * @return  the name of the device controller
	 */
	public String getName() {
		return this.name;
	}
	
	/**
	 * <p>Returns the unique serial number of the device.</p>
	 * 
	 * <p>Note that this method does not communicate with the device, since
	 * the serial number is retrieved at creation time and is almost always
	 * unwritable.</p>
	 * 
	 * @return  the serial number of the device
	 */
	public String getSerialNumber() {
		return this.serialNumber;
	}
	
	/**
	 * <p>Returns the name of the manufacturer of the device.</p>
	 * 
	 * <p>Note that this method does not communicate with the device, since
	 * the manufacturer name is retrieved at creation time and is almost
	 * always unwritable.</p>
	 * 
	 * @return  the name of the device manufacturer
	 */
	public String getManufacturer() {
		return this.manufacturer;
	}
	
	/**
	 * Returns the firmware version of the device.
	 * 
	 * @return  the firmware version
	 */
	public String getFirmwareVersion()
			throws NanoleafException, IOException {
		String body = get(getURL(""));
		JSONObject controllerInfo = new JSONObject(body);
		return controllerInfo.getString("firmwareVersion");
	}
	
	/**
	 * <p>Asynchronously gets the firmware version of the device.</p>
	 * 
	 * <p>The callback status will return {@link NanoleafCallback.SUCCESS} on success,
	 * or {@link NanoleafCallback.UNAUTHORIZED} if the access token is invalid. If an
	 * internal API error occurs, it will instead return
	 * {@link NanoleafCallback.FAILURE}.</p>
	 * 
	 * @param callback   returns the firmware version
	 */
	public void getFirmwareVersionAsync(NanoleafCallback<String> callback) {
		getAsync(getURL(""), (status, data, device) -> {
			if (status != NanoleafCallback.SUCCESS) {
				callback.onCompleted(NanoleafCallback.FAILURE, null, device);
			}
			JSONObject controllerInfo = new JSONObject(data);
			if (!controllerInfo.has("firmwareVersion")) {
				callback.onCompleted(NanoleafCallback.FAILURE, null, device);
			}
			String firmwareVersion = controllerInfo.getString("firmwareVersion");
			callback.onCompleted(NanoleafCallback.SUCCESS, firmwareVersion, device);
		});
	}
	
	/**
	 * <p>Returns the model of the device.</p>
	 * 
	 * <p>Note that this method does not communicate with the device, since
	 * the model number is retrieved at creation time and is almost always
	 * unwritable.</p>
	 * 
	 * @return  the model of the device
	 */
	public String getModel() {
		return this.model;
	}
	
	/**
	 * Causes the panels to flash in unison. This is typically used to help
	 * users differentiate between multiple panels.
	 * 
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public void identify()
			throws NanoleafException, IOException {
		put(getURL("identify"), null);
	}
	
	/**
	 * <p>Asynchronously causes the panels to flash in unison. This is typically
	 * used to help users differentiate between multiple panels.</p>
	 *
	 * <p>The callback status will return {@link NanoleafCallback.SUCCESS} on success,
	 * or {@link NanoleafCallback.UNAUTHORIZED} if the access token is invalid. If an
	 * internal API error occurs, it will instead return
	 * {@link NanoleafCallback.FAILURE}.</p>
	 * 
	 * @param callback   called when the identify animation begins or when an error occurs
	 */
	public void identifyAsync(NanoleafCallback<String> callback) {
		putAsync(getURL("identify"), null, callback);
	}
	
	/**
	 * Gets the on state of the device (true = on, false = off).
	 * 
	 * @return                     true, if the device is on
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public boolean getOn()
			throws NanoleafException, IOException {
		return Boolean.parseBoolean(get(getURL("state/on/value")));
	}
	
	/**
	 * <p>Asynchronously gets the on state of the device (true = on, false = off).</p>
	 * 
	 * <p>The callback status will return {@link NanoleafCallback.SUCCESS} on success,
	 * or {@link NanoleafCallback.UNAUTHORIZED} if the access token is invalid. If an
	 * internal API error occurs, it will instead return
	 * {@link NanoleafCallback.FAILURE}.</p>
	 * 
	 * @param callback   returns the on state
	 */
	public void getOnAsync(NanoleafCallback<String> callback) {
		getAsync(getURL("state/on/value"), callback);
	}
	
	/**
	 * Sets the on state of the device (true = on, false = off).
	 * 
	 * @param on                   whether the device should be turned on or off
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public void setOn(boolean on)
			throws NanoleafException, IOException {
		String body = String.format("{\"on\": {\"value\": %b}}", on);
		put(getURL("state"), body);
	}
	
	/**
	 * <p>Asynchronously sets the on state of the device (true = on, false = off).</p>
	 * 
	 * <p>The callback status will return {@link NanoleafCallback.SUCCESS} on success,
	 * or {@link NanoleafCallback.UNAUTHORIZED} if the access token is invalid. If an
	 * internal API error occurs, it will instead return
	 * {@link NanoleafCallback.FAILURE}.</p>
	 * 
	 * @param on         whether the device should be turned on or off
	 * @param callback   called when the device changes power state or when an error occurs
	 */
	public void setOnAsync(boolean on, NanoleafCallback<String> callback) {
		String body = String.format("{\"on\": {\"value\": %b}}", on);
		putAsync(getURL("state"), body, callback);
	}
	
	/**
	 * Toggles the on state of the device (on = off, off = on).
	 * 
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public void toggleOn()
			throws NanoleafException, IOException {
		setOn(!this.getOn());
	}
	
	/**
	 * <p>Toggles the on state of the device (on = off, off = on).</p>
	 * 
	 * <p>The callback status will return {@link NanoleafCallback.SUCCESS} on success,
	 * or {@link NanoleafCallback.UNAUTHORIZED} if the access token is invalid. If an
	 * internal API error occurs, it will instead return
	 * {@link NanoleafCallback.FAILURE}.</p>
	 * 
	 * @param callback             called when the the device changes
	 *                             power state or if error occurs
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public void toggleOnAsync(NanoleafCallback<String> callback)
			throws NanoleafException, IOException {
		setOnAsync(!this.getOn(), callback);
	}
	
	/**
	 * Gets the master brightness of the device.
	 * 
	 * @return                     the brightness of the device
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public int getBrightness()
			throws NanoleafException, IOException {
		return Integer.parseInt(get(getURL("state/brightness/value")));
	}
	
	/**
	 * <p>Asynchronously gets the master brightness of the device.</p>
	 * 
	 * <p>The callback status will return {@link NanoleafCallback.SUCCESS} on success,
	 * or {@link NanoleafCallback.UNAUTHORIZED} if the access token is invalid. If an
	 * internal API error occurs, it will instead return
	 * {@link NanoleafCallback.FAILURE}.</p>
	 * 
	 * @param callback   returns the brightness of the device
	 */
	public void getBrightnessAsync(NanoleafCallback<Integer> callback) {
		getAsyncInt(getURL("state/brightness/value"), callback);
	}
	
	/**
	 * Sets the master brightness of the device.
	 * 
	 * @param brightness           the new brightness level as a percent
	 * @throws NanoleafException   If the access token is invalid, or the specified
	 *                             brightness is not between 0 and 100 (inclusive)
	 * @throws IOException         If an HTTP exception occurs
	 */
	public void setBrightness(int brightness)
			throws NanoleafException, IOException {
		String body = String.format("{\"brightness\": {\"value\": %d}}", brightness);
		put(getURL("state"), body);
	}
	
	/**
	 * <p>Asynchronously sets the master brightness of the device.</p>
	 * 
	 * <p>The callback status will return {@link NanoleafCallback.SUCCESS} on success,
	 * or {@link NanoleafCallback.UNAUTHORIZED} if the access token is invalid. If an
	 * internal API error occurs, it will instead return
	 * {@link NanoleafCallback.FAILURE}.</p>
	 * 
	 * @param brightness   the new brightness level as a percent
	 * @param callback     called when the brightness is changed or when an error occurs
	 */
	public void setBrightnessAsync(int brightness, NanoleafCallback<String> callback) {
		String body = String.format("{\"brightness\": {\"value\": %d}}", brightness);
		putAsync(getURL("state"), body, callback);
	}
	
	/**
	 * <p>Fades the master brightness of the device over a period of time.</p>
	 * 
	 * <p><b>Note:</b> the fade itself is not synchronous, only the request being made.
	 * This means that the calling thread will not hang during the fade.</p>
	 * 
	 * @param brightness           the new brightness level as a percent
	 * @param duration             the fade time, in seconds
	 * @throws NanoleafException   If the access token is invalid, or the specified
	 *                             brightness is not between 0 and 100 (inclusive)
	 * @throws IOException         If an HTTP exception occurs
	 */
	public void fadeToBrightness(int brightness, int duration)
			throws NanoleafException, IOException {
		String body = String.format("{\"brightness\": {\"value\": %d, \"duration\": %d}}",
				brightness, duration);
		put(getURL("state"), body);
	}
	
	/**
	 * <p>Asynchronously fades the master brightness of the device over a period of time.</p>
	 * 
	 * <p>The callback status will return {@link NanoleafCallback.SUCCESS} on success,
	 * or {@link NanoleafCallback.UNAUTHORIZED} if the access token is invalid. If an
	 * internal API error occurs, it will instead return
	 * {@link NanoleafCallback.FAILURE}.</p>
	 * 
	 * @param brightness   the new brightness level as a percent
	 * @param duration     the fade time, in seconds
	 * @param callback     called when the fade begins or when an error occurs
	 */
	public void fadeToBrightnessAsync(int brightness, int duration, NanoleafCallback<String> callback) {
		String body = String.format("{\"brightness\": {\"value\": %d, \"duration\": %d}}",
				brightness, duration);
		putAsync(getURL("state"), body, callback);
	}
	
	/**
	 * Increases the brightness by an amount as a percent.
	 * 
	 * @param amount               the amount to increase by
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public void increaseBrightness(int amount)
			throws NanoleafException, IOException {
		String body = String.format("{\"brightness\": {\"increment\": %d}}", amount);
		put(getURL("state"), body);
	}
	
	/**
	 * <p>Asynchronously increases the brightness by an amount as a percent.</p>
	 * 
	 * <p>The callback status will return {@link NanoleafCallback.SUCCESS} on success,
	 * or {@link NanoleafCallback.UNAUTHORIZED} if the access token is invalid. If an
	 * internal API error occurs, it will instead return
	 * {@link NanoleafCallback.FAILURE}.</p>
	 * 
	 * @param amount     the amount to increase by
	 * @param callback   called when the brightness changes or when an error occurs
	 */
	public void increaseBrightnessAsync(int amount, NanoleafCallback<String> callback) {
		String body = String.format("{\"brightness\": {\"increment\": %d}}", amount);
		putAsync(getURL("state"), body, callback);
	}
	
	/**
	 * Decreases the brightness by an amount as a percent.
	 * 
	 * @param amount               the amount to decrease by
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public void decreaseBrightness(int amount)
			throws NanoleafException, IOException {
		increaseBrightness(-amount);
	}
	
	/**
	 * <p>Asynchronously decreases the brightness by an amount as a percent.</p>
	 * 
	 * <p>The callback status will return {@link NanoleafCallback.SUCCESS} on success,
	 * or {@link NanoleafCallback.UNAUTHORIZED} if the access token is invalid. If an
	 * internal API error occurs, it will instead return
	 * {@link NanoleafCallback.FAILURE}.</p>
	 * 
	 * @param amount     the amount to decrease by
	 * @param callback   called when the brightness changes or when an error occurs
	 */
	public void decreaseBrightnessAsync(int amount, NanoleafCallback<String> callback) {
		increaseBrightnessAsync(-amount, callback);
	}
	
	/**
	 * Gets the maximum brightness of the device.
	 * 
	 * @return                     the maximum brightness
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public int getMaxBrightness()
			throws NanoleafException, IOException {
		return Integer.parseInt(get(getURL("state/brightness/max")));
	}
	
	/**
	 * <p>Asynchronously gets the maximum brightness of the device.</p>
	 * 
	 * <p>The callback status will return {@link NanoleafCallback.SUCCESS} on success,
	 * or {@link NanoleafCallback.UNAUTHORIZED} if the access token is invalid. If an
	 * internal API error occurs, it will instead return
	 * {@link NanoleafCallback.FAILURE}.</p>
	 * 
	 * @param callback   returns the maximum brightness
	 */
	public void getMaxBrightnessAsync(NanoleafCallback<Integer> callback) {
		getAsyncInt(getURL("state/brightness/max"), callback);
	}
	
	/**
	 * Gets the minimum brightness of the device.
	 * 
	 * @return                     the minimum brightness
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public int getMinBrightness()
			throws NanoleafException, IOException {
		return Integer.parseInt(get(getURL("state/brightness/min")));
	}
	
	/**
	 * <p>Asynchronously gets the minimum brightness of the device.</p>
	 * 
	 * <p>The callback status will return {@link NanoleafCallback.SUCCESS} on success,
	 * or {@link NanoleafCallback.UNAUTHORIZED} if the access token is invalid. If an
	 * internal API error occurs, it will instead return
	 * {@link NanoleafCallback.FAILURE}.</p>
	 * 
	 * @param callback   returns the minimum brightness
	 */
	public void getMinBrightnessAsync(NanoleafCallback<Integer> callback) {
		getAsyncInt(getURL("state/brightness/min"), callback);
	}
	
	/**
	 * Gets the hue of the device (static/custom effects only).
	 * 
	 * @return                     the hue of the device
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public int getHue()
			throws NanoleafException, IOException {
		return Integer.parseInt(get(getURL("state/hue/value")));
	}
	
	/**
	 * <p>Asynchronously gets the hue of the device (static/custom effects only).</p>
	 * 
	 * <p>The callback status will return {@link NanoleafCallback.SUCCESS} on success,
	 * or {@link NanoleafCallback.UNAUTHORIZED} if the access token is invalid. If an
	 * internal API error occurs, it will instead return
	 * {@link NanoleafCallback.FAILURE}.</p>
	 * 
	 * @param callback   returns the hue
	 */
	public void getHueAsync(NanoleafCallback<Integer> callback) {
		getAsyncInt(getURL("state/hue/value"), callback);
	}
	
	/**
	 * Sets the hue of the device (static/custom effects only).
	 * 
	 * @param hue                  the new hue
	 * @throws NanoleafException   If the access token is invalid, or the specified
	 *                             hue is not between 0 and 360 (inclusive).
	 * @throws IOException         If an HTTP exception occurs
	 */
	public void setHue(int hue)
			throws NanoleafException, IOException {
		String body = String.format("{\"hue\": {\"value\": %d}}", hue);
		put(getURL("state"), body);
	}
	
	/**
	 * Increases the hue by a set amount.
	 * 
	 * @param amount               the amount to increase by
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public void increaseHue(int amount)
			throws NanoleafException, IOException {
		String body = String.format("{\"hue\": {\"increment\": %d}}", amount);
		put(getURL("state"), body);
	}
	
	/**
	 * <p>Asynchronously increases the hue by a set amount.</p>
	 * 
	 * <p>The callback status will return {@link NanoleafCallback.SUCCESS} on success,
	 * or {@link NanoleafCallback.UNAUTHORIZED} if the access token is invalid. If an
	 * internal API error occurs, it will instead return
	 * {@link NanoleafCallback.FAILURE}. The returned data will never be meaningful
	 * (either an empty string or null).</p>
	 * 
	 * @param amount     the amount to increase by
	 * @param callback   called when the hue changes or when an error occurs
	 */
	public void increaseHueAsync(int amount, NanoleafCallback<String> callback) {
		String body = String.format("{\"hue\": {\"increment\": %d}}", amount);
		putAsync(getURL("state"), body, callback);
	}
	
	/**
	 * Decreases the hue by a set amount.
	 * 
	 * @param amount               the amount to decrease by
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public void decreaseHue(int amount)
			throws NanoleafException, IOException {
		increaseHue(-amount);
	}
	
	/**
	 * <p>Asynchronously decreases the hue by a set amount.</p>
	 * 
	 * <p>The callback status will return {@link NanoleafCallback.SUCCESS} on success,
	 * or {@link NanoleafCallback.UNAUTHORIZED} if the access token is invalid. If an
	 * internal API error occurs, it will instead return
	 * {@link NanoleafCallback.FAILURE}. The returned data will never be meaningful
	 * (either an empty string or null).</p>
	 * 
	 * @param amount     the amount to decrease by
	 * @param callback   called when the brightness changes or when an error occurs
	 */
	public void decreaseHueAsync(int amount, NanoleafCallback<String> callback) {
		increaseHueAsync(-amount, callback);
	}
	
	/**
	 * Gets the maximum hue of the device.
	 * 
	 * @return                     the maximum hue
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public int getMaxHue()
			throws NanoleafException, IOException {
		return Integer.parseInt(get(getURL("state/hue/max")));
	}
	
	/**
	 * <p>Asynchronously gets the maximum hue of the device.</p>
	 * 
	 * <p>The callback status will return {@link NanoleafCallback.SUCCESS} on success,
	 * or {@link NanoleafCallback.UNAUTHORIZED} if the access token is invalid. If an
	 * internal API error occurs, it will instead return
	 * {@link NanoleafCallback.FAILURE}. The returned data will never be meaningful
	 * (either an empty string or null).</p>
	 * 
	 * @param callback   returns the maximum hue
	 */
	public void getMaxHueAsync(NanoleafCallback<Integer> callback) {
		getAsyncInt(getURL("state/hue/max"), callback);
	}
	
	/**
	 * Gets the minimum hue of the device.
	 * 
	 * @return                     the minimum hue
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public int getMinHue()
			throws NanoleafException, IOException {
		return Integer.parseInt(get(getURL("state/hue/min")));
	}
	
	/**
	 * <p>Asynchronously gets the minimum hue of the device.</p>
	 * 
	 * <p>The callback status will return {@link NanoleafCallback.SUCCESS} on success,
	 * or {@link NanoleafCallback.UNAUTHORIZED} if the access token is invalid. If an
	 * internal API error occurs, it will instead return
	 * {@link NanoleafCallback.FAILURE}. The returned data will never be meaningful
	 * (either an empty string or null).</p>
	 * 
	 * @param callback   returns the minimum hue
	 */
	public void getMinHueAsync(NanoleafCallback<Integer> callback) {
		getAsyncInt(getURL("state/hue/min"), callback);
	}
	
	/**
	 * Gets the saturation of the device (static/custom effects only).
	 * 
	 * @return                     the saturation of the device
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public int getSaturation()
			throws NanoleafException, IOException {
		return Integer.parseInt(get(getURL("state/sat/value")));
	}
	
	/**
	 * <p>Asynchronously gets the saturation of the device (static/custom effects only).</p>
	 * 
	 * <p>The callback status will return {@link NanoleafCallback.SUCCESS} on success,
	 * or {@link NanoleafCallback.UNAUTHORIZED} if the access token is invalid. If an
	 * internal API error occurs, it will instead return
	 * {@link NanoleafCallback.FAILURE}. The returned data will never be meaningful
	 * (either an empty string or null).</p>
	 * 
	 * @param callback   returns the saturation
	 */
	public void getSaturationAsync(NanoleafCallback<Integer> callback) {
		getAsyncInt(getURL("state/sat/value"), callback);
	}
	
	/**
	 * Sets the saturation of the device (static/custom effects only).
	 * 
	 * @param saturation           the new saturation
	 * @throws NanoleafException   If the access token is invalid, or the specified
	 *                             saturation is not between 0 and 100 (inclusive).
	 * @throws IOException         If an HTTP exception occurs
	 */
	public void setSaturation(int saturation)
			throws NanoleafException, IOException {
		String body = String.format("{\"sat\": {\"value\": %d}}", saturation);
		put(getURL("state"), body);
	}
	
	/**
	 * <p>Asynchronously sets the saturation of the device (static/custom effects only).</p>
	 * 
	 * <p>The callback status will return {@link NanoleafCallback.SUCCESS} on success,
	 * or {@link NanoleafCallback.UNAUTHORIZED} if the access token is invalid. If an
	 * internal API error occurs, it will instead return
	 * {@link NanoleafCallback.FAILURE}. The returned data will never be meaningful
	 * (either an empty string or null).</p>
	 * 
	 * @param saturation   the new saturation
	 * @param callback     called when the saturation is set or when an error occurs
	 */
	public void setSaturationAsync(int saturation, NanoleafCallback<String> callback) {
		String body = String.format("{\"sat\": {\"value\": %d}}", saturation);
		putAsync(getURL("state/hue/min"), body, callback);
	}
	
	/**
	 * Increases the saturation by a set amount.
	 * 
	 * @param amount               the amount to increase by
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public void increaseSaturation(int amount)
			throws NanoleafException, IOException {
		String body = String.format("{\"sat\": {\"increment\": %d}}", amount);
		put(getURL("state"), body);
	}
	
	/**
	 * <p>Asynchronously increases the saturation by a set amount.</p>
	 * 
	 * <p>The callback status will return {@link NanoleafCallback.SUCCESS} on success,
	 * or {@link NanoleafCallback.UNAUTHORIZED} if the access token is invalid. If an
	 * internal API error occurs, it will instead return
	 * {@link NanoleafCallback.FAILURE}. The returned data will never be meaningful
	 * (either an empty string or null).</p>
	 * 
	 * @param amount     the amount to increase by
	 * @param callback   called when the saturation changes or when an error occurs
	 */
	public void increaseSaturationAsync(int amount, NanoleafCallback<String> callback) {
		String body = String.format("{\"sat\": {\"increment\": %d}}", amount);
		putAsync(getURL("state"), body, callback);
	}
	
	/**
	 * Decreases the saturation by a set amount.
	 * 
	 * @param amount               the amount to decrease by
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public void decreaseSaturation(int amount)
			throws NanoleafException, IOException {
		increaseSaturation(-amount);
	}
	
	/**
	 * <p>Asynchronously decreases the saturation by a set amount.</p>
	 * 
	 * <p>The callback status will return {@link NanoleafCallback.SUCCESS} on success,
	 * or {@link NanoleafCallback.UNAUTHORIZED} if the access token is invalid. If an
	 * internal API error occurs, it will instead return
	 * {@link NanoleafCallback.FAILURE}. The returned data will never be meaningful
	 * (either an empty string or null).</p>
	 * 
	 * @param amount     the amount to decrease by
	 * @param callback   called when the saturation changes or when an error occurs
	 */
	public void decreaseSaturationAsync(int amount, NanoleafCallback<String> callback) {
		increaseSaturationAsync(-amount, callback);
	}
	
	/**
	 * Gets the maximum saturation of the device.
	 * 
	 * @return                     the maximum saturation
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public int getMaxSaturation()
			throws NanoleafException, IOException {
		return Integer.parseInt(get(getURL("state/sat/max")));
	}
	
	/**
	 * <p>Asynchronously gets the maximum saturation of the device.</p>
	 * 
	 * <p>The callback status will return {@link NanoleafCallback.SUCCESS} on success,
	 * or {@link NanoleafCallback.UNAUTHORIZED} if the access token is invalid. If an
	 * internal API error occurs, it will instead return
	 * {@link NanoleafCallback.FAILURE}. The returned data will never be meaningful
	 * (either an empty string or null).</p>
	 * 
	 * @param callback   returns the maximum saturation
	 */
	public void getMaxSaturationAsync(NanoleafCallback<Integer> callback) {
		getAsyncInt(getURL("state/sat/max"), callback);
	}
	
	/**
	 * Gets the minimum saturation of the device.
	 * 
	 * @return                     the minimum saturation
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public int getMinSaturation()
			throws NanoleafException, IOException {
		return Integer.parseInt(get(getURL("state/sat/min")));
	}
	
	/**
	 * <p>Asynchronously gets the minimum saturation of the device.</p>
	 * 
	 * <p>The callback status will return {@link NanoleafCallback.SUCCESS} on success,
	 * or {@link NanoleafCallback.UNAUTHORIZED} if the access token is invalid. If an
	 * internal API error occurs, it will instead return
	 * {@link NanoleafCallback.FAILURE}. The returned data will never be meaningful
	 * (either an empty string or null).</p>
	 * 
	 * @param callback   returns the minimum saturation
	 */
	public void getMinSaturationAsync(NanoleafCallback<Integer> callback) {
		getAsyncInt(getURL("state/sat/min"), callback);
	}
	
	/**
	 * Gets the color temperature of the device (color temperature effect only).
	 * 
	 * @return                     the color temperature of the device
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public int getColorTemperature()
			throws NanoleafException, IOException {
		return Integer.parseInt(get(getURL("state/ct/value")));
	}
	
	/**
	 * <p>Asynchronously gets the color temperature of the device (color temperature
	 * effect only).</p>
	 * 
	 * <p>The callback status will return {@link NanoleafCallback.SUCCESS} on success,
	 * or {@link NanoleafCallback.UNAUTHORIZED} if the access token is invalid. If an
	 * internal API error occurs, it will instead return
	 * {@link NanoleafCallback.FAILURE}. The returned data will never be meaningful
	 * (either an empty string or null).</p>
	 * 
	 * @param callback   returns the color temperature
	 */
	public void getColorTemperatureAsync(NanoleafCallback<Integer> callback) {
		getAsyncInt(getURL("state/ct/value"), callback);
	}
	
	/**
	 * Sets the color temperature of the device in Kelvin.
	 * 
	 * @param colorTemperature     color temperature in Kelvin
	 * @throws NanoleafException   If the access token is invalid, or the specified
	 *                             color temperature is not between 1200 and 6500
	 *                             (inclusive)
	 * @throws IOException         If an HTTP exception occurs
	 */
	public void setColorTemperature(int colorTemperature)
			throws NanoleafException, IOException {
		String body = String.format("{\"ct\": {\"value\": %d}}", colorTemperature);
		put(getURL("state"), body);
	}
	
	/**
	 * <p>Asynchronously sets the color temperature of the device in Kelvin.</p>
	 * 
	 * <p>The callback status will return {@link NanoleafCallback.SUCCESS} on success,
	 * or {@link NanoleafCallback.UNAUTHORIZED} if the access token is invalid. If an
	 * internal API error occurs, it will instead return
	 * {@link NanoleafCallback.FAILURE}. The returned data will never be meaningful
	 * (either an empty string or null).</p>
	 * 
	 * @param colorTemperature   color temperature in Kelvin
	 * @param callback           called when the color temperature changes or when
	 *                           an error occurs
	 */
	public void setColorTemperatureAsync(int colorTemperature, NanoleafCallback<String> callback) {
		String body = String.format("{\"ct\": {\"value\": %d}}", colorTemperature);
		putAsync(getURL("state"), body, callback);
	}
	
	/**
	 * Increases the color temperature by a set amount.
	 * 
	 * @param amount               the amount to increase by
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public void increaseColorTemperature(int amount)
			throws NanoleafException, IOException {
		String body = String.format("{\"ct\": {\"increment\": %d}}", amount);
		put(getURL("state"), body);
	}
	
	/**
	 * <p>Asynchronously increases the color temperature by a set amount.</p>
	 * 
	 * <p>The callback status will return {@link NanoleafCallback.SUCCESS} on success,
	 * or {@link NanoleafCallback.UNAUTHORIZED} if the access token is invalid. If an
	 * internal API error occurs, it will instead return
	 * {@link NanoleafCallback.FAILURE}. The returned data will never be meaningful
	 * (either an empty string or null).</p>
	 * 
	 * @param amount     the amount to increase by
	 * @param callback   called when the color temperature changes or when
	 *                   an error occurs
	 */
	public void increaseColorTemperatureAsync(int amount, NanoleafCallback<String> callback) {
		String body = String.format("{\"ct\": {\"increment\": %d}}", amount);
		putAsync(getURL("state"), body, callback);
	}
	
	/**
	 * Decreases the color temperature by a set amount.
	 * 
	 * @param amount               the amount to decrease by
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public void decreaseColorTemperature(int amount)
			throws NanoleafException, IOException {
		increaseColorTemperature(-amount);
	}
	
	/**
	 * <p>Asynchronously decreases the color temperature by a set amount.</p>
	 * 
	 * <p>The callback status will return {@link NanoleafCallback.SUCCESS} on success,
	 * or {@link NanoleafCallback.UNAUTHORIZED} if the access token is invalid. If an
	 * internal API error occurs, it will instead return
	 * {@link NanoleafCallback.FAILURE}. The returned data will never be meaningful
	 * (either an empty string or null).</p>
	 * 
	 * @param amount     the amount to decrease by
	 * @param callback   called when the color temperature changes or when
	 *                   an error occurs
	 */
	public void decreaseColorTemperatureAsync(int amount, NanoleafCallback<String> callback) {
		increaseColorTemperatureAsync(-amount, callback);
	}
	
	/**
	 * Gets the maximum color temperature of the device.
	 * 
	 * @return                     the maximum color temperature
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public int getMaxColorTemperature()
			throws NanoleafException, IOException {
		return Integer.parseInt(get(getURL("state/ct/max")));
	}
	
	/**
	 * <p>Asynchronously gets the maximum color temperature of the device.</p>
	 * 
	 * <p>The callback status will return {@link NanoleafCallback.SUCCESS} on success,
	 * or {@link NanoleafCallback.UNAUTHORIZED} if the access token is invalid. If an
	 * internal API error occurs, it will instead return
	 * {@link NanoleafCallback.FAILURE}. The returned data will never be meaningful
	 * (either an empty string or null).</p>
	 * 
	 * @param callback   returns the maximum color temperature
	 */
	public void getMaxColorTemperatureAsync(NanoleafCallback<Integer> callback) {
		getAsyncInt(getURL("state/ct/max"), callback);
	}
	
	/**
	 * Gets the minimum color temperature of the device.
	 * 
	 * @return                     the minimum color temperature
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public int getMinColorTemperature()
			throws NanoleafException, IOException {
		return Integer.parseInt(get(getURL("state/ct/min")));
	}
	
	/**
	 * <p>Asynchronously gets the minimum color temperature of the device.</p>
	 * 
	 * <p>The callback status will return {@link NanoleafCallback.SUCCESS} on success,
	 * or {@link NanoleafCallback.UNAUTHORIZED} if the access token is invalid. If an
	 * internal API error occurs, it will instead return
	 * {@link NanoleafCallback.FAILURE}. The returned data will never be meaningful
	 * (either an empty string or null).</p>
	 * 
	 * @param callback   returns the minimum color temperature
	 */
	public void getMinColorTemperature(NanoleafCallback<Integer> callback) {
		getAsyncInt(getURL("state/ct/min"), callback);
	}
	
	/**
	 * Gets the color mode of the device.
	 * 
	 * @return                     the color mode
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public String getColorMode()
			throws NanoleafException, IOException {
		return get(getURL("state/colorMode")).replace("\"", "");
	}
	
	/**
	 * <p>Asynchronously gets the color mode of the device.</p>
	 * 
	 * <p>The callback status will return {@link NanoleafCallback.SUCCESS} on success,
	 * or {@link NanoleafCallback.UNAUTHORIZED} if the access token is invalid. If an
	 * internal API error occurs, it will instead return
	 * {@link NanoleafCallback.FAILURE}. The returned data will never be meaningful
	 * (either an empty string or null).</p>
	 * 
	 * @param callback   returns the color mode
	 */
	public void getColorModeAsync(NanoleafCallback<String> callback) {
		getAsync(getURL("state/colorMode"), (status, data, device) -> {
			callback.onCompleted(status, data.replace("\"", ""), device);
		});
	}
	
	/**
	 * <p>Gets the current color (HSB/RGB) of the device.</p>
	 * 
	 * <p><b>Note:</b> This only works if the device is displaying a solid color.</p>
	 * 
	 * @return                     the color of the device
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public Color getColor()
			throws NanoleafException, IOException {
		return Color.fromHSB(getHue(), getSaturation(), getBrightness());
	}
	
	/**
	 * Sets the color (HSB/RGB) of the device.
	 * 
	 * @param color                the new color
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public void setColor(Color color)
			throws NanoleafException, IOException {
		setHue(color.getHue());
		setSaturation(color.getSaturation());
		setBrightness(color.getBrightness());
	}
	
	/**
	 * Gets the name of the currently selected effect on the device.
	 * 
	 * @return                     the name of the effect
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public String getCurrentEffectName()
			throws NanoleafException, IOException {
		return get(getURL("effects/select")).replace("\"", "");
	}
	
	public void getCurrentEffectNameAsync(NanoleafCallback<String> callback) {
		getAsync(getURL("effects/select"), (status, data, device) -> {
			callback.onCompleted(status, data.replace("\"", ""), device);
		});
	}
	
	/**
	 * Gets the currently selected effect as an <code>Effect</code> object.
	 * 
	 * @return                     the effect object
	 * @throws NanoleafException   If the access token is invalid, or the
	 *                             effect does not exist on the device
	 * @throws IOException         If an HTTP exception occurs
	 */
	public Effect getCurrentEffect()
			throws NanoleafException, IOException, InterruptedException {
		return getEffect(getCurrentEffectName());
	}
	
	public void getCurrentEffectAsync(NanoleafCallback<Effect> callback) {
		getCurrentEffectNameAsync((status, data, device) -> {
			if (status != NanoleafCallback.SUCCESS) {
				callback.onCompleted(status, null, device);
			}
			getEffectAsync(data, callback);
		});
	}
	
	/**
	 * Sets the selected effect on the device to the effect specified by
	 * <code>effectName</code>.
	 * 
	 * @param effectName           the name of the effect
	 * @throws NanoleafException   If the access token is invalid, or the
	 *                             effect does not exist on the device
	 * @throws IOException         If an HTTP exception occurs
	 */
	public void setEffect(String effectName)
			throws NanoleafException, IOException {
		String body = String.format("{\"select\": \"%s\"}", effectName);
		put(getURL("effects"), body);
	}
	
	public void setEffectAsync(String effectName, NanoleafCallback<String> callback) {
		String body = String.format("{\"select\": \"%s\"}", effectName);
		putAsync(getURL("effects"), body, callback);
	}
	
	/**
	 * Sets a random effect based on the effects installed on the
	 * device controller. This includes dynamic as well as Rhythm effects.
	 * 
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public void setRandomEffect()
			throws NanoleafException, IOException {
		String[] effects = getEffectsList();
		String currentEffect = getCurrentEffectName();
		String effect = currentEffect;
		while (effect.equals(currentEffect)) {
			int i = new Random().nextInt(effects.length);
			effect = effects[i];
		}
		setEffect(effect);
	}
	
	/**
	 * Gets a string array of all the effects installed on the device. This
	 * includes static, dynamic, and Rhythm effects.
	 * 
	 * @return                     a string array of all the effects
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public String[] getEffectsList()
			throws NanoleafException, IOException {
		JSONArray json = new JSONArray(get(getURL("effects/effectsList")));
		String[] effects = new String[json.length()];
		for (int i = 0; i < json.length(); i++)
			effects[i] = json.getString(i);
		return effects;
	}
	
	public void getEffectsListAsync(NanoleafCallback<String[]> callback) {
		getAsync(getURL("effects/effectsList"), (status, data, device) -> {
			if (status != NanoleafCallback.SUCCESS) {
				callback.onCompleted(status, null, device);
			}
			JSONArray json = new JSONArray(data);
			String[] effects = new String[json.length()];
			for (int i = 0; i < json.length(); i++)
				effects[i] = json.getString(i);
			callback.onCompleted(status, effects, device);
		});
	}
	
	/**
	 * Creates an <code>Effect</code> object from the JSON data for the effect
	 * <code>effectName</code>.
	 * 
	 * @param effectName           the name of the effect
	 * @return                     a new <code>Effect</code> object based on the effect
	 *                             <code>effectName</code>
	 * @throws NanoleafException   If the access token is invalid, or the specified effect
	 *                             does not exist on the device
	 * @throws IOException         If an HTTP exception occurs
	 */
	public Effect getEffect(String effectName)
			throws NanoleafException, IOException, InterruptedException {
		String body = String.format("{\"write\": {\"command\": \"request\", \"animName\": \"%s\"}}", effectName);
		return Effect.createFromJSON(put(getURL("effects"), body));
	}
	
	public void getEffectAsync(String effectName, NanoleafCallback<Effect> callback) {
		String body = String.format("{\"write\": {\"command\": \"request\", \"animName\": \"%s\"}}", effectName);
		putAsync(getURL("effects"), body, (status, data, device) -> {
			if (status != NanoleafCallback.SUCCESS) {
				callback.onCompleted(status, null, device);
			}
			callback.onCompleted(status, Effect.fromJSON(data), device);
		});
	}
	
	/**
	 * Gets an array of type <code>Effect</code> containing all of the effects
	 * installed on the device.
	 * 
	 * @return                     an array of the effects installed on the device
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public Effect[] getAllEffects()
			throws NanoleafException, IOException {
		JSONObject json = new JSONObject(writeEffect("{\"command\": \"requestAll\"}"));
		JSONArray animations = json.getJSONArray("animations");
		Effect[] effects = new Effect[animations.length()];
		for (int i = 0; i < animations.length(); i++) {
			effects[i] = Effect.createFromJSON(animations.getJSONObject(i));
		}
		return effects;
	}
	
	public void getAllEffectsAsync(NanoleafCallback<Effect[]> callback) {
		writeEffectAsync("{\"command\": \"requestAll\"}", (status, data, device) -> {
			if (status != NanoleafCallback.SUCCESS) {
				callback.onCompleted(status, null, device);
			}
			JSONObject json = new JSONObject(data);
			JSONArray animations = json.getJSONArray("animations");
			Effect[] effects = new Effect[animations.length()];
			for (int i = 0; i < animations.length(); i++) {
				effects[i] = Effect.createFromJSON(animations.getJSONObject(i));
			}
			callback.onCompleted(status, effects, device);
		});
	}
	
	/**
	 * Uploads and installs the local effect <code>effect</code> to the device. If the
	 * effect does not exist on the device it will be created. If the effect exists
	 * it will be overwritten.
	 * 
	 * @param effect               the effect to be uploaded
	 * @throws NanoleafException   If the access token is invalid, or the effect parameter
	 *                             is configured incorrectly
	 * @throws IOException         If an HTTP exception occurs
	 */
	public void addEffect(Effect effect)
			throws NanoleafException, IOException {
		writeEffect(effect.toJSON("add").toString());
	}
	
	public void addEffectAsync(Effect effect, NanoleafCallback<String> callback) {
		writeEffectAsync(effect.toJSON("add").toString(), callback);
	}
	
	/**
	 * Deletes an effect from the device.
	 * 
	 * @param effectName           the name of the effect
	 * @throws NanoleafException   If the access token is invalid, or the effect
	 *                             does not exist on the device
	 * @throws IOException         If an HTTP exception occurs
	 */
	public void deleteEffect(String effectName)
			throws NanoleafException, IOException {
		writeEffect(String.format("{\"command\": \"delete\", \"animName\": \"%s\"}", effectName));
	}
	
	public void deleteEffectAsync(String effectName, NanoleafCallback<String> callback) {
		writeEffectAsync(String.format("{\"command\": \"delete\", \"animName\": \"%s\"}", effectName), callback);
	}
	
	/**
	 * Renames an effect on the device.
	 * 
	 * @param effectName           the name of the effect
	 * @param newName              the new name of the effect
	 * @throws NanoleafException   If the access token is invalid, or the effect
	 *                             does not exist on the device, or the new name
	 *                             is illegal
	 * @throws IOException         If an HTTP exception occurs
	 */
	public void renameEffect(String effectName, String newName)
			throws NanoleafException, IOException {
		writeEffect(String.format("{\"command\": \"rename\", \"animName\": \"%s\", \"newName\": \"%s\"}",
				effectName, newName));
	}
	
	public void renameEffectAsync(String effectName, String newName, NanoleafCallback<String> callback) {
		writeEffectAsync(String.format("{\"command\": \"rename\", \"animName\": \"%s\", \"newName\": \"%s\"}",
				effectName, newName), callback);
	}
	
	/**
	 * Displays an effect on the device without installing it.
	 * 
	 * @param effect               the effect to be previewed
	 * @throws NanoleafException   If the access token is invalid, or the effect
	 *                             parameter is configured incorrectly
	 * @throws IOException         If an HTTP exception occurs
	 */
	public void displayEffect(Effect effect)
			throws NanoleafException, IOException {
		writeEffect(effect.toJSON("display").toString());
	}
	
	public void displayEffectAsync(Effect effect, NanoleafCallback<String> callback) {
		writeEffectAsync(effect.toJSON("display").toString(), callback);
	}
	
	/**
	 * Displays an effect on the device for a given duration without installing it.
	 * 
	 * @param effectName           the name of the effect to be previewed
	 * @param duration             the duration for the effect to be displayed
	 * @throws NanoleafException   If the access token is invalid, or the specified
	 *                             effect does not exist on the device
	 * @throws IOException         If an HTTP exception occurs
	 */
	public void displayEffectFor(String effectName, int duration)
			throws NanoleafException, IOException {
		writeEffect(String.format("{\"command\": \"displayTemp\", \"duration\": %d, \"animName\": \"%s\"}", duration, effectName));
	}
	
	public void displayEffectForAsync(String effectName, int duration, NanoleafCallback<String> callback) {
		writeEffectAsync(String.format("{\"command\": \"displayTemp\", \"duration\": %d, \"animName\": \"%s\"}", duration, effectName), callback);
	}
	
	/**
	 * Sets the color of a single panel on the device.
	 * 
	 * @param panel                the target panel
	 * @param red                  the red RGB value
	 * @param green                the green RGB value
	 * @param blue                 the blue RGB value
	 * @param transitionTime       the time to transition to this frame from
	 * 						       the previous frame (must be 1 or greater)
	 * @throws NanoleafException   If the access token is invalid, or the specified panel does
	 *                             not exist, or the RGB values are invalid (must be 0 &#60;
	 *                             x &#60; 255)
	 * @throws IOException         If an HTTP exception occurs
	 */
	public void setPanelColor(Panel panel, int red, int green, int blue, int transitionTime)
			throws NanoleafException, IOException {
		setPanelColor(panel.getId(), red, green, blue, transitionTime);
	}
	
	/**
	 * Sets the color of a single panel on the device.
	 * 
	 * @param panel                the target panel
	 * @param hexColor             the new hex color
	 * @param transitionTime       the time to transition to this frame from
	 * 						       the previous frame (must be 1 or greater)
	 * @throws NanoleafException   If the access token is invalid, or the specified panel does
	 *                             not exist, or the RGB values are invalid (must be 0 &#60;
	 *                             x &#60; 255)
	 * @throws IOException         If an HTTP exception occurs
	 */
	public void setPanelColor(Panel panel, String hexColor, int transitionTime)
			throws NanoleafException, IOException {
		java.awt.Color color = java.awt.Color.decode(hexColor);
		setPanelColor(panel, color.getRed(),
				color.getGreen(), color.getBlue(), transitionTime);
	}
	
	/**
	 * Sets the color of a single panel on the device.
	 * 
	 * @param panelId              the target panel id
	 * @param red                  the red RGB value
	 * @param green                the green RGB value
	 * @param blue                 the blue RGB value
	 * @param transitionTime       the time to transition to this frame from
	 * 						       the previous frame (must be 1 or greater)
	 * @throws NanoleafException   If the access token is invalid, or the specified panel does
	 *                             not exist, or the RGB values are invalid (must be 0 &#60;
	 *                             x &#60; 255)
	 * @throws IOException         If an HTTP exception occurs
	 */
	public void setPanelColor(int panelId, int red, int green, int blue, int transitionTime)
			throws NanoleafException, IOException {
		CustomEffect custom = new CustomEffect();
		custom.setVersion("1.0"); // ?
		custom.setAnimationData("1 " + panelId + " 1 " +
				red + " " + green + " " + blue + " 0 " + transitionTime);
		custom.setLoopEnabled(false);
		displayEffect(custom);
	}
	
	/**
	 * Sets the color of a single panel on the device.
	 * @param panelId              the target panel id
	 * @param hexColor             the new hex color
	 * @param transitionTime       the time to transition to this frame from
	 * 						       the previous frame (must be 1 or greater)
	 * @throws NanoleafException   If the access token is invalid, or the hex color
	 *                             is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public void setPanelColor(int panelId, String hexColor, int transitionTime)
			throws NanoleafException, IOException, InterruptedException {
		java.awt.Color color = java.awt.Color.decode(hexColor);
		setPanelColor(panelId, color.getRed(),
				color.getGreen(), color.getBlue(), transitionTime);
	}
	
	/**
	 * Fades all of the panels to an RGB color over a period of time.
	 * 
	 * @param red                  the red RGB value
	 * @param green                the green RGB value
	 * @param blue                 the blue RGB value
	 * @param duration             the fade time, in hertz (10Hz = 1sec)
	 * @throws NanoleafException   If the access token is invalid, or the RGB values
	 *                             are invalid (must be 0 &#60; x &#60; 255), or the
	 *                             duration is negative
	 * @throws IOException         If an HTTP exception occurs
	 */
	public void fadeToColor(int red, int green, int blue, int duration)
			throws NanoleafException, IOException, InterruptedException {
		CustomEffect ef = new CustomEffect.Builder(NanoleafDevice.this)
							.addFrameToAllPanels(new Frame(red, green, blue, duration))
							.build(null, false);
		displayEffect(ef);
	}
	
	/**
	 * Fades all of the panels to a hex color over a period of time.
	 * 
	 * @param hexColor             the new hex color
	 * @param duration             the fade time <i>in hertz (frames per second)</i>
	 * @throws NanoleafException   If the access token is invalid, or the hex color is
	 *                             invalid, or the duration is negative
	 * @throws IOException         If an HTTP exception occurs
	 */
	public void fadeToColor(String hexColor, int duration)
			throws NanoleafException, IOException, InterruptedException {
		java.awt.Color color = java.awt.Color.decode(hexColor);
		fadeToColor(color.getRed(), color.getGreen(), color.getBlue(), duration);
	}
	
	/**
	 * <p>Gets all the plugins/motions from the device.</p>
	 * 
	 * <p><b>Note:</b> This method is slow.</p>
	 * 
	 * @return  an array of plugins from the device
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public Plugin[] getPlugins()
			throws NanoleafException, IOException {
		String body = String.format("{\"write\": {\"command\": \"requestPlugins\"}}");
		JSONObject json = new JSONObject(put(getURL("effects"), body));
		JSONArray arr = json.getJSONArray("plugins");
		return jsonToPlugins(arr);
	}
	
	public void getPluginsAsync(NanoleafCallback<Plugin[]> callback) {
		String body = String.format("{\"write\": {\"command\": \"requestPlugins\"}}");
		putAsync(getURL("effects"), body, (status, data, device) -> {
			JSONArray json = new JSONArray(data);
			callback.onCompleted(status, jsonToPlugins(json), device);
		});
	}
	
	private Plugin[] jsonToPlugins(JSONArray json) {
		Plugin[] plugins = new Plugin[json.length()];
		for (int i = 0; i < json.length(); i++) {
			plugins[i] = Plugin.fromJSON(json.getJSONObject(i));
		}
		return plugins;
	}
	
	/**
	 * <p><b>(This method works with JSON data)</b></p>
	 * 
	 * <p>Uploads a JSON string to the device. Calls the <code>write</code> effect
	 * command from the <a href = "https://forum.nanoleaf.me/docs#_u2t4jzmkp8nt">OpenAPI</a>.
	 * Refer to it for more information about the commands.</p>
	 * 
	 * <h1>Commands:</h1>
	 * <ul>
	 *   <li><i>add</i>          -  Installs an effect on the device or updates the effect
	 *                              if it already exists.</li>
	 *   <li><i>delete</i>       -  Permanently removes an effect from the device.</li>
	 *   <li><i>request</i>      -  Requests a single effect by name.</li>
	 *   <li><i>requestAll</i>   -  Requests all the installed effects from the device.</li>
	 *   <li><i>display</i>      -  Sets a color mode on the device (used for previewing
	 *                              effects).</li>
	 *   <li><i>displayTemp</i>  -  Temporarily sets a color mode on the device (typically
	 *                              used for notifications of visual indicators).</li>
	 *   <li><i>rename</i>       -  Changes the name of an effect on the device.</li>
	 * </ul>
	 * 
	 * @param command              the operation to perform the write with
	 * @throws NanoleafException   If the access token is invalid, or the command parameter
	 *                             is invalid JSON, or the command parameter contains an
	 *                             invalid command or has invalid command options
	 * @throws IOException         If an HTTP exception occurs
	 */
	public String writeEffect(String command)
			throws NanoleafException, IOException {
		String body = String.format("{\"write\": %s}", command);
		return put(getURL("effects"), body);
	}
	
	public void writeEffectAsync(String command, NanoleafCallback<String> callback) {
		String body = String.format("{\"write\": %s}", command);
		putAsync(getURL("effects"), body, callback);
	}
	
	/**
	 * <p>Gets an array of the connected panels.</p>
	 * 
	 * <p>This is the ORIGINAL location data.
	 * Since the original location data is not affected by the global orientation,
	 * this data may not accurately represent the panels if displayed as is. For rotated
	 * panel data, use the {@link NanoleafDevice#getPanelsRotated} method instead.</p>
	 * 
	 * @return                     an array of panels
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public Panel[] getPanels()
			throws NanoleafException, IOException {
		return parsePanelsJSON(get(getURL("panelLayout/layout")));
	}
	
	public void getPanelsAsync(NanoleafCallback<Panel[]> callback) {
		getAsync(getURL("panelLayout/layout"), (status, data, device) -> {
			if (status != NanoleafCallback.SUCCESS) {
				callback.onCompleted(status, null, device);
			}
			Panel[] panels = parsePanelsJSON(data);
			callback.onCompleted(status, panels, device);
		});
	}
	
	private Panel[] parsePanelsJSON(String jsonStr) {
		if (jsonStr == null) {
			return null;
		}
		JSONObject json = new JSONObject(jsonStr);
		JSONArray arr = json.getJSONArray("positionData");
		Panel[] pd = new Panel[arr.length()];
		for (int i = 0; i < arr.length(); i++) {
			JSONObject data = arr.getJSONObject(i);
			int panelId = data.getInt("panelId");
			int x = data.getInt("x");
			int y = data.getInt("y");
			int o = data.getInt("o");
			ShapeType s = new ShapeType(data.getInt("shapeType"));
			pd[i] = new Panel(panelId, x, y, o, s);
		}
		return pd;
	}
	
	/**
	 * Gets an array of the connected panels that are rotated to match the
	 * global orientation.
	 * 
	 * @return                     an array of rotated panels
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public Panel[] getPanelsRotated()
			throws NanoleafException, IOException {
		Panel[] panels = getPanels();
		Point origin = getLayoutCentroid(panels);
		int globalOrientation = getGlobalOrientation();
		globalOrientation = globalOrientation == 360 ? 0 : globalOrientation;
		double radAngle = Math.toRadians(globalOrientation);
		for (Panel p : panels) {
			int x = p.getX() - origin.x;
			int y = p.getY() - origin.y;
			
			double newX = x * Math.cos(radAngle) - y * Math.sin(radAngle);
			double newY = x * Math.sin(radAngle) + y * Math.cos(radAngle);
			
			x = (int)(newX + origin.x);
			y = (int)(newY + origin.y);
			p.setX(x);
			p.setY(y);
		}
		return panels;
	}
	
	/**
	 * Finds a <code>Panel</code> object if you have a panel ID but not the panel object.
	 * @param id                   the panel id for the panel
	 * @return                     a panel with the same id, or null if no panel is found
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public Panel getPanel(int id)
			throws NanoleafException, IOException {
		return getPanel(id, getPanels());
	}
	
	public void getPanelAsync(int id, NanoleafCallback<Panel> callback) {
		getPanelsAsync((status, data, device) -> {
			if (status != NanoleafCallback.SUCCESS) {
				callback.onCompleted(status, null, device);
			}
			Panel panel = getPanel(id, data);
			callback.onCompleted(status, panel, device);
		});
	}
	
	private Panel getPanel(int id, Panel[] panels) {
		for (Panel panel : panels) {
			if (panel.getId() == id) {
				return panel;
			}
		}
		return null;
	}
	
	/**
	 * Gets the global orientation for the device.
	 * @return                     the global orientation
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public int getGlobalOrientation()
			throws NanoleafException, IOException {
		return Integer.parseInt(get(getURL("panelLayout/globalOrientation/value")));
	}
	
	public void getGlobalOrientationAsync(NanoleafCallback<Integer> callback) {
		getAsyncInt(getURL("panelLayout/globalOrientation/value"), callback);
	}
	
	/**
	 * Sets the global orientation for the device.
	 * 
	 * @param orientation          the global orientation
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public void setGlobalOrientation(int orientation)
			throws NanoleafException, IOException {
		String body = String.format("{\"globalOrientation\": {\"value\": %d}}", orientation);
		put(getURL("panelLayout"), body);
	}
	
	public void setGlobalOrientation(int orientation, NanoleafCallback<String> callback) {
		String body = String.format("{\"globalOrientation\": {\"value\": %d}}", orientation);
		putAsync(getURL("panelLayout"), body, callback);
	}
	
	/**
	 * Gets the maximum global orientation for the device.
	 * 
	 * @return                     the maximum global orientation
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public int getMaxGlobalOrientation()
			throws NanoleafException, IOException {
		return Integer.parseInt(get(getURL("panelLayout/globalOrientation/max")));
	}
	
	public void getMaxGlobalOrientationAsync(NanoleafCallback<Integer> callback) {
		getAsyncInt(getURL("panelLayout/globalOrientation/max"), callback);
	}
	
	/**
	 * Gets the minimum global orientation for the device.
	 * @return                     the minimum global orientation
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public int getMinGlobalOrientation()
			throws NanoleafException, IOException {
		return Integer.parseInt(get(getURL("panelLayout/globalOrientation/min")));
	}
	
	public void getMinGlobalOrientationAsync(NanoleafCallback<Integer> callback) {
		getAsyncInt(getURL("panelLayout/globalOrientation/min"), callback);
	}
	
	private Point getLayoutCentroid(Panel[] panels) {
		int centroidX = 0, centroidY = 0;
		int numXPoints = 0, numYPoints = 0;
		List<Integer> xpoints = new ArrayList<Integer>();
		List<Integer> ypoints = new ArrayList<Integer>();
		
		for (Panel p : panels) {
			int x = p.getX();
			int y = p.getY();
			if (!xpoints.contains(x)) {
				centroidX += x;
				xpoints.add(x);
				numXPoints++;
			}
			if (!ypoints.contains(y)) {
				centroidY += y;
				ypoints.add(y);
				numYPoints++;
			}
		}
		centroidX /= numXPoints;
		centroidY /= numYPoints;
		return new Point(centroidX, centroidY);
	}
	
	/**
	 * Gets the <code>SocketAddress</code> containing the hostname and port
	 * of the external streaming controller.
	 * 
	 * @return the <code>SocketAddress</code> of the streaming controller
	 */
	public InetSocketAddress getExternalStreamingAddress() {
		return externalAddress;
	}
	
	/**
	 * Enables external streaming mode over UDP.
	 * 
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public void enableExternalStreaming()
			throws NanoleafException, IOException {
		String body = "{\"write\": {\"command\": \"display\", \"animType\": \"extControl\", \"extControlVersion\": \"v2\"}}";
		put(getURL("effects"), body);
		externalAddress = new InetSocketAddress(hostname, port);
	}
	
	public void enableExternalStreamingAsync(NanoleafCallback<String> callback)
			throws NanoleafException, IOException {
		String body = "{\"write\": {\"command\": \"display\", \"animType\": \"extControl\", \"extControlVersion\": \"v2\"}}";
		putAsync(getURL("effects"), body, (status, data, device) -> {
			if (status == NanoleafCallback.SUCCESS) { 
				externalAddress = new InetSocketAddress(hostname, port);
			}
			callback.onCompleted(status, null, device);
		});
	}
	
	/**
	 * <p>Sends a series of frames to the target device.</p>
	 * 
	 * <p><b>Note:</b>Requires external streaming to be enabled. Enable it
	 * using the {@link NanoleafDevice#enableExternalStreaming} method.</p>
	 * 
	 * @param effect               the custom effect to be sent to the device
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an I/O exception occurs
	 * @throws SocketException     If the target device cannot be found or connected to
	 */
	public void sendStaticEffect(StaticEffect effect)
			throws NanoleafException, IOException
	{
		sendAnimData(effect.getAnimationData());
	}
	
	/**
	 * <p>Sends a static animation data string to the target device.</p>
	 * 
	 * <p><b>Note:</b>Requires external streaming to be enabled. Enable it
	 * using the {@link NanoleafDevice#enableExternalStreaming} method.</p>
	 * 
	 * @param animData             the static animation data to be sent to the device
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an I/O exception occurs
	 * @throws SocketException     If the target device cannot be found or connected to
	 */
	public void sendAnimData(String animData)
			throws NanoleafException, IOException {
		byte[] data = animDataToBytes(animData);
		
		DatagramPacket packet = new DatagramPacket(data,
				data.length, externalAddress.getAddress(), externalAddress.getPort());
		
		try {
			DatagramSocket socket = new DatagramSocket();
			socket.send(packet);
			socket.close();
		}
		catch (SocketException se) {
			throw new SocketException("Failed to connect to target device.");
		}
		catch (IOException ioe) {
			throw new IOException("I/O error.");
		}
	}
	
	/**
	 * Updates the color of a single panel.
	 * 
	 * @param panelId              the id of the panel to update
	 * @param red                  the red RGB value
	 * @param green                the green RGB value
	 * @param blue                 the blue RGB value
	 * @param transitionTime       the time to transition to this frame from
	 * 						       the previous frame (must be 1 or greater)
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an I/O exception occurs
	 * @throws SocketException     If the target device cannot be found or connected to
	 */
	public void setPanelExternalStreaming(int panelId, int red, int green, int blue, int transitionTime)
			throws NanoleafException, IOException {
		String frame = String.format("%s %s %d %d %d 0 %s",
				intToBigEndian(1), intToBigEndian(panelId), red, green,
				blue, intToBigEndian(transitionTime));
		sendAnimData(frame);
	}
	
	/**
	 * Updates the color of a single panel using external streaming.
	 * 
	 * @param panel                the panel to update
	 * @param red                  the red RGB value
	 * @param green                the green RGB value
	 * @param blue                 the blue RGB value
	 * @param transitionTime       the time to transition to this frame from
	 * 						       the previous frame (must be 1 or greater)
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an I/O exception occurs
	 * @throws SocketException     If the target device cannot be found or connected to
	 */
	public void setPanelExternalStreaming(Panel panel, int red, int green, int blue, int transitionTime)
			throws NanoleafException, IOException {
		setPanelExternalStreaming(panel.getId(), red, green, blue, transitionTime);
	}
	
	/**
	 * Updates the color of a single panel using external streaming.
	 * 
	 * @param panelId              the id of the panel to update
	 * @param color                the color object
	 * @param transitionTime       the time to transition to this frame from
	 * 						       the previous frame (must be 1 or greater)
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an I/O exception occurs
	 * @throws SocketException     If the target device cannot be found or connected to
	 */
	public void setPanelExternalStreaming(int panelId, Color color, int transitionTime)
			throws NanoleafException, IOException {
		setPanelExternalStreaming(panelId, color.getRed(),
				color.getGreen(), color.getBlue(), transitionTime);
	}
	
	/**
	 * Updates the color of a single panel using external streaming.
	 * 
	 * @param panel                the panel to update
	 * @param color                the color object
	 * @param transitionTime       the time to transition to this frame from
	 * 						       the previous frame (must be 1 or greater)
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an I/O exception occurs
	 * @throws SocketException     If the target device cannot be found or connected to
	 */
	public void setPanelExternalStreaming(Panel panel, Color color, int transitionTime)
			throws NanoleafException, IOException {
		setPanelExternalStreaming(panel.getId(), color.getRed(),
				color.getGreen(), color.getBlue(), transitionTime);
	}
	
	/**
	 * Updates the color of a single panel using external streaming.
	 * 
	 * @param panelId              the id of the panel to update
	 * @param hexColor             the hex color code
	 * @param transitionTime       the time to transition to this frame from
	 * 						       the previous frame (must be 1 or greater)
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an I/O exception occurs
	 * @throws SocketException     If the target device cannot be found or connected to
	 */
	public void setPanelExternalStreaming(int panelId, String hexColor, int transitionTime)
			throws NanoleafException, IOException {
		java.awt.Color color = java.awt.Color.decode(hexColor);
		setPanelExternalStreaming(panelId, color.getRed(),
				color.getGreen(), color.getBlue(), transitionTime);
	}
	
	/**
	 * Updates the color of a single panel using external streaming.
	 * 
	 * @param panel                the panel to update
	 * @param hexColor             the hex color code
	 * @param transitionTime       the time to transition to this frame from
	 * 						       the previous frame (must be 1 or greater)
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an I/O exception occurs
	 * @throws SocketException     If the target device cannot be found or connected to
	 */
	public void setPanelExternalStreaming(Panel panel, String hexColor, int transitionTime)
			throws NanoleafException, IOException {
		java.awt.Color color = java.awt.Color.decode(hexColor);
		setPanelExternalStreaming(panel.getId(), color.getRed(),
				color.getGreen(), color.getBlue(), transitionTime);
	}
	
	private byte[] animDataToBytes(String animData) {
		String[] dataStr = animData.split(" ");
		byte[] dataBytes = new byte[dataStr.length];
		for (int i = 0; i < dataStr.length; i++)
			dataBytes[i] = (byte)Integer.parseInt(dataStr[i]);
		return dataBytes;
	}
	
	private static String intToBigEndian(int num) {
		final int BYTE_SIZE = 256;
		int times = Math.floorDiv(num, BYTE_SIZE);
		return String.format("%s %s", times, num-(BYTE_SIZE*times));
	}
	
	public ServerSentEvent registerTouchEventListener(NanoleafEventListener listener,
			boolean stateEvents, boolean layoutEvents, boolean effectsEvents, boolean touchEvents) {
		String url = getURL("events" + getEventsQueryString(stateEvents, layoutEvents, effectsEvents, touchEvents));
		Request req = new Request.Builder()
				.url(url)
				.get()
				.build();
		OkSse okSse = new OkSse(client);
		ServerSentEvent s = okSse.newServerSentEvent(req, listener);
		sse.add(s);
		return s;
	}
	
	private String getEventsQueryString(boolean stateEvents, boolean layoutEvents, boolean effectsEvents, boolean touchEvents) {
		StringBuilder query = new StringBuilder("?id=");
		if (stateEvents) {
			query.append("1");
		}
		if (layoutEvents) {
			if (query.length() > 0) {
				query.append(",");
			}
			query.append("2");
		}
		if (effectsEvents) {
			if (query.length() > 0) {
				query.append(",");
			}
			query.append("3");
		}
		if (effectsEvents) {
			if (query.length() > 0) {
				query.append(",");
			}
			query.append("4");
		}
		return query.toString();
	}
	
	/**
	 * Gets an array of schedules stored on the device.
	 * 
	 * @return  an array of schedules
	 */
	public Schedule[] getSchedules()
			throws NanoleafException, IOException {
		JSONObject obj = new JSONObject(get(getURL("schedules")));
		JSONArray arr = obj.getJSONArray("schedules");
		Schedule[] schedules = new Schedule[arr.length()];
		for (int i = 0; i < arr.length(); i++) {
			schedules[i] = Schedule.fromJSON(
					arr.getJSONObject(i).toString());
		}
		return schedules;
	}
	
	/**
	 * Uploads an array of schedules to the device.
	 * 
	 * @param schedules            an array of schedules
	 * @throws NanoleafException   If the access token is invalid, or one or more
	 *                             given schedules are configured incorrectly
	 * @throws IOException         If an HTTP exception occurs
	 */
	public void addSchedules(Schedule[] schedules)
			throws NanoleafException, IOException {
		String schedulesStr = "\"schedules\":[";
		for (int i = 0; i < schedules.length; i++) {
			schedulesStr += schedules[i];
			if (i < schedules.length-1) {
				schedulesStr += ",";
			}
			else {
				schedulesStr += "]";
			}
		}
		String body = String.format("{\"write\":{\"command\":" +
				"\"addSchedules\",%s}}", schedulesStr);
		put(getURL("effects"), body);
	}
	
	/**
	 * Uploads a schedule to the device.
	 * 
	 * @param schedule             the schedule to upload
	 * @throws NanoleafException   If the access token is invalid, or one or more
	 *                             given schedules are configured incorrectly
	 * @throws IOException         If an HTTP exception occurs
	 */
	public void addSchedule(Schedule schedule)
			throws NanoleafException, IOException {
		addSchedules(new Schedule[]{schedule});
	}
	
	/**
	 * Deletes an array of schedules from the device.
	 * 
	 * @param schedules            an array of schedules be deleted
	 * @throws NanoleafException   If the access token is invalid, or one or more
	 *                             given schedules do not exist on the device
	 * @throws IOException         If an HTTP exception occurs
	 */
	public void removeSchedules(Schedule[] schedules)
			throws NanoleafException, IOException {
		int[] ids = new int[schedules.length];
		for (int i = 0; i < schedules.length; i++) {
			ids[i] = schedules[i].getId();
		}
		removeSchedulesById(ids);
	}
	
	/**
	 * Deletes a schedule from the device.
	 * 
	 * @param schedule             the schedule to delete
	 * @throws NanoleafException   If the access token is invalid, or the given
	 *                             schedule does not exist on the device
	 * @throws IOException         If an HTTP exception occurs
	 */
	public void removeSchedule(Schedule schedule)
			throws NanoleafException, IOException {
		removeSchedules(new Schedule[]{schedule});
	}
	
	/**
	 * Deletes an array of schedules from the device using their unique schedule IDs.
	 * 
	 * @param scheduleIds          an array of schedule IDs to be delete
	 * @throws NanoleafException   If the access token is invalid, or the device
	 *                             does not contain one or more of the schedule IDs
	 * @throws IOException         If an HTTP exception occurs
	 */
	public void removeSchedulesById(int[] scheduleIds)
			throws NanoleafException, IOException {
		String schedulesStr = "\"schedules\":[";
		for (int i = 0; i < scheduleIds.length; i++) {
			schedulesStr += String.format("{\"id\":%d}",
					scheduleIds[i]);
			if (i < scheduleIds.length-1) {
				schedulesStr += ",";
			}
			else {
				schedulesStr += "]";
			}
		}
		String body = String.format("{\"write\":{\"command\":" +
				"\"removeSchedules\",%s}}", schedulesStr);
		put(getURL("effects"), body);
	}
	
	/**
	 * Deletes a schedule from the device using its unique schedule ID.
	 * 
	 * @param scheduleId           the schedule ID of the schedule to be deleted
	 * @throws NanoleafException   If the access token is invalid, or the device
	 *                             does not contain the schedule ID
	 * @throws IOException         If an HTTP exception occurs
	 */
	public void removeScheduleById(int scheduleId)
			throws NanoleafException, IOException {
		removeSchedulesById(new int[]{scheduleId});
	}
	
	/**
	 * Constructs a full URL to make an API call.
	 * @param endpoint  the final location in the API call
	 * @return  a URL that points to some endpoint on a specific device
	 */
	protected String getURL(String endpoint) {
		return String.format("http://%s:%d/api/%s/%s/%s",
				hostname, port, API_LEVEL, accessToken, endpoint);
	}
	
	private static String getURL(String endpoint, String hostname, int port, String accessToken) {
		return String.format("http://%s:%d/api/%s/%s/%s",
				hostname, port, API_LEVEL, accessToken, endpoint);
	}
	
	protected String get(String url) throws NanoleafException, IOException {
		Response resp = HttpUtil.getHttpSync(client, url);
		NanoleafException.checkStatusCode(resp.code());
		return resp.body().string();
	}
	
	protected String post(String url, String data) throws NanoleafException, IOException {
		Response resp = HttpUtil.postHttpSync(client, url, data);
		NanoleafException.checkStatusCode(resp.code());
		return resp.body().string();
	}
	
	protected String put(String url, String data) throws NanoleafException, IOException {
		Response resp = HttpUtil.putHttpSync(client, url, data);
		if (resp == null) {
			return null;
		}
		NanoleafException.checkStatusCode(resp.code());
		return resp.body().string();
	}
	
	protected void getAsyncInt(String url, NanoleafCallback<Integer> callback) {
		getAsync(url, (status, data, device) -> {
			try {
				int value = Integer.parseInt(data);
				callback.onCompleted(status, value, device);
			}
			catch (NumberFormatException e) {
				callback.onCompleted(NanoleafCallback.FAILURE, 0, device);
			}
		});
	}
	
	protected void getAsyncBool(String url, NanoleafCallback<Boolean> callback) {
		getAsync(url, (status, data, device) -> {
			try {
				boolean value = Boolean.parseBoolean(data);
				callback.onCompleted(status, value, device);
			}
			catch (NumberFormatException e) {
				callback.onCompleted(NanoleafCallback.FAILURE, false, device);
			}
		});
	}
	
	protected void getAsync(String url, NanoleafCallback<String> callback) {
		HttpUtil.getHttpAsync(client, url, new Callback() {
			@Override
			public void onFailure(Call call, IOException e) {
				if (callback != null) {
					callback.onCompleted(NanoleafCallback.FAILURE, null, NanoleafDevice.this);
				}
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException {
				if (callback != null) {
					int code = response.code();
					if (code == 200 || code == 204) {
						code = NanoleafCallback.SUCCESS;
					}
					callback.onCompleted(code, response.body().string(), NanoleafDevice.this);
				}
			}
		});
	}
	
	protected void postAsync(String url, String data, NanoleafCallback<String> callback) {
		HttpUtil.postHttpAsync(client, url, data, new Callback() {
			@Override
			public void onFailure(Call call, IOException e) {
				if (callback != null) {
					callback.onCompleted(NanoleafCallback.FAILURE, null, NanoleafDevice.this);
				}
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException {
				if (callback != null) {
					int code = response.code();
					if (code == 200 || code == 204) {
						code = NanoleafCallback.SUCCESS;
					}
					callback.onCompleted(code, response.body().string(), NanoleafDevice.this);
				}
			}
		});
	}
	
	protected void putAsync(String url, String data, NanoleafCallback<String> callback) {
		HttpUtil.putHttpAsync(client, url, data, new Callback() {
			@Override
			public void onFailure(Call call, IOException e) {
				if (callback != null) {
					callback.onCompleted(NanoleafCallback.FAILURE, null, NanoleafDevice.this);
				}
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException {
				if (callback != null) {
					int code = response.code();
					if (code == 200 || code == 204) {
						code = NanoleafCallback.SUCCESS;
					}
					callback.onCompleted(code, response.body().string(), NanoleafDevice.this);
				}
			}
		});
	}
}
