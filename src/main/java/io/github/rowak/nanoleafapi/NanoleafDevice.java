package io.github.rowak.nanoleafapi;

import org.json.JSONArray;
import org.json.JSONObject;

import com.here.oksse.OkSse;
import com.here.oksse.ServerSentEvent;

import io.github.rowak.nanoleafapi.event.Event;
import io.github.rowak.nanoleafapi.event.NanoleafEventListener;
import io.github.rowak.nanoleafapi.event.NanoleafTouchEventListener;
import io.github.rowak.nanoleafapi.event.UDPTouchEventListener;
import io.github.rowak.nanoleafapi.schedule.Schedule;
import io.github.rowak.nanoleafapi.util.HttpUtil;
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
	
	/** The port for external streaming v2 */
	public static final int EXTERNAL_STREAMING_PORT = 60222;
	
	/** Internal HTTP client for communications with the Nanoleaf device */
	private OkHttpClient client;
	
	/** Internal SSE clients record (for resource cleanup) */
	private List<ServerSentEvent> sse;
	private UDPTouchEventListener touchEventListener;
	private Thread touchEventThread;
	
	private String hostname, accessToken;
	private int port;
	
	private int touchEventStreamingPort = -1;
	
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
	public static final NanoleafDevice createDevice(String hostname, int port, String accessToken)
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
	public static final void createDeviceAsync(String hostname, int port, String accessToken, NanoleafCallback<NanoleafDevice> callback)
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
	private static final NanoleafDevice createDeviceFromName(String name,
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
	 * {@link NanoleafDevice#registerEventListener(NanoleafEventListener,
	 * boolean, boolean, boolean, boolean)}.
	 */
	public void closeEventListeners() {
		for (ServerSentEvent s : sse) {
			s.close();
		}
	}
	
	/**
	 * Closes all low latency touch event (streaming) listeners that
	 * have been opened with {@link NanoleafDevice#registerTouchEventStreamingListener(NanoleafTouchEventListener)}
	 */
	public void closeTouchEventListeners() {
		touchEventListener.close();
		touchEventListener = null;
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
	 * <p>The callback status will return {@link NanoleafCallback#SUCCESS} on success,
	 * or {@link NanoleafCallback#UNAUTHORIZED} if the access token is invalid. If an
	 * internal API error occurs, it will instead return {@link NanoleafCallback#FAILURE}.</p>
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
	 * <p>The callback status will return {@link NanoleafCallback#SUCCESS} on success,
	 * or {@link NanoleafCallback#UNAUTHORIZED} if the access token is invalid. If an
	 * internal API error occurs, it will instead return {@link NanoleafCallback#FAILURE}.</p>
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
	 * <p>The callback status will return {@link NanoleafCallback#SUCCESS} on success,
	 * or {@link NanoleafCallback#UNAUTHORIZED} if the access token is invalid. If an
	 * internal API error occurs, it will instead return {@link NanoleafCallback#FAILURE}.</p>
	 * 
	 * @param callback   returns the on state
	 */
	public void getOnAsync(NanoleafCallback<Boolean> callback) {
		getAsyncBool(getURL("state/on/value"), callback);
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
	 * <p>The callback status will return {@link NanoleafCallback#SUCCESS} on success,
	 * or {@link NanoleafCallback#UNAUTHORIZED} if the access token is invalid. If an
	 * internal API error occurs, it will instead return {@link NanoleafCallback#FAILURE}.</p>
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
	public boolean toggleOn()
			throws NanoleafException, IOException {
		boolean on = getOn();
		setOn(!on);
		return !on;
	}
	
	/**
	 * <p>Toggles the on state of the device (on = off, off = on).</p>
	 * 
	 * <p>The callback status will return {@link NanoleafCallback#SUCCESS} on success,
	 * or {@link NanoleafCallback#UNAUTHORIZED} if the access token is invalid. If an
	 * internal API error occurs, it will instead return {@link NanoleafCallback#FAILURE}.</p>
	 * 
	 * @param callback             called when the the device changes
	 *                             power state or if error occurs
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public void toggleOnAsync(NanoleafCallback<Boolean> callback)
			throws NanoleafException, IOException {
		getOnAsync((status, on, device) -> {
			if (status != NanoleafCallback.SUCCESS) {
				callback.onCompleted(status, null, device);
			}
			else {
				try {
					setOn(!on);
					callback.onCompleted(NanoleafCallback.SUCCESS, !on, device);
				}
				catch (NanoleafException e) {
					callback.onCompleted(status, !on, device);
				}
				catch (Exception e) {
					callback.onCompleted(NanoleafCallback.FAILURE, null, device);
				}
			}
		});
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
	 * <p>The callback status will return {@link NanoleafCallback#SUCCESS} on success,
	 * or {@link NanoleafCallback#UNAUTHORIZED} if the access token is invalid. If an
	 * internal API error occurs, it will instead return {@link NanoleafCallback#FAILURE}.</p>
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
	 * <p>The callback status will return {@link NanoleafCallback#SUCCESS} on success,
	 * or {@link NanoleafCallback#UNAUTHORIZED} if the access token is invalid. If an
	 * internal API error occurs, it will instead return {@link NanoleafCallback#FAILURE}.</p>
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
	 * <p>The callback status will return {@link NanoleafCallback#SUCCESS} on success,
	 * or {@link NanoleafCallback#UNAUTHORIZED} if the access token is invalid. If an
	 * internal API error occurs, it will instead return {@link NanoleafCallback#FAILURE}.</p>
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
	 * <p>The callback status will return {@link NanoleafCallback#SUCCESS} on success,
	 * or {@link NanoleafCallback#UNAUTHORIZED} if the access token is invalid. If an
	 * internal API error occurs, it will instead return {@link NanoleafCallback#FAILURE}.</p>
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
	 * <p>The callback status will return {@link NanoleafCallback#SUCCESS} on success,
	 * or {@link NanoleafCallback#UNAUTHORIZED} if the access token is invalid. If an
	 * internal API error occurs, it will instead return {@link NanoleafCallback#FAILURE}.</p>
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
	 * <p>The callback status will return {@link NanoleafCallback#SUCCESS} on success,
	 * or {@link NanoleafCallback#UNAUTHORIZED} if the access token is invalid. If an
	 * internal API error occurs, it will instead return {@link NanoleafCallback#FAILURE}.</p>
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
	 * <p>The callback status will return {@link NanoleafCallback#SUCCESS} on success,
	 * or {@link NanoleafCallback#UNAUTHORIZED} if the access token is invalid. If an
	 * internal API error occurs, it will instead return {@link NanoleafCallback#FAILURE}.</p>
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
	 * <p>The callback status will return {@link NanoleafCallback#SUCCESS} on success,
	 * or {@link NanoleafCallback#UNAUTHORIZED} if the access token is invalid. If an
	 * internal API error occurs, it will instead return {@link NanoleafCallback#FAILURE}.</p>
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
	 * <p>Asynchronously sets the hue of the device (static/custom effects only).</p>
	 * 
	 * <p>The callback status will return {@link NanoleafCallback#SUCCESS} on success,
	 * or {@link NanoleafCallback#UNAUTHORIZED} if the access token is invalid. If an
	 * internal API error occurs, it will instead return {@link NanoleafCallback#FAILURE}.</p>
	 * 
	 * @param hue        the new hue
	 * @param callback   called when the hue changes or when an error occurs
	 */
	public void setHueAsync(int hue, NanoleafCallback<String> callback) {
		String body = String.format("{\"hue\": {\"value\": %d}}", hue);
		putAsync(getURL("state"), body, callback);
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
	 * <p>The callback status will return {@link NanoleafCallback#SUCCESS} on success,
	 * or {@link NanoleafCallback#UNAUTHORIZED} if the access token is invalid. If an
	 * internal API error occurs, it will instead return
	 * {@link NanoleafCallback#FAILURE}. The returned data will never be meaningful
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
	 * <p>The callback status will return {@link NanoleafCallback#SUCCESS} on success,
	 * or {@link NanoleafCallback#UNAUTHORIZED} if the access token is invalid. If an
	 * internal API error occurs, it will instead return
	 * {@link NanoleafCallback#FAILURE}. The returned data will never be meaningful
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
	 * <p>The callback status will return {@link NanoleafCallback#SUCCESS} on success,
	 * or {@link NanoleafCallback#UNAUTHORIZED} if the access token is invalid. If an
	 * internal API error occurs, it will instead return {@link NanoleafCallback#FAILURE}.</p>
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
	 * <p>The callback status will return {@link NanoleafCallback#SUCCESS} on success,
	 * or {@link NanoleafCallback#UNAUTHORIZED} if the access token is invalid. If an
	 * internal API error occurs, it will instead return {@link NanoleafCallback#FAILURE}.</p>
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
	 * <p>The callback status will return {@link NanoleafCallback#SUCCESS} on success,
	 * or {@link NanoleafCallback#UNAUTHORIZED} if the access token is invalid. If an
	 * internal API error occurs, it will instead return {@link NanoleafCallback#FAILURE}.</p>
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
	 * <p>The callback status will return {@link NanoleafCallback#SUCCESS} on success,
	 * or {@link NanoleafCallback#UNAUTHORIZED} if the access token is invalid. If an
	 * internal API error occurs, it will instead return
	 * {@link NanoleafCallback#FAILURE}. The returned data will never be meaningful
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
	 * <p>The callback status will return {@link NanoleafCallback#SUCCESS} on success,
	 * or {@link NanoleafCallback#UNAUTHORIZED} if the access token is invalid. If an
	 * internal API error occurs, it will instead return
	 * {@link NanoleafCallback#FAILURE}. The returned data will never be meaningful
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
	 * <p>The callback status will return {@link NanoleafCallback#SUCCESS} on success,
	 * or {@link NanoleafCallback#UNAUTHORIZED} if the access token is invalid. If an
	 * internal API error occurs, it will instead return
	 * {@link NanoleafCallback#FAILURE}. The returned data will never be meaningful
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
	 * <p>The callback status will return {@link NanoleafCallback#SUCCESS} on success,
	 * or {@link NanoleafCallback#UNAUTHORIZED} if the access token is invalid. If an
	 * internal API error occurs, it will instead return {@link NanoleafCallback#FAILURE}.</p>
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
	 * <p>The callback status will return {@link NanoleafCallback#SUCCESS} on success,
	 * or {@link NanoleafCallback#UNAUTHORIZED} if the access token is invalid. If an
	 * internal API error occurs, it will instead return {@link NanoleafCallback#FAILURE}.</p>
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
	 * <p>The callback status will return {@link NanoleafCallback#SUCCESS} on success,
	 * or {@link NanoleafCallback#UNAUTHORIZED} if the access token is invalid. If an
	 * internal API error occurs, it will instead return {@link NanoleafCallback#FAILURE}.</p>
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
	 * <p>The callback status will return {@link NanoleafCallback#SUCCESS} on success,
	 * or {@link NanoleafCallback#UNAUTHORIZED} if the access token is invalid. If an
	 * internal API error occurs, it will instead return
	 * {@link NanoleafCallback#FAILURE}. The returned data will never be meaningful
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
	 * <p>The callback status will return {@link NanoleafCallback#SUCCESS} on success,
	 * or {@link NanoleafCallback#UNAUTHORIZED} if the access token is invalid. If an
	 * internal API error occurs, it will instead return
	 * {@link NanoleafCallback#FAILURE}. The returned data will never be meaningful
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
	 * <p>The callback status will return {@link NanoleafCallback#SUCCESS} on success,
	 * or {@link NanoleafCallback#UNAUTHORIZED} if the access token is invalid. If an
	 * internal API error occurs, it will instead return
	 * {@link NanoleafCallback#FAILURE}. The returned data will never be meaningful
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
	 * <p>The callback status will return {@link NanoleafCallback#SUCCESS} on success,
	 * or {@link NanoleafCallback#UNAUTHORIZED} if the access token is invalid. If an
	 * internal API error occurs, it will instead return {@link NanoleafCallback#FAILURE}.</p>
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
	 * <p>The callback status will return {@link NanoleafCallback#SUCCESS} on success,
	 * or {@link NanoleafCallback#UNAUTHORIZED} if the access token is invalid. If an
	 * internal API error occurs, it will instead return {@link NanoleafCallback#FAILURE}.</p>
	 * 
	 * @param callback   returns the minimum color temperature
	 */
	public void getMinColorTemperatureAsync(NanoleafCallback<Integer> callback) {
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
	 * <p>The callback status will return {@link NanoleafCallback#SUCCESS} on success,
	 * or {@link NanoleafCallback#UNAUTHORIZED} if the access token is invalid. If an
	 * internal API error occurs, it will instead return {@link NanoleafCallback#FAILURE}.</p>
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
	
	/**
	 * <p>Gets the name of the currently selected effect on the device.</p>
	 * 
	 * <p>The callback status will return {@link NanoleafCallback#SUCCESS} on success,
	 * or {@link NanoleafCallback#UNAUTHORIZED} if the access token is invalid. If an
	 * internal API error occurs, it will instead return {@link NanoleafCallback#FAILURE}.</p>
	 * 
	 * @param callback   returns the current effect name
	 */
	public void getCurrentEffectNameAsync(NanoleafCallback<String> callback) {
		getAsync(getURL("effects/select"), (status, data, device) -> {
			callback.onCompleted(status, data.replace("\"", ""), device);
		});
	}
	
	/**
	 * Gets the currently selected effect as an Effect object.
	 * 
	 * @return                     the effect object
	 * @throws NanoleafException   If the access token is invalid, or the
	 *                             effect does not exist on the device
	 * @throws IOException         If an HTTP exception occurs
	 */
	public Effect getCurrentEffect()
			throws NanoleafException, IOException {
		return getEffect(getCurrentEffectName());
	}
	
	/**
	 * <p>Asynchronously gets the currently selected effect as an <code>Effect</code> object.</p>
	 * 
	 * <p>The callback status will return {@link NanoleafCallback#SUCCESS} on success,
	 * or {@link NanoleafCallback#UNAUTHORIZED} if the access token is invalid. If an
	 * internal API error occurs, it will instead return {@link NanoleafCallback#FAILURE}.</p>
	 * 
	 * @param callback   returns the current effect
	 */
	public void getCurrentEffectAsync(NanoleafCallback<Effect> callback) {
		getCurrentEffectNameAsync((status, data, device) -> {
			if (status != NanoleafCallback.SUCCESS) {
				callback.onCompleted(status, null, device);
				return;
			}
			try {
				Effect ef = getEffect(data);
				callback.onCompleted(NanoleafCallback.SUCCESS, ef, device);
			}
			catch (NanoleafException e) {
				callback.onCompleted(e.getCode(), null, device);
			}
			catch (Exception e) {
				callback.onCompleted(NanoleafCallback.FAILURE, null, device);
			}
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
	
	/**
	 * <p>Sets the selected effect on the device to the effect specified by
	 * <code>effectName</code>.</p>
	 * 
	 * <p>The callback status will return {@link NanoleafCallback#SUCCESS} on success,
	 * or {@link NanoleafCallback#UNAUTHORIZED} if the access token is invalid, or
	 * {@link NanoleafCallback#NOT_FOUND} if the effect does not exist. If an
	 * internal API error occurs, it will instead return {@link NanoleafCallback#FAILURE}.
	 * The returned data will never be meaningful (either an empty string or null).</p>
	 * 
	 * @param effectName   the name of the effect
	 * @param callback     called when the selected effect is changed or when
	 *                     an error occurs
	 */
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
		List<String> effects = getEffectsList();
		String currentEffect = getCurrentEffectName();
		String effect = currentEffect;
		while (effect.equals(currentEffect)) {
			int i = new Random().nextInt(effects.size());
			effect = effects.get(i);
		}
		setEffect(effect);
	}
	
	/**
	 * <p>Asynchronously sets a random effect based on the effects installed on the
	 * device controller. This includes dynamic as well as Rhythm effects.</p>
	 * 
	 * <p>The callback status will return {@link NanoleafCallback#SUCCESS} on success,
	 * or {@link NanoleafCallback#UNAUTHORIZED} if the access token is invalid. If an
	 * internal API error occurs, it will instead return {@link NanoleafCallback#FAILURE}.
	 * The returned data will never be meaningful (either an empty string or null).</p>
	 * 
	 * @param callback   called when the current effect is changed or when
	 *                   an error occurs
	 */
	public void setRandomEffectAsync(NanoleafCallback<String> callback) {
		getEffectsListAsync((status, effects, device) -> {
			if (status != NanoleafCallback.SUCCESS) {
				callback.onCompleted(status, null, device);
			}
			try {
				String currentEffect = getCurrentEffectName();
				String effect = currentEffect;
				while (effect.equals(currentEffect)) {
					int i = new Random().nextInt(effects.size());
					effect = effects.get(i);
				}
				setEffect(effect);
				callback.onCompleted(NanoleafCallback.SUCCESS, effect, device);
			}
			catch (NanoleafException e) {
				callback.onCompleted(e.getCode(), "", device);
			}
			catch (Exception e) {
				callback.onCompleted(NanoleafCallback.FAILURE, null, device);
			}
		});
	}
	
	/**
	 * Gets a string array of all the effects installed on the device. This
	 * includes static, dynamic, and Rhythm effects.
	 * 
	 * @return                     a string array of all the effects
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public List<String> getEffectsList()
			throws NanoleafException, IOException {
		JSONArray json = new JSONArray(get(getURL("effects/effectsList")));
		List<String> effects = new ArrayList<String>();
		for (int i = 0; i < json.length(); i++)
			effects.add(json.getString(i));
		return effects;
	}
	
	/**
	 * <p>Asynchronously gets a string array of all the effects installed on the device. This
	 * includes static, dynamic, and Rhythm effects.</p>
	 * 
	 * <p>The callback status will return {@link NanoleafCallback#SUCCESS} on success,
	 * or {@link NanoleafCallback#UNAUTHORIZED} if the access token is invalid.If an
	 * internal API error occurs, it will instead return {@link NanoleafCallback#FAILURE}.
	 * The returned data will never be meaningful (either an empty string or null).</p>
	 * 
	 * @param callback   returns the effects list
	 */
	public void getEffectsListAsync(NanoleafCallback<List<String>> callback) {
		getAsync(getURL("effects/effectsList"), (status, data, device) -> {
			if (status != NanoleafCallback.SUCCESS) {
				callback.onCompleted(status, null, device);
			}
			JSONArray json = new JSONArray(data);
			List<String> effects = new ArrayList<String>();
			for (int i = 0; i < json.length(); i++)
				effects.add(json.getString(i));
			callback.onCompleted(status, effects, device);
		});
	}
	
	/**
	 * Gets the effect currently being displayed on the device.
	 * 
	 * @param effectName           the name of the effect
	 * @return                     a new effect based on the current effect
	 * @throws NanoleafException   If the access token is invalid, or the specified effect
	 *                             does not exist on the device
	 * @throws IOException         If an HTTP exception occurs
	 */
	public Effect getEffect(String effectName)
			throws NanoleafException, IOException {
		String body = String.format("{\"write\": {\"command\": \"request\", \"animName\": \"%s\"}}", effectName);
		return Effect.createFromJSON(put(getURL("effects"), body));
	}
	
	/**
	 * <p>Asynchronously gets the effect currently being displayed on the device.</p>
	 * 
	 * <p>The callback status will return {@link NanoleafCallback#SUCCESS} on success,
	 * or {@link NanoleafCallback#UNAUTHORIZED} if the access token is invalid, or
	 * {@link NanoleafCallback#NOT_FOUND} if the effect does not exist. If an
	 * internal API error occurs, it will instead return
	 * {@link NanoleafCallback#FAILURE}. The returned data will never be meaningful
	 * (either an empty string or null).</p>
	 * 
	 * @param effectName   the name of the effect
	 * @param callback     returns the effect
	 */
	public void getEffectAsync(String effectName, NanoleafCallback<Effect> callback) {
		String body = String.format("{\"write\": {\"command\": \"request\", \"animName\": \"%s\"}}", effectName);
		putAsync(getURL("effects"), body, (status, data, device) -> {
			if (status != NanoleafCallback.SUCCESS) {
				callback.onCompleted(status, null, device);
			}
			else {
				callback.onCompleted(status, Effect.createFromJSON(data), device);
			}
		});
	}
	
	/**
	 * Gets an array containing all of the effects installed on the device.
	 * 
	 * @return                     an array of the effects installed on the device
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public List<Effect> getAllEffects()
			throws NanoleafException, IOException {
		JSONObject json = new JSONObject(writeEffect("{\"command\": \"requestAll\"}"));
		JSONArray animations = json.getJSONArray("animations");
		List<Effect> effects = new ArrayList<Effect>();
		for (int i = 0; i < animations.length(); i++) {
			effects.add(Effect.createFromJSON(animations.getJSONObject(i)));
		}
		return effects;
	}
	
	/**
	 * <p>Asynchronously gets an array containing all of the effects installed on
	 * the device.</p>
	 * 
	 * <p>The callback status will return {@link NanoleafCallback#SUCCESS} on success,
	 * or {@link NanoleafCallback#UNAUTHORIZED} if the access token is invalid. If an
	 * internal API error occurs, it will instead return
	 * {@link NanoleafCallback#FAILURE}. The returned data will never be meaningful
	 * (either an empty string or null).</p>
	 * 
	 * @param callback   returns the array of effects
	 */
	public void getAllEffectsAsync(NanoleafCallback<List<Effect>> callback) {
		writeEffectAsync("{\"command\": \"requestAll\"}", (status, data, device) -> {
			if (status != NanoleafCallback.SUCCESS) {
				callback.onCompleted(status, null, device);
			}
			JSONObject json = new JSONObject(data);
			JSONArray animations = json.getJSONArray("animations");
			List<Effect> effects = new ArrayList<Effect>();
			for (int i = 0; i < animations.length(); i++) {
				effects.add(Effect.createFromJSON(animations.getJSONObject(i)));
			}
			callback.onCompleted(status, effects, device);
		});
	}
	
	/**
	 * Uploads and installs an effect to the device. If the effect does not exist
	 * on the device it will be created. If the effect exists it will be overwritten.
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
	
	/**
	 * <p>Asynchronously uploads and installs an effect to the device. If the effect does
	 * not exist on the device it will be created. If the effect exists it will be
	 * overwritten.</p>
	 * 
	 * <p>The callback status will return {@link NanoleafCallback#SUCCESS} on success,
	 * or {@link NanoleafCallback#UNAUTHORIZED} if the access token is invalid, or
	 * {@link NanoleafCallback#UNPROCESSABLE_ENTITY}/{@link NanoleafCallback#BAD_REQUEST}
	 * if the effect is invalid. If an internal API error occurs, it will instead return
	 * {@link NanoleafCallback#FAILURE}. The returned data will never be meaningful
	 * (either an empty string or null).</p>
	 * 
	 * @param effect     the effect to be uploaded
	 * @param callback   called when the effect is added or when an error occurs
	 */
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
	
	/**
	 * <p>Asynchronously deletes an effect from the device.</p>
	 * 
	 * <p>The callback status will return {@link NanoleafCallback#SUCCESS} on success,
	 * or {@link NanoleafCallback#UNAUTHORIZED} if the access token is invalid, or
	 * {@link NanoleafCallback#NOT_FOUND} if the effect does not exist. If an
	 * internal API error occurs, it will instead return
	 * {@link NanoleafCallback#FAILURE}. The returned data will never be meaningful
	 * (either an empty string or null).</p>
	 * 
	 * @param effectName   the name of the effect
	 * @param callback     called when the effect is deleted or when an error occurs
	 */
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
	
	/**
	 * <p>Asynchronously renames an effect on the device.</p>
	 * 
	 * <p>The callback status will return {@link NanoleafCallback#SUCCESS} on success,
	 * or {@link NanoleafCallback#UNAUTHORIZED} if the access token is invalid, or
	 * {@link NanoleafCallback#NOT_FOUND} if the effect does not exist. If an
	 * internal API error occurs, it will instead return
	 * {@link NanoleafCallback#FAILURE}. The returned data will never be meaningful
	 * (either an empty string or null).</p>
	 * 
	 * @param effectName   the name of the effect
	 * @param newName      the new name of the effect
	 * @param callback     called when the effect name is changed or when an error occurs
	 */
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
	
	/**
	 * <p>Asynchronously displays an effect on the device without installing it.</p>
	 * 
	 * <p>The callback status will return {@link NanoleafCallback#SUCCESS} on success,
	 * or {@link NanoleafCallback#UNAUTHORIZED} if the access token is invalid, or
	 * {@link NanoleafCallback#UNPROCESSABLE_ENTITY}/{@link NanoleafCallback#BAD_REQUEST}
	 * if the effect is invalid. If an internal API error occurs, it will instead return
	 * {@link NanoleafCallback#FAILURE}. The returned data will never be meaningful
	 * (either an empty string or null).</p>
	 * 
	 * @param effect     the effect to be displayed
	 * @param callback   called when the effect is displayed
	 */
	public void displayEffectAsync(Effect effect, NanoleafCallback<String> callback) {
		writeEffectAsync(effect.toJSON("display").toString(), callback);
	}
	
	/**
	 * Displays an effect for a given duration on the device without installing it.
	 * 
	 * @param effect               the effect to be previewed
	 * @param duration             the duration for the effect to be displayed
	 * @throws NanoleafException   If the access token is invalid, or the effect
	 *                             parameter is configured incorrectly
	 * @throws IOException         If an HTTP exception occurs
	 */
	public void displayEffectFor(Effect effect, int duration)
			throws NanoleafException, IOException {
		JSONObject body = effect.toJSON("displayTemp");
		body.put("duration", duration);
		writeEffect(body.toString());
	}
	
	/**
	 * <p>Asynchronously displays an effect for a given duration on the device without
	 * installing it.</p>
	 * 
	 * <p>The callback status will return {@link NanoleafCallback#SUCCESS} on success,
	 * or {@link NanoleafCallback#UNAUTHORIZED} if the access token is invalid, or
	 * {@link NanoleafCallback#UNPROCESSABLE_ENTITY}/{@link NanoleafCallback#BAD_REQUEST}
	 * if the effect is invalid. If an internal API error occurs, it will instead return
	 * {@link NanoleafCallback#FAILURE}. The returned data will never be meaningful
	 * (either an empty string or null).</p>
	 * 
	 * @param effect     the effect to be displayed
	 * @param duration   the duration for the effect to be displayed
	 * @param callback   called when the effect is displayed
	 */
	public void displayEffectForAsync(Effect effect, int duration, NanoleafCallback<String> callback) {
		JSONObject body = effect.toJSON("displayTemp");
		body.put("duration", duration);
		writeEffectAsync(body.toString(), callback);
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
	
	/**
	 * <p>Asynchronously displays an effect on the device for a given duration without
	 * installing it.</p>
	 * 
	 * <p>The callback status will return {@link NanoleafCallback#SUCCESS} on success,
	 * or {@link NanoleafCallback#UNAUTHORIZED} if the access token is invalid, or
	 * {@link NanoleafCallback#NOT_FOUND} if the effect does not exist on the device.
	 * If an internal API error occurs, it will instead return
	 * {@link NanoleafCallback#FAILURE}. The returned data will never be meaningful
	 * (either an empty string or null).</p>
	 * 
	 * @param effectName   the name of the effect to be displayed
	 * @param duration     the duration for the effect to be displayed
	 * @param callback     called when the effect is displayed
	 */
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
	 * <p>Asynchronously sets the color of a single panel on the device.</p>
	 * 
	 * <p>The callback status will return {@link NanoleafCallback#SUCCESS} on success,
	 * or {@link NanoleafCallback#UNAUTHORIZED} if the access token is invalid, or
	 * {@link NanoleafCallback#UNPROCESSABLE_ENTITY}/{@link NanoleafCallback#BAD_REQUEST}
	 * if the color is invalid. If an internal API error occurs, it will instead return
	 * {@link NanoleafCallback#FAILURE}. The returned data will never be meaningful
	 * (either an empty string or null).</p>
	 * 
	 * @param panel            the target panel
	 * @param red              the red RGB value
	 * @param green            the green RGB value
	 * @param blue             the blue RGB value
	 * @param transitionTime   the time to transition to this frame from
	 * 						   the previous frame (must be 1 or greater)
	 * @param callback         called when the panel changes color or when
	 *                         an error occurs
	 */
	public void setPanelColorAsync(Panel panel, int red, int green, int blue,
			int transitionTime, NanoleafCallback<String> callback) {
		setPanelColorAsync(panel.getId(), red, green, blue, transitionTime, callback);
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
	 * <p>Asynchronously sets the color of a single panel on the device.</p>
	 * 
	 * <p>The callback status will return {@link NanoleafCallback#SUCCESS} on success,
	 * or {@link NanoleafCallback#UNAUTHORIZED} if the access token is invalid, or
	 * {@link NanoleafCallback#UNPROCESSABLE_ENTITY}/{@link NanoleafCallback#BAD_REQUEST}
	 * if the color is invalid. If an internal API error occurs, it will instead return
	 * {@link NanoleafCallback#FAILURE}. The returned data will never be meaningful
	 * (either an empty string or null).</p>
	 * 
	 * @param panel            the target panel
	 * @param hexColor         the new hex color
	 * @param transitionTime   the time to transition to this frame from
	 * 						   the previous frame (must be 1 or greater)
	 * @param callback         called when the panel color changes or when
	 *                         an error occurs
	 */
	public void setPanelColorAsync(Panel panel, String hexColor, int transitionTime,
			NanoleafCallback<String> callback) {
		java.awt.Color color = java.awt.Color.decode(hexColor);
		setPanelColorAsync(panel, color.getRed(),
				color.getGreen(), color.getBlue(), transitionTime, callback);
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
		CustomEffect custom = createSinglePanelEffect(panelId, red, green, blue, transitionTime);
		displayEffect(custom);
	}
	
	/**
	 * <p>Asynchronously sets the color of a single panel on the device.</p>
	 * 
	 * <p>The callback status will return {@link NanoleafCallback#SUCCESS} on success,
	 * or {@link NanoleafCallback#UNAUTHORIZED} if the access token is invalid, or
	 * {@link NanoleafCallback#UNPROCESSABLE_ENTITY}/{@link NanoleafCallback#BAD_REQUEST}
	 * if the color is invalid. If an internal API error occurs, it will instead return
	 * {@link NanoleafCallback#FAILURE}. The returned data will never be meaningful
	 * (either an empty string or null).</p>
	 * 
	 * @param panelId          the target panel id
	 * @param red              the red RGB value
	 * @param green            the green RGB value
	 * @param blue             the blue RGB value
	 * @param transitionTime   the time to transition to this frame from
	 * 						   the previous frame (must be 1 or greater)
	 * @param callback         called when the panel color changes or when
	 *                         an error occurs
	 */
	public void setPanelColorAsync(int panelId, int red, int green, int blue,
			int transitionTime, NanoleafCallback<String> callback) {
		CustomEffect custom = createSinglePanelEffect(panelId, red, green, blue, transitionTime);
		displayEffectAsync(custom, callback);
	}
	
	private CustomEffect createSinglePanelEffect(int panelId, int red, int green,
			int blue, int transitionTime) {
		CustomEffect custom = new CustomEffect();
		custom.setVersion("2.0");
		custom.setAnimationData(String.format("1 %d 1 %d %d %d 0 %d",
				panelId, red, green, blue, transitionTime));
		custom.setLoopEnabled(false);
		custom.setPalette(new Palette());
		return custom;
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
			throws NanoleafException, IOException {
		java.awt.Color color = java.awt.Color.decode(hexColor);
		setPanelColor(panelId, color.getRed(),
				color.getGreen(), color.getBlue(), transitionTime);
	}
	
	/**
	 * <p>Asynchronously sets the color of a single panel on the device.</p>
	 * 
	 * <p>The callback status will return {@link NanoleafCallback#SUCCESS} on success,
	 * or {@link NanoleafCallback#UNAUTHORIZED} if the access token is invalid, or
	 * {@link NanoleafCallback#UNPROCESSABLE_ENTITY}/{@link NanoleafCallback#BAD_REQUEST}
	 * if the color is invalid. If an internal API error occurs, it will instead return
	 * {@link NanoleafCallback#FAILURE}. The returned data will never be meaningful
	 * (either an empty string or null).</p>
	 * 
	 * @param panelId          the target panel id
	 * @param hexColor         the new hex color
	 * @param transitionTime   the time to transition to this frame from
	 * 						   the previous frame (must be 1 or greater)
	 * @param callback         called when the panel color changes or when
	 *                         an error occurs
	 */
	public void setPanelColorAsync(int panelId, String hexColor,
			int transitionTime, NanoleafCallback<String> callback) {
		java.awt.Color color = java.awt.Color.decode(hexColor);
		setPanelColorAsync(panelId, color.getRed(), color.getGreen(), color.getBlue(),
				transitionTime, callback);
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
			throws NanoleafException, IOException {
		CustomEffect ef = new CustomEffect.Builder(NanoleafDevice.this)
							.addFrameToAllPanels(new Frame(red, green, blue, duration))
							.build(null, false);
		displayEffect(ef);
	}
	
	/**
	 * <p>Asynchronously fades all of the panels to an RGB color over a period of time.</p>
	 * 
	 * <p>The callback status will return {@link NanoleafCallback#SUCCESS} on success,
	 * or {@link NanoleafCallback#UNAUTHORIZED} if the access token is invalid, or
	 * {@link NanoleafCallback#UNPROCESSABLE_ENTITY}/{@link NanoleafCallback#BAD_REQUEST}
	 * if the color/duration are invalid. If an internal API error occurs, it will instead
	 * return {@link NanoleafCallback#FAILURE}. The returned data will never be meaningful
	 * (either an empty string or null).</p>
	 * 
	 * @param red        the red RGB value
	 * @param green      the green RGB value
	 * @param blue       the blue RGB value
	 * @param duration   the fade time, in hertz (10Hz = 1sec)
	 * @param callback   called when the fade starts or when an error occurs
	 */
	public void fadeToColorAsync(int red, int green, int blue, int duration, NanoleafCallback<String> callback) {
		CustomEffect.Builder.createBuilderAsync(this, (status, builder, device) -> {
			if (status != NanoleafCallback.SUCCESS) {
				callback.onCompleted(status, null, device);
			}
			CustomEffect ef = null;
			try {
				ef = builder.addFrameToAllPanels(new Frame(red, green, blue, duration))
							.build(null, false);
				displayEffect(ef);
				callback.onCompleted(NanoleafCallback.SUCCESS, "", device);
			}
			catch (NanoleafException e) {
				callback.onCompleted(e.getCode(), null, device);
			}
			catch (Exception e) {
				callback.onCompleted(NanoleafCallback.FAILURE, null, device);
			}
		});
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
			throws NanoleafException, IOException {
		java.awt.Color color = java.awt.Color.decode(hexColor);
		fadeToColor(color.getRed(), color.getGreen(), color.getBlue(), duration);
	}
	
	/**
	 * <p>Asynchronously fades all of the panels to a hex color over a period of time.</p>
	 * 
	 * <p>The callback status will return {@link NanoleafCallback#SUCCESS} on success,
	 * or {@link NanoleafCallback#UNAUTHORIZED} if the access token is invalid, or
	 * {@link NanoleafCallback#UNPROCESSABLE_ENTITY}/{@link NanoleafCallback#BAD_REQUEST}
	 * if the color/duration are invalid. If an internal API error occurs, it will instead
	 * return {@link NanoleafCallback#FAILURE}. The returned data will never be meaningful
	 * (either an empty string or null).</p>
	 * 
	 * @param hexColor   the new hex color
	 * @param duration   the fade time <i>in hertz (frames per second)</i>
	 * @param callback   called when the fade begins or when an error occurs
	 */
	public void fadeToColorAsync(String hexColor, int duration, NanoleafCallback<String> callback) {
		java.awt.Color color = java.awt.Color.decode(hexColor);
		fadeToColorAsync(color.getRed(), color.getGreen(), color.getBlue(), duration, callback);
	}
	
	/**
	 * Fades all of the panels to a hex color over a period of time.
	 * 
	 * @param color                the new color
	 * @param duration             the fade time <i>in hertz (frames per second)</i>
	 * @throws NanoleafException   If the access token is invalid, or the hex color is
	 *                             invalid, or the duration is negative
	 * @throws IOException         If an HTTP exception occurs
	 */
	public void fadeToColor(Color color, int duration)
			throws NanoleafException, IOException {
		fadeToColor(color.getRed(), color.getGreen(), color.getBlue(), duration);
	}
	
	/**
	 * <p>Asynchronously fades all of the panels to a hex color over a period of time.</p>
	 * 
	 * <p>The callback status will return {@link NanoleafCallback#SUCCESS} on success,
	 * or {@link NanoleafCallback#UNAUTHORIZED} if the access token is invalid, or
	 * {@link NanoleafCallback#UNPROCESSABLE_ENTITY}/{@link NanoleafCallback#BAD_REQUEST}
	 * if the color/duration are invalid. If an internal API error occurs, it will instead
	 * return {@link NanoleafCallback#FAILURE}. The returned data will never be meaningful
	 * (either an empty string or null).</p>
	 * 
	 * @param color      the new color
	 * @param duration   the fade time <i>in hertz (frames per second)</i>
	 * @param callback   called when the fade begins or when an error occurs
	 */
	public void fadeToColorAsync(Color color, int duration, NanoleafCallback<String> callback) {
		fadeToColorAsync(color.getRed(), color.getGreen(), color.getBlue(), duration, callback);
	}
	
	/**
	 * <p>Gets all the plugins/motions from the device.</p>
	 * 
	 * <p><b>Note:</b> This method is slow.</p>
	 * 
	 * @return                     an array of plugins from the device
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public List<PluginMeta> getPlugins()
			throws NanoleafException, IOException {
		String body = String.format("{\"write\": {\"command\": \"requestPlugins\"}}");
		JSONObject json = new JSONObject(put(getURL("effects"), body));
		JSONArray arr = json.getJSONArray("plugins");
		return jsonToPlugins(arr);
	}
	
	/**
	 * <p>Asynchronously gets all the plugins/motions from the device.</p>
	 * 
	 * <p>The callback status will return {@link NanoleafCallback#SUCCESS} on success,
	 * or {@link NanoleafCallback#UNAUTHORIZED} if the access token is invalid.
	 * If an internal API error occurs, it will instead return {@link NanoleafCallback#FAILURE}.</p>
	 * 
	 * @param callback   returns the array of plugins
	 */
	public void getPluginsAsync(NanoleafCallback<List<PluginMeta>> callback) {
		String body = String.format("{\"write\": {\"command\": \"requestPlugins\"}}");
		putAsync(getURL("effects"), body, (status, data, device) -> {
			if (status != NanoleafCallback.SUCCESS) {
				callback.onCompleted(status, null, device);
				return;
			}
			JSONObject json = new JSONObject(data);
			JSONArray arr = json.getJSONArray("plugins");
			callback.onCompleted(status, jsonToPlugins(arr), device);
		});
	}
	
	private List<PluginMeta> jsonToPlugins(JSONArray json) {
		List<PluginMeta> plugins = new ArrayList<PluginMeta>();
		for (int i = 0; i < json.length(); i++) {
			plugins.add(PluginMeta.fromJSON(json.getJSONObject(i)));
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
	
	/**
	 * <p><b>(This method works with JSON data)</b></p>
	 * 
	 * <p>Asynchronously uploads a JSON string to the device. Calls the write effect
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
	 * <p>The callback status will return {@link NanoleafCallback#SUCCESS} on success,
	 * or {@link NanoleafCallback#UNAUTHORIZED} if the access token is invalid, or
	 * {@link NanoleafCallback#UNPROCESSABLE_ENTITY}/{@link NanoleafCallback#BAD_REQUEST}
	 * if the effect is invalid. If an internal API error occurs, it will instead return
	 * {@link NanoleafCallback#FAILURE}. The returned data will never be meaningful
	 * (either an empty string or null).</p>
	 * 
	 * @param command               the operation to perform the write with
	 * @param callback              called when the write request completes or when
	 *                              an error occurs
	 */
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
	public List<Panel> getPanels()
			throws NanoleafException, IOException {
		return parsePanelsJSON(get(getURL("panelLayout/layout")));
	}
	
	/**
	 * <p>Gets an array of the connected panels.</p>
	 * 
	 * <p>This is the ORIGINAL location data.
	 * Since the original location data is not affected by the global orientation,
	 * this data may not accurately represent the panels if displayed as is. For rotated
	 * panel data, use the {@link NanoleafDevice#getPanelsRotated} method instead.</p>
	 * 
	 * <p>The callback status will return {@link NanoleafCallback#SUCCESS} on success,
	 * or {@link NanoleafCallback#UNAUTHORIZED} if the access token is invalid.
	 * If an internal API error occurs, it will instead return
	 * {@link NanoleafCallback#FAILURE}.</p>
	 * 
	 * @param callback   returns the array of panels
	 */
	public void getPanelsAsync(NanoleafCallback<List<Panel>> callback) {
		getAsync(getURL("panelLayout/layout"), (status, data, device) -> {
			if (status != NanoleafCallback.SUCCESS) {
				callback.onCompleted(status, null, device);
			}
			List<Panel> panels = parsePanelsJSON(data);
			callback.onCompleted(status, panels, device);
		});
	}
	
	private List<Panel> parsePanelsJSON(String jsonStr) {
		if (jsonStr == null) {
			return null;
		}
		JSONObject json = new JSONObject(jsonStr);
		JSONArray arr = json.getJSONArray("positionData");
		List<Panel> pd = new ArrayList<Panel>();
		for (int i = 0; i < arr.length(); i++) {
			JSONObject data = arr.getJSONObject(i);
			int panelId = data.getInt("panelId");
			int x = data.getInt("x");
			int y = data.getInt("y");
			int o = data.getInt("o");
			ShapeType s = new ShapeType(data.getInt("shapeType"));
			pd.add(new Panel(panelId, x, y, o, s));
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
	public List<Panel> getPanelsRotated()
			throws NanoleafException, IOException {
		List<Panel> panels = getPanels();
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
	 * Gets a panel by its panel ID.
	 * 
	 * @param id                   the panel id for the panel
	 * @return                     a panel with the same id, or null if no panel is found
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public Panel getPanel(int id)
			throws NanoleafException, IOException {
		return getPanel(id, getPanels());
	}
	
	/**
	 * <p>Asynchronously gets a panel by its panel ID.</p>
	 * 
	 * <p>The callback status will return {@link NanoleafCallback#SUCCESS} on success,
	 * or {@link NanoleafCallback#UNAUTHORIZED} if the access token is invalid.
	 * If an internal API error occurs, it will instead return
	 * {@link NanoleafCallback#FAILURE}.</p>
	 * 
	 * @param id         the panel id for the panel
	 * @param callback   returns the panel
	 */
	public void getPanelAsync(int id, NanoleafCallback<Panel> callback) {
		getPanelsAsync((status, data, device) -> {
			if (status != NanoleafCallback.SUCCESS) {
				callback.onCompleted(status, null, device);
			}
			Panel panel = getPanel(id, data);
			callback.onCompleted(status, panel, device);
		});
	}
	
	private Panel getPanel(int id, List<Panel> panels) {
		for (Panel panel : panels) {
			if (panel.getId() == id) {
				return panel;
			}
		}
		return null;
	}
	
	/**
	 * Gets the global orientation for the device.
	 * 
	 * @return                     the global orientation
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public int getGlobalOrientation()
			throws NanoleafException, IOException {
		return Integer.parseInt(get(getURL("panelLayout/globalOrientation/value")));
	}
	
	/**
	 * <p>Asynchronously gets the global orientation for the device.</p>
	 * 
	 * <p>The callback status will return {@link NanoleafCallback#SUCCESS} on success,
	 * or {@link NanoleafCallback#UNAUTHORIZED} if the access token is invalid.
	 * If an internal API error occurs, it will instead return
	 * {@link NanoleafCallback#FAILURE}.</p>
	 * 
	 * @param callback   returns the global orientation
	 */
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
	
	/**
	 * <p>Asynchronously sets the global orientation for the device.</p>
	 * 
	 * <p>The callback status will return {@link NanoleafCallback#SUCCESS} on success,
	 * or {@link NanoleafCallback#UNAUTHORIZED} if the access token is invalid.
	 * If an internal API error occurs, it will instead return
	 * {@link NanoleafCallback#FAILURE}.</p>
	 * 
	 * @param orientation   the global orientation
	 * @param callback      called when the global orientation is set or when
	 *                      an error occurs
	 */
	public void setGlobalOrientationAsync(int orientation, NanoleafCallback<String> callback) {
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
	
	/**
	 * <p>Asynchronously gets the maximum global orientation for the device.</p>
	 * 
	 * <p>The callback status will return {@link NanoleafCallback#SUCCESS} on success,
	 * or {@link NanoleafCallback#UNAUTHORIZED} if the access token is invalid.
	 * If an internal API error occurs, it will instead return
	 * {@link NanoleafCallback#FAILURE}.</p>
	 * 
	 * @param callback   returns the maximum global orientation
	 */
	public void getMaxGlobalOrientationAsync(NanoleafCallback<Integer> callback) {
		getAsyncInt(getURL("panelLayout/globalOrientation/max"), callback);
	}
	
	/**
	 * Gets the minimum global orientation for the device.
	 * 
	 * @return                     the minimum global orientation
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public int getMinGlobalOrientation()
			throws NanoleafException, IOException {
		return Integer.parseInt(get(getURL("panelLayout/globalOrientation/min")));
	}
	
	/**
	 * <p>Gets the minimum global orientation for the device.</p>
	 * 
	 * <p>The callback status will return {@link NanoleafCallback#SUCCESS} on success,
	 * or {@link NanoleafCallback#UNAUTHORIZED} if the access token is invalid.
	 * If an internal API error occurs, it will instead return
	 * {@link NanoleafCallback#FAILURE}.</p>
	 * 
	 * @param callback   return the minimum global orientation
	 */
	public void getMinGlobalOrientationAsync(NanoleafCallback<Integer> callback) {
		getAsyncInt(getURL("panelLayout/globalOrientation/min"), callback);
	}
	
	private Point getLayoutCentroid(List<Panel> panels) {
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
		externalAddress = new InetSocketAddress(hostname, EXTERNAL_STREAMING_PORT);
	}
	
	/**
	 * <p>Asynchronously enables external streaming mode over UDP.</p>
	 * 
	 * <p>The callback status will return {@link NanoleafCallback#SUCCESS} on success,
	 * or {@link NanoleafCallback#UNAUTHORIZED} if the access token is invalid.
	 * If an internal API error occurs, it will instead return
	 * {@link NanoleafCallback#FAILURE}.</p>
	 * 
	 * @param callback   called when external streaming is enabled or when
	 *                   an error occurs
	 */
	public void enableExternalStreamingAsync(NanoleafCallback<String> callback) {
		String body = "{\"write\": {\"command\": \"display\", \"animType\": \"extControl\", \"extControlVersion\": \"v2\"}}";
		putAsync(getURL("effects"), body, (status, data, device) -> {
			if (status == NanoleafCallback.SUCCESS) { 
				externalAddress = new InetSocketAddress(hostname, EXTERNAL_STREAMING_PORT);
			}
			callback.onCompleted(status, null, device);
		});
	}
	
	/**
	 * <p>Sends a series of frames to the target Aurora.</p>
	 * 
	 * <p><b>Note:</b>Requires external streaming to be enabled. Enable it
	 * using the {@link NanoleafDevice#enableExternalStreaming} method.</p>
	 * 
	 * @param effect               the custom effect to be sent to the Aurora
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an I/O exception occurs
	 * @throws SocketException     If the target device cannot be found or connected to
	 */
	public void sendStaticEffectExternalStreaming(StaticEffect effect)
			throws NanoleafException, IOException {
		sendAnimData(animDataToV2(effect.getAnimationData()));
	}
	
	/**
	 * <p>Asynchronously sends a series of frames to the target Aurora.</p>
	 * 
	 * <p><b>Note:</b>Requires external streaming to be enabled. Enable it
	 * using the {@link NanoleafDevice#enableExternalStreaming} method.</p>
	 * 
	 * <p>The callback status will return {@link NanoleafCallback#SUCCESS} on success,
	 * or {@link NanoleafCallback#UNAUTHORIZED} if the access token is invalid.
	 * If an internal API error occurs, it will instead return
	 * {@link NanoleafCallback#FAILURE}.</p>
	 * 
	 * @param effect     the custom effect to be sent to the Aurora
	 * @param callback   called when the effect is sent or when an error occurs
	 */
	public void sendStaticEffectExternalStreamingAsync(StaticEffect effect,
			NanoleafCallback<String> callback) {
		sendAnimDataAsync(animDataToV2(effect.getAnimationData()), callback);
	}
	
	/**
	 * <p>Sends a static animation data string to the target device.</p>
	 * 
	 * <p><b>Note:</b>Requires external streaming to be enabled. Enable it
	 * using the {@link NanoleafDevice#enableExternalStreaming} method.</p>
	 * 
	 * <p><b>Also Note:</b> It seems that the Nanoleaf Shapes devices have a
	 * limit on how fast they can stream. It seems that about 50ms between
	 * requests is the limit. The Aurora does *not* seem to have this limitation
	 * even on the latest firmware.</p>
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
	 * <p>Sends a static animation data string to the target device.</p>
	 * 
	 * <p><b>Note:</b>Requires external streaming to be enabled. Enable it
	 * using the {@link NanoleafDevice#enableExternalStreaming} method.</p>
	 * 
	 * <p><b>Also Note:</b> It seems that the Nanoleaf Shapes devices have a
	 * limit on how fast they can stream. It seems that about 50ms between
	 * requests is the limit. The Aurora does *not* seem to have this limitation
	 * even on the latest firmware.</p>
	 * 
	 * <p>The callback status will return {@link NanoleafCallback#SUCCESS} on success,
	 * or {@link NanoleafCallback#UNAUTHORIZED} if the access token is invalid.
	 * If an internal API error occurs, it will instead return
	 * {@link NanoleafCallback#FAILURE}.</p>
	 * 
	 * @param animData   the static animation data to be sent to the device
	 * @param callback   called when the data is sent or when an error occurs
	 */
	public void sendAnimDataAsync(String animData, NanoleafCallback<String> callback) {
		new Thread(() -> {
			try {
				sendAnimData(animData);
				if (callback != null) {
					callback.onCompleted(NanoleafCallback.SUCCESS, null, NanoleafDevice.this);
				}
			}
			catch (Exception e) {
				if (callback != null) {
					callback.onCompleted(NanoleafCallback.FAILURE, null, NanoleafDevice.this);
				}
			}
		}).start();
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
	public void setPanelExternalStreaming(int panelId, int red, int green,
			int blue, int transitionTime)
					throws NanoleafException, IOException {
		String frame = String.format("%s %s %d %d %d 0 %s",
				intToBigEndian(1), intToBigEndian(panelId), red, green,
				blue, intToBigEndian(transitionTime));
		sendAnimData(frame);
	}
	
	/**
	 * <p>Asynchronously updates the color of a single panel.</p>
	 * 
	 * <p>The callback status will return {@link NanoleafCallback#SUCCESS} on success,
	 * or {@link NanoleafCallback#UNAUTHORIZED} if the access token is invalid.
	 * If an internal API error occurs, it will instead return
	 * {@link NanoleafCallback#FAILURE}.</p>
	 * 
	 * @param panelId          the id of the panel to update
	 * @param red              the red RGB value
	 * @param green            the green RGB value
	 * @param blue             the blue RGB value
	 * @param transitionTime   the time to transition to this frame from
	 * 						   the previous frame (must be 1 or greater)
	 * @param callback         called when the panel color changes or when
	 *                         an error occurs
	 */
	public void setPanelExternalStreamingAsync(int panelId, int red, int green,
			int blue, int transitionTime, NanoleafCallback<String> callback) {
		String frame = String.format("%s %s %d %d %d 0 %s",
				intToBigEndian(1), intToBigEndian(panelId), red, green,
				blue, intToBigEndian(transitionTime));
		sendAnimDataAsync(frame, callback);
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
	 * <p>Updates the color of a single panel using external streaming.</p>
	 * 
	 * <p>The callback status will return {@link NanoleafCallback#SUCCESS} on success,
	 * or {@link NanoleafCallback#UNAUTHORIZED} if the access token is invalid.
	 * If an internal API error occurs, it will instead return
	 * {@link NanoleafCallback#FAILURE}.</p>
	 * 
	 * @param panel            the panel to update
	 * @param red              the red RGB value
	 * @param green            the green RGB value
	 * @param blue             the blue RGB value
	 * @param transitionTime   the time to transition to this frame from
	 * 						   the previous frame (must be 1 or greater)
	 * @param callback         called when the panel color changes or when
	 *                         an error occurs
	 */
	public void setPanelExternalStreamingAsync(Panel panel, int red, int green, int blue,
			int transitionTime, NanoleafCallback<String> callback) {
		setPanelExternalStreamingAsync(panel.getId(), red, green, blue, transitionTime, callback);
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
	 * <p>Updates the color of a single panel using external streaming.</p>
	 * 
	 * <p>The callback status will return {@link NanoleafCallback#SUCCESS} on success,
	 * or {@link NanoleafCallback#UNAUTHORIZED} if the access token is invalid.
	 * If an internal API error occurs, it will instead return
	 * {@link NanoleafCallback#FAILURE}.</p>
	 * 
	 * @param panelId          the id of the panel to update
	 * @param color            the color object
	 * @param transitionTime   the time to transition to this frame from
	 * 						   the previous frame (must be 1 or greater)
	 * @param callback         called when the panel color changes or when
	 *                         an error occurs
	 */
	public void setPanelExternalStreamingAsync(int panelId, Color color,
			int transitionTime, NanoleafCallback<String> callback) {
		setPanelExternalStreamingAsync(panelId, color.getRed(),
				color.getGreen(), color.getBlue(), transitionTime, callback);
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
	 * <p>Updates the color of a single panel using external streaming.</p>
	 * 
	 * <p>The callback status will return {@link NanoleafCallback#SUCCESS} on success,
	 * or {@link NanoleafCallback#UNAUTHORIZED} if the access token is invalid.
	 * If an internal API error occurs, it will instead return
	 * {@link NanoleafCallback#FAILURE}.</p>
	 * 
	 * @param panel            the panel to update
	 * @param color            the color object
	 * @param transitionTime   the time to transition to this frame from
	 * 						   the previous frame (must be 1 or greater)
	 * @param callback         called when the panel color changes or when
	 *                         an error occurs
	 */
	public void setPanelExternalStreamingAsync(Panel panel, Color color,
			int transitionTime, NanoleafCallback<String> callback) {
		setPanelExternalStreamingAsync(panel.getId(), color.getRed(),
				color.getGreen(), color.getBlue(), transitionTime, callback);
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
	 * <p>Updates the color of a single panel using external streaming.</p>
	 * 
	 * <p>The callback status will return {@link NanoleafCallback#SUCCESS} on success,
	 * or {@link NanoleafCallback#UNAUTHORIZED} if the access token is invalid.
	 * If an internal API error occurs, it will instead return
	 * {@link NanoleafCallback#FAILURE}.</p>
	 * 
	 * @param panelId          the id of the panel to update
	 * @param hexColor         the hex color code
	 * @param transitionTime   the time to transition to this frame from
	 * 						   the previous frame (must be 1 or greater)
	 * @param callback         called when the panel color changes or when
	 *                         an error occurs
	 */
	public void setPanelExternalStreamingAsync(int panelId, String hexColor,
			int transitionTime, NanoleafCallback<String> callback) {
		java.awt.Color color = java.awt.Color.decode(hexColor);
		setPanelExternalStreamingAsync(panelId, color.getRed(),
				color.getGreen(), color.getBlue(), transitionTime, callback);
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
	
	/**
	 * <p>Updates the color of a single panel using external streaming.</p>
	 * 
	 * <p>The callback status will return {@link NanoleafCallback#SUCCESS} on success,
	 * or {@link NanoleafCallback#UNAUTHORIZED} if the access token is invalid.
	 * If an internal API error occurs, it will instead return
	 * {@link NanoleafCallback#FAILURE}.</p>
	 * 
	 * @param panel            the panel to update
	 * @param hexColor         the hex color code
	 * @param transitionTime   the time to transition to this frame from
	 * 						   the previous frame (must be 1 or greater)
	 * @param callback         called when the panel color changes or when
	 *                         an error occurs
	 */
	public void setPanelExternalStreamingAsync(Panel panel, String hexColor,
			int transitionTime, NanoleafCallback<String> callback) {
		java.awt.Color color = java.awt.Color.decode(hexColor);
		setPanelExternalStreamingAsync(panel.getId(), color.getRed(),
				color.getGreen(), color.getBlue(), transitionTime, callback);
	}
	
	private byte[] animDataToBytes(String animData) {
		String[] dataStr = animData.split(" ");
		byte[] dataBytes = new byte[dataStr.length];
		for (int i = 0; i < dataStr.length; i++)
			dataBytes[i] = (byte)Integer.parseInt(dataStr[i]);
		return dataBytes;
	}
	
	private String intToBigEndian(int num) {
		final int BYTE_SIZE = 256;
		int times = Math.floorDiv(num, BYTE_SIZE);
		return String.format("%s %s", times, num-(BYTE_SIZE*times));
	}
	
	// Changes the two-byte fields of an animation data string into big endian
	// so that it can be used for external streaming
	private String animDataToV2(String animData) {
		String[] fields = animData.split(" ");
		StringBuilder data = new StringBuilder();
		int numPanels = Integer.parseInt(fields[0]);
		data.append(intToBigEndian(numPanels));
		for (int i = 0; i < numPanels; i++) {
			String panelid = intToBigEndian(Integer.parseInt(fields[i*7+1]));
			int r = Integer.parseInt(fields[i*7+3]);
			int g = Integer.parseInt(fields[i*7+4]);
			int b = Integer.parseInt(fields[i*7+5]);
			String transition = intToBigEndian(Integer.parseInt(fields[i*7+7]));
			data.append(String.format(" %s %d %d %d 0 %s", panelid, r, g, b, transition));
		}
		return data.toString();
	}
	
	/**
	 * <p>Registers an event listener for one or more types of events.</p>
	 * 
	 * <p><b>Note:</b> the touch events produced by this listener have a very high
	 * latency of about 1 to 2 seconds. For very low latency touch events, you
	 * can instead use the {@link NanoleafDevice#registerTouchEventStreamingListener(NanoleafTouchEventListener)}
	 * method for near-realtime latency.</p>
	 * 
	 * @param listener        a listener to listen for events
	 * @param stateEvents     listens for changes to the state of the device
	 * @param layoutEvents    listens for changes to the layout of the device panels
	 * @param effectsEvents   listens for changes to the device effects or selected effect
	 * @param touchEvents     listens for touch events such as "tap", "double-tap", and "swipe"
	 * @return                an SSE object
	 */
	public ServerSentEvent registerEventListener(NanoleafEventListener listener,
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
	
	/**
	 * Enables touch event streaming on a specific port. This allows for very
	 * low latency and detailed touch events. This method should only be
	 * called once.
	 * 
	 * @param port               the port to listen for events on
	 * @throws SocketException   If a socket exception occurs
	 */
	public void enableTouchEventStreaming(int port) throws SocketException {
		if (touchEventListener == null) {
			touchEventListener = new UDPTouchEventListener(port);
			touchEventThread = new Thread(touchEventListener);
			touchEventThread.start();
			touchEventStreamingPort = port;
		}
	}
	
	/**
	 * <p>Registers an event listener for very low latency and detailed touch events.</p>
	 * 
	 * <p><b>Note:</b> the method {@link NanoleafDevice#enableTouchEventStreaming(int)}
	 * MUST be called before any touch event listeners are registered.</p>
	 * 
	 * @param listener
	 */
	public void registerTouchEventStreamingListener(NanoleafTouchEventListener listener) {
		String url = getURL("events?id=4");
		Request req = new Request.Builder()
				.url(url)
				.addHeader("TouchEventsPort", touchEventStreamingPort + "")
				.get()
				.build();
		OkSse okSse = new OkSse(client);
		ServerSentEvent s = okSse.newServerSentEvent(req, new NanoleafEventListener() {
			// Dummy listener
			public void onOpen(){}
			public void onClosed(){}
			public void onEvent(Event[] events){}
		});
		sse.add(s);
		touchEventListener.addListener(listener);
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
	
//	/**
//	 * Gets an array of schedules stored on the device.
//	 * 
//	 * @return  an array of schedules
//	 */
//	public Schedule[] getSchedules()
//			throws NanoleafException, IOException {
//		JSONObject obj = new JSONObject(get(getURL("schedules")));
//		JSONArray arr = obj.getJSONArray("schedules");
//		Schedule[] schedules = new Schedule[arr.length()];
//		for (int i = 0; i < arr.length(); i++) {
//			schedules[i] = Schedule.fromJSON(
//					arr.getJSONObject(i).toString());
//		}
//		return schedules;
//	}
//	
//	/**
//	 * Uploads an array of schedules to the device.
//	 * 
//	 * @param schedules            an array of schedules
//	 * @throws NanoleafException   If the access token is invalid, or one or more
//	 *                             given schedules are configured incorrectly
//	 * @throws IOException         If an HTTP exception occurs
//	 */
//	public void addSchedules(Schedule[] schedules)
//			throws NanoleafException, IOException {
//		String schedulesStr = "\"schedules\":[";
//		for (int i = 0; i < schedules.length; i++) {
//			schedulesStr += schedules[i];
//			if (i < schedules.length-1) {
//				schedulesStr += ",";
//			}
//			else {
//				schedulesStr += "]";
//			}
//		}
//		String body = String.format("{\"write\":{\"command\":" +
//				"\"addSchedules\",%s}}", schedulesStr);
//		put(getURL("effects"), body);
//	}
//	
//	/**
//	 * Uploads a schedule to the device.
//	 * 
//	 * @param schedule             the schedule to upload
//	 * @throws NanoleafException   If the access token is invalid, or one or more
//	 *                             given schedules are configured incorrectly
//	 * @throws IOException         If an HTTP exception occurs
//	 */
//	public void addSchedule(Schedule schedule)
//			throws NanoleafException, IOException {
//		addSchedules(new Schedule[]{schedule});
//	}
//	
//	/**
//	 * Deletes an array of schedules from the device.
//	 * 
//	 * @param schedules            an array of schedules be deleted
//	 * @throws NanoleafException   If the access token is invalid, or one or more
//	 *                             given schedules do not exist on the device
//	 * @throws IOException         If an HTTP exception occurs
//	 */
//	public void removeSchedules(Schedule[] schedules)
//			throws NanoleafException, IOException {
//		int[] ids = new int[schedules.length];
//		for (int i = 0; i < schedules.length; i++) {
//			ids[i] = schedules[i].getId();
//		}
//		removeSchedulesById(ids);
//	}
//	
//	/**
//	 * Deletes a schedule from the device.
//	 * 
//	 * @param schedule             the schedule to delete
//	 * @throws NanoleafException   If the access token is invalid, or the given
//	 *                             schedule does not exist on the device
//	 * @throws IOException         If an HTTP exception occurs
//	 */
//	public void removeSchedule(Schedule schedule)
//			throws NanoleafException, IOException {
//		removeSchedules(new Schedule[]{schedule});
//	}
//	
//	/**
//	 * Deletes an array of schedules from the device using their unique schedule IDs.
//	 * 
//	 * @param scheduleIds          an array of schedule IDs to be delete
//	 * @throws NanoleafException   If the access token is invalid, or the device
//	 *                             does not contain one or more of the schedule IDs
//	 * @throws IOException         If an HTTP exception occurs
//	 */
//	public void removeSchedulesById(int[] scheduleIds)
//			throws NanoleafException, IOException {
//		String schedulesStr = "\"schedules\":[";
//		for (int i = 0; i < scheduleIds.length; i++) {
//			schedulesStr += String.format("{\"id\":%d}",
//					scheduleIds[i]);
//			if (i < scheduleIds.length-1) {
//				schedulesStr += ",";
//			}
//			else {
//				schedulesStr += "]";
//			}
//		}
//		String body = String.format("{\"write\":{\"command\":" +
//				"\"removeSchedules\",%s}}", schedulesStr);
//		put(getURL("effects"), body);
//	}
//	
//	/**
//	 * Deletes a schedule from the device using its unique schedule ID.
//	 * 
//	 * @param scheduleId           the schedule ID of the schedule to be deleted
//	 * @throws NanoleafException   If the access token is invalid, or the device
//	 *                             does not contain the schedule ID
//	 * @throws IOException         If an HTTP exception occurs
//	 */
//	public void removeScheduleById(int scheduleId)
//			throws NanoleafException, IOException {
//		removeSchedulesById(new int[]{scheduleId});
//	}
	
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
					if (code == NanoleafCallback.OK || code == NanoleafCallback.NO_CONTENT) {
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
					if (code == NanoleafCallback.OK || code == NanoleafCallback.NO_CONTENT) {
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
					if (code == NanoleafCallback.OK || code == NanoleafCallback.NO_CONTENT) {
						code = NanoleafCallback.SUCCESS;
					}
					callback.onCompleted(code, response.body().string(), NanoleafDevice.this);
				}
			}
		});
	}
	
	@Override
	public String toString() {
		return String.format("%s (%s:%d)", name, hostname, port);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null || obj.getClass() != this.getClass()) {
			return false;
		}
		NanoleafDevice other = (NanoleafDevice)obj;
		return this.hostname.equals(other.hostname) && this.port == other.port &&
				this.name.equals(other.name) && this.serialNumber.equals(other.serialNumber) &&
				this.manufacturer.equals(other.manufacturer) && this.model.equals(other.model);
	}
}
