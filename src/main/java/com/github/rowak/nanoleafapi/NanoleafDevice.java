package com.github.rowak.nanoleafapi;

import org.json.JSONArray;
import org.json.JSONObject;

import com.github.rowak.nanoleafapi.event.NanoleafEventListener;
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

public abstract class NanoleafDevice {
	
	public static final String API_LEVEL = "v1";
	public static final int DEFAULT_PORT = 16021;
	
	/** Internal HTTP client for communications with the Nanoleaf device **/
	private OkHttpClient client;
	
	/** Internal SSE clients record (for resource cleanup) **/
	private List<ServerSentEvent> sse;
	
	private String hostname, accessToken;
	private int port;
	
	/** This information is very unlikely to change, so it is cached **/
	private String name;
	private String serialNumber;
	private String manufacturer;
	private String firmwareVersion;
	private String model;
	
	/**
	 * The address of the aurora controller <i>for streaming mode only</i>.
	 */
	protected InetSocketAddress externalAddress;
	
	/**
	 * A generic creation method for instantiating a NanoleafDevice object, without requiring
	 * prior knowledge of the device *type*.
	 * @param hostname  			the hostname of the controller
	 * @param port  				the port of the controller (default=16021)
	 * @param accessToken  			a unique authentication token
	 * @return  					a new nanoleaf device
	 * @throws NanoleafException  	if the access token is invalid
	 * @throws IOException			if an HTTP exception occurs
	 */
	public static NanoleafDevice createDevice(String hostname, int port, String accessToken)
			throws NanoleafException, IOException {
		OkHttpClient client = new OkHttpClient.Builder().readTimeout(0, TimeUnit.SECONDS).build();
		Response resp = HttpUtil.getHttpSync(client, getURL("", hostname, port, accessToken));
		NanoleafException.checkStatusCode(resp.code());
		JSONObject controllerInfo = new JSONObject(resp.body().string());
		if (controllerInfo.has("name")) {
			String name = controllerInfo.getString("name").toLowerCase();
			if (name.contains("aurora") || name.contains("light panels")) {
				return new Aurora(hostname, port, accessToken);
			}
			else if (name.contains("canvas")) {
				return new Canvas(hostname, port, accessToken);
			}
			else if (name.contains("shapes")) {
				return new Shapes(hostname, port, accessToken);
			}
		}
		return null;
	}
	
	/**
	 * Creates a new instance of the controller.
	 * @param hostname  the hostname of the controller
	 * @param port  the port of the controller (default=16021)
	 * @param accessToken  a unique authentication token
	 * @throws NanoleafException  if the access token is invalid
	 * @throws IOException
	 * @throws InterruptedException
	 */
	protected NanoleafDevice(String hostname, int port, String accessToken)
			throws NanoleafException, IOException {
		init(hostname, port, accessToken);
	}
	
	protected NanoleafDevice(String hostname, int port, String accessToken, NanoleafCallback<String> callback) {
		initAsync(hostname, port, accessToken, callback);
	}
	
	/**
	 * Creates a new instance of the controller.
	 * @param hostname  the hostname of the controller
	 * @param accessToken  a unique authentication token
	 * @throws NanoleafException  if the access token is invalid
	 * @throws IOException
	 * @throws InterruptedException
	 */
	protected NanoleafDevice(String hostname, String accessToken)
			throws NanoleafException, IOException {
		init(hostname, DEFAULT_PORT, accessToken);
	}
	
	protected NanoleafDevice(String hostname, String accessToken, NanoleafCallback<String> callback) {
		initAsync(hostname, DEFAULT_PORT, accessToken, callback);
	}
	
	/**
	 * Initialize the NanoleafDevice object and gather initial data.
	 * @param hostname  the hostname of the controller
	 * @param port  the port of the controller (default=16021)
	 * @param apiLevel  the current version of the OpenAPI
	 * 					(for example: "v1" or "beta")
	 * @param accessToken  a unique authentication token
	 * @throws NanoleafException  if the access token is invalid
	 * @throws HttpRequestException  if the connection times out
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private void init(String hostname, int port, String accessToken)
			throws NanoleafException, IOException {
		this.hostname = hostname;
		this.port = port;
		this.accessToken = accessToken;
		
		client = new OkHttpClient.Builder().readTimeout(0, TimeUnit.SECONDS).build();
		String body = get(getURL(""));
		JSONObject controllerInfo = new JSONObject(body);
		this.name = controllerInfo.getString("name");
		this.serialNumber = controllerInfo.getString("serialNo");
		this.manufacturer = controllerInfo.getString("manufacturer");
		this.firmwareVersion = controllerInfo.getString("firmwareVersion");
		this.model = controllerInfo.getString("model");
		
		sse = new ArrayList<ServerSentEvent>();
	}
	
	private void initAsync(String hostname, int port, String accessToken, NanoleafCallback<String> callback) {
		client = new OkHttpClient.Builder().readTimeout(0, TimeUnit.SECONDS).build();
		sse = new ArrayList<ServerSentEvent>();
		getAsync(getURL(""), (status, data, device) -> {
			JSONObject controllerInfo = new JSONObject(data);
			this.name = controllerInfo.getString("name");
			this.serialNumber = controllerInfo.getString("serialNo");
			this.manufacturer = controllerInfo.getString("manufacturer");
			this.firmwareVersion = controllerInfo.getString("firmwareVersion");
			this.model = controllerInfo.getString("model");
			callback.onCompleted(status, data, device);
		});
	}
	
	/**
	 * Force a shutdown of the internal HTTP client.
	 * <b>Note:</b> This method only has effect if asynchronous calls have
	 * been made. The HTTP client will eventually shut down on its own, but
	 * the application will likely hang until it does (unless this method
	 * is called).
	 */
	public void closeAsync() {
		client.dispatcher().executorService().shutdown();
	}
	
	/**
	 * Closes all event listeners.
	 */
	public void closeEventListener() {
		for (ServerSentEvent s : sse) {
			s.close();
		}
	}
	
	/**
	 * Returns the device's host name (IP address).
	 * @return  the host name for this device
	 */
	public String getHostname() {
		return this.hostname;
	}
	
	/**
	 * Returns the port that the device is running on.
	 * @return  the port for this device
	 */
	public int getPort() {
		return this.port;
	}
	
	/**
	 * Returns the access token generated by the user for this device.
	 * @return  the access token for this device
	 */
	public String getAccessToken() {
		return this.accessToken;
	}
	
	/**
	 * Returns the unique name of the device controller.
	 * 
	 * Note that this method does not communicate with the device, since
	 * the name is retrieved at creation time and is almost always unwritable.
	 * 
	 * @return  the name of the device controller
	 */
	public String getName() {
		return this.name;
	}
	
	/**
	 * Returns the unique serial number of the device.
	 * 
	 * Note that this method does not communicate with the device, since
	 * the serial number is retrieved at creation time and is almost always
	 * unwritable.
	 * 
	 * @return  the serial number of the device
	 */
	public String getSerialNumber() {
		return this.serialNumber;
	}
	
	/**
	 * Returns the name of the manufacturer of the device.
	 * 
	 * Note that this method does not communicate with the device, since
	 * the manufacturer name is retrieved at creation time and is almost
	 * always unwritable.
	 * 
	 * @return  the name of the device manufacturer
	 */
	public String getManufacturer() {
		return this.manufacturer;
	}
	
	/**
	 * Returns the firmware version of the device.
	 * @return  the firmware version
	 */
	public String getFirmwareVersion()
			throws NanoleafException, IOException {
		String body = get(getURL(""));
		JSONObject controllerInfo = new JSONObject(body);
		return controllerInfo.getString("firmwareVersion");
	}
	
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
	 * Returns the model of the device.
	 * 
	 * Note that this method does not communicate with the device, since
	 * the model number is retrieved at creation time and is almost always
	 * unwritable.
	 * 
	 * @return  the model of the device
	 */
	public String getModel() {
		return this.model;
	}
	
	/**
	 * Causes the panels to flash in unison.
	 * This is typically used to help users differentiate between multiple panels.
	 * @return  (204 No Content, 401 Unauthorized)
	 * @throws UnauthorizedException  if the access token is invalid
	 */
	public void identify()
			throws NanoleafException, IOException {
		put(getURL("identify"), null);
	}
	
	public void identifyAsync(NanoleafCallback<String> callback) {
		putAsync(getURL("identify"), null, callback);
	}
	
	/**
	 * Gets the on state of the Aurora (true = on, false = off).
	 * @return true, if the Aurora is on
	 * @throws UnauthorizedException  if the access token is invalid
	 */
	public boolean getOn()
			throws NanoleafException, IOException {
		return Boolean.parseBoolean(get(getURL("state/on/value")));
	}
	
	public void getOnAsync(NanoleafCallback<String> callback) {
		getAsync(getURL("state/on/value"), callback);
	}
	
	/**
	 * Sets the on state of the Aurora (true = on, false = off).
	 * @param on  whether the Aurora should be turned on or off
	 * @return  (200 OK, 401 Unauthorized, 422 Unprocessable Entity)
	 * @throws UnauthorizedException  if the access token is invalid
	 */
	public void setOn(boolean on)
			throws NanoleafException, IOException {
		String body = String.format("{\"on\": {\"value\": %b}}", on);
		put(getURL("state"), body);
	}
	
	public void setOnAsync(boolean on, NanoleafCallback<String> callback) {
		String body = String.format("{\"on\": {\"value\": %b}}", on);
		putAsync(getURL("state"), body, callback);
	}
	
	/**
	 * Toggles the on state of the Aurora (on = off, off = on).
	 * @return  (200 OK, 401 Unauthorized)
	 * @throws UnauthorizedException  if the access token is invalid
	 */
	public void toggleOn()
			throws NanoleafException, IOException {
		setOn(!this.getOn());
	}
	
	public void toggleOnAsync(NanoleafCallback<String> callback)
			throws NanoleafException, IOException {
		setOnAsync(!this.getOn(), callback);
	}
	
	/**
	 * Gets the master brightness of the Aurora.
	 * @return  the brightness of the Aurora
	 * @throws UnauthorizedException  if the access token is invalid
	 */
	public int getBrightness()
			throws NanoleafException, IOException {
		return Integer.parseInt(get(getURL("state/brightness/value")));
	}
	
	public void getBrightnessAsync(NanoleafCallback<Integer> callback) {
		getAsyncInt(getURL("state/brightness/value"), callback);
	}
	
	/**
	 * Sets the master brightness of the Aurora.
	 * @param brightness  the new brightness level as a percent
	 * @return  (204 No Content, 401 Unauthorized, 422 Unprocessable Entity)
	 * @throws UnauthorizedException  if the access token is invalid
	 * @throws UnprocessableEntityException  if <code>brightness</code> is not within the
	 * 								 		 maximum (100) and minimum (0) restrictions
	 */
	public void setBrightness(int brightness)
			throws NanoleafException, IOException {
		String body = String.format("{\"brightness\": {\"value\": %d}}", brightness);
		put(getURL("state"), body);
	}
	
	public void setBrightnessAsync(int brightness, NanoleafCallback<String> callback) {
		String body = String.format("{\"brightness\": {\"value\": %d}}", brightness);
		putAsync(getURL("state"), body, callback);
	}
	
	/**
	 * Fades the master brightness of the Aurora over a perdiod of time.
	 * @param brightness  the new brightness level as a percent
	 * @param duration  the fade time <i>in seconds</i>
	 * @return  (204 No Content, 401 Unauthorized, 422 Unprocessable Entity)
	 * @throws UnauthorizedException  if the access token is invalid
	 * @throws UnprocessableEntityException  if <code>brightness</code> is not within the
	 * 								 		 maximum (100) and minimum (0) restrictions
	 */
	public void fadeToBrightness(int brightness, int duration)
			throws NanoleafException, IOException {
		String body = String.format("{\"brightness\": {\"value\": %d, \"duration\": %d}}",
				brightness, duration);
		put(getURL("state"), body);
	}
	
	public void fadeToBrightnessAsync(int brightness, int duration, NanoleafCallback<String> callback) {
		String body = String.format("{\"brightness\": {\"value\": %d, \"duration\": %d}}",
				brightness, duration);
		putAsync(getURL("state"), body, callback);
	}
	
	/**
	 * Increases the brightness by an amount as a percent.
	 * @param amount  the amount to increase by
	 * @return  (204 No Content, 401 Unauthorized, 422 Unprocessable Entity)
	 * @throws UnauthorizedException  if the access token is invalid
	 */
	public void increaseBrightness(int amount)
			throws NanoleafException, IOException {
		String body = String.format("{\"brightness\": {\"increment\": %d}}", amount);
		put(getURL("state"), body);
	}
	
	public void increaseBrightnessAsync(int amount, NanoleafCallback<String> callback) {
		String body = String.format("{\"brightness\": {\"increment\": %d}}", amount);
		putAsync(getURL("state"), body, callback);
	}
	
	/**
	 * Decreases the brightness by an amount as a percent.
	 * @param amount  the amount to decrease by
	 * @return  (204 No Content, 401 Unauthorized, 422 Unprocessable Entity)
	 * @throws UnauthorizedException  if the access token is invalid
	 */
	public void decreaseBrightness(int amount)
			throws NanoleafException, IOException {
		increaseBrightness(-amount);
	}
	
	public void decreaseBrightnessAsync(int amount, NanoleafCallback<String> callback) {
		increaseBrightnessAsync(-amount, callback);
	}
	
	/**
	 * Gets the maximum brightness of the Aurora.
	 * @return  the maximum brightness
	 * @throws UnauthorizedException  if the access token is invalid
	 */
	public int getMaxBrightness()
			throws NanoleafException, IOException {
		return Integer.parseInt(get(getURL("state/brightness/max")));
	}
	
	public void getMaxBrightnessAsync(NanoleafCallback<Integer> callback) {
		getAsyncInt(getURL("state/brightness/max"), callback);
	}
	
	/**
	 * Gets the minimum brightness of the Aurora.
	 * @return  the minimum brightness
	 * @throws UnauthorizedException  if the access token is invalid
	 */
	public int getMinBrightness()
			throws NanoleafException, IOException {
		return Integer.parseInt(get(getURL("state/brightness/min")));
	}
	
	public void getMinBrightnessAsync(NanoleafCallback<Integer> callback) {
		getAsyncInt(getURL("state/brightness/min"), callback);
	}
	
	/**
	 * Gets the hue of the Aurora (static/custom effects only).
	 * @return  the hue of the Aurora
	 * @throws UnauthorizedException  if the access token is invalid
	 */
	public int getHue()
			throws NanoleafException, IOException {
		return Integer.parseInt(get(getURL("state/hue/value")));
	}
	
	public void getHueAsync(NanoleafCallback<Integer> callback) {
		getAsyncInt(getURL("state/hue/value"), callback);
	}
	
	/**
	 * Sets the hue of the Aurora (static/custom effects only).
	 * @param hue  the new hue
	 * @return  (204 No Content, 401 Unauthorized, 422 Unprocessable Entity)
	 * @throws UnauthorizedException  if the access token is invalid
	 * @throws UnprocessableEntityException  if <code>hue</code> is not within the
	 * 										 maximum and minimum restrictions
	 */
	public void setHue(int hue)
			throws NanoleafException, IOException {
		String body = String.format("{\"hue\": {\"value\": %d}}", hue);
		put(getURL("state"), body);
	}
	
	public void increaseHue(int amount)
			throws NanoleafException, IOException {
		String body = String.format("{\"hue\": {\"increment\": %d}}", amount);
		put(getURL("state"), body);
	}
	
	public void decreaseHue(int amount)
			throws NanoleafException, IOException {
		increaseHue(-amount);
	}
	
	/**
	 * Gets the maximum hue of the Aurora.
	 * @return  the maximum hue
	 * @throws UnauthorizedException  if the access token is invalid
	 */
	public int getMaxHue()
			throws NanoleafException, IOException {
		return Integer.parseInt(get(getURL("state/hue/max")));
	}
	
	public void getMaxHueAsync(NanoleafCallback<Integer> callback) {
		getAsyncInt(getURL("state/hue/max"), callback);
	}
	
	/**
	 * Gets the minimum hue of the Aurora.
	 * @return  the minimum hue
	 * @throws UnauthorizedException  if the access token is invalid
	 */
	public int getMinHue()
			throws NanoleafException, IOException {
		return Integer.parseInt(get(getURL("state/hue/min")));
	}
	
	public void getMinHueAsync(NanoleafCallback<Integer> callback) {
		getAsyncInt(getURL("state/hue/min"), callback);
	}
	
	/**
	 * Gets the saturation of the Aurora (static/custom effects only).
	 * @return  tue saturation of the Aurora
	 * @throws UnauthorizedException  if the access token is invalid
	 */
	public int getSaturation()
			throws NanoleafException, IOException {
		return Integer.parseInt(get(getURL("state/sat/value")));
	}
	
	public void getSaturationAsync(NanoleafCallback<Integer> callback) {
		getAsyncInt(getURL("state/sat/value"), callback);
	}
	
	/**
	 * Sets the saturation of the Aurora (static/custom effects only).
	 * @param saturation  the new saturation
	 * @return  (204 No Content, 401 Unauthorized, 422 Unprocessable Entity)
	 * @throws UnauthorizedException  if the access token is invalid
	 * @throws UnprocessableEntityException  if <code>saturation</code> is not within
	 * 										 the maximum and minimum restrictions
	 */
	public void setSaturation(int saturation)
			throws NanoleafException, IOException {
		String body = String.format("{\"sat\": {\"value\": %d}}", saturation);
		put(getURL("state"), body);
	}
	
	public void setSaturationAsync(int saturation, NanoleafCallback<String> callback) {
		String body = String.format("{\"sat\": {\"value\": %d}}", saturation);
		putAsync(getURL("state/hue/min"), body, callback);
	}
	
	public void increaseSaturation(int amount)
			throws NanoleafException, IOException {
		String body = String.format("{\"sat\": {\"increment\": %d}}", amount);
		put(getURL("state"), body);
	}
	
	public void increaseSaturationAsync(int amount, NanoleafCallback<String> callback) {
		String body = String.format("{\"sat\": {\"increment\": %d}}", amount);
		putAsync(getURL("state"), body, callback);
	}
	
	public void decreaseSaturation(int amount)
			throws NanoleafException, IOException {
		increaseSaturation(-amount);
	}
	
	public void decreaseSaturationAsync(int amount, NanoleafCallback<String> callback) {
		increaseSaturationAsync(-amount, callback);
	}
	
	/**
	 * Gets the maximum saturation of the Aurora.
	 * @return  the maximum saturation
	 * @throws UnauthorizedException  if the access token is invalid
	 */
	public int getMaxSaturation()
			throws NanoleafException, IOException {
		return Integer.parseInt(get(getURL("state/sat/max")));
	}
	
	public void getMaxSaturationAsync(NanoleafCallback<Integer> callback) {
		getAsyncInt(getURL("state/sat/max"), callback);
	}
	
	/**
	 * Gets the minimum saturation of the Aurora.
	 * @return  the minimum saturation
	 * @throws UnauthorizedException  if the access token is invalid
	 */
	public int getMinSaturation()
			throws NanoleafException, IOException {
		return Integer.parseInt(get(getURL("state/sat/min")));
	}
	
	public void getMinSaturationAsync(NanoleafCallback<Integer> callback) {
		getAsyncInt(getURL("state/sat/min"), callback);
	}
	
	/**
	 * Gets the color temperature of the Aurora (color temperature effect only).
	 * @return  the color temperature of the Aurora
	 * @throws UnauthorizedException  if the access token is invalid
	 */
	public int getColorTemperature()
			throws NanoleafException, IOException {
		return Integer.parseInt(get(getURL("state/ct/value")));
	}
	
	public void getColorTemperatureAsync(NanoleafCallback<Integer> callback) {
		getAsyncInt(getURL("state/ct/value"), callback);
	}
	
	/**
	 * Sets the color temperature of the Aurora in Kelvin.
	 * @param colorTemperature  color temperature in Kelvin
	 * @return  (204 No Content, 401 Unauthorized, 422 Unprocessable Entity)
	 * @throws UnauthorizedException  if the access token is invalid
	 * @throws UnprocessableEntityException  if <code>colorTemperature</code> is not
	 * 										 within the maximum and minimum values
	 */
	public void setColorTemperature(int colorTemperature)
			throws NanoleafException, IOException {
		String body = String.format("{\"ct\": {\"value\": %d}}", colorTemperature);
		put(getURL("state"), body);
	}
	
	public void setColorTemperatureAsync(int colorTemperature, NanoleafCallback<String> callback) {
		String body = String.format("{\"ct\": {\"value\": %d}}", colorTemperature);
		putAsync(getURL("state"), body, callback);
	}
	
	public void increaseColorTemperature(int amount)
			throws NanoleafException, IOException {
		String body = String.format("{\"ct\": {\"increment\": %d}}", amount);
		put(getURL("state"), body);
	}
	
	public void increaseColorTemperatureAsync(int amount, NanoleafCallback<String> callback) {
		String body = String.format("{\"ct\": {\"increment\": %d}}", amount);
		putAsync(getURL("state"), body, callback);
	}
	
	public void decreaseColorTemperature(int amount)
			throws NanoleafException, IOException {
		increaseColorTemperature(-amount);
	}
	
	public void decreaseColorTemperatureAsync(int amount, NanoleafCallback<String> callback) {
		increaseColorTemperatureAsync(-amount, callback);
	}
	
	/**
	 * Gets the maximum color temperature of the Aurora.
	 * @return  the maximum color temperature
	 * @throws UnauthorizedException  if the access token is invalid
	 */
	public int getMaxColorTemperature()
			throws NanoleafException, IOException {
		return Integer.parseInt(get(getURL("state/ct/max")));
	}
	
	public void getMaxColorTemperatureAsync(NanoleafCallback<Integer> callback) {
		getAsyncInt(getURL("state/ct/max"), callback);
	}
	
	/**
	 * Gets the minimum color temperature of the Aurora.
	 * @return  the minimum color temperature
	 * @throws UnauthorizedException  if the access token is invalid
	 */
	public int getMinColorTemperature()
			throws NanoleafException, IOException {
		return Integer.parseInt(get(getURL("state/ct/min")));
	}
	
	public void getMinColorTemperature(NanoleafCallback<Integer> callback) {
		getAsyncInt(getURL("state/ct/min"), callback);
	}
	
	/**
	 * Gets the color mode of the Aurora.
	 * @return  the color mode
	 * @throws UnauthorizedException  if the access token is invalid
	 */
	public String getColorMode()
			throws NanoleafException, IOException {
		return get(getURL("state/colorMode")).replace("\"", "");
	}
	
	public void getColorModeAsync(NanoleafCallback<String> callback) {
		getAsync(getURL("state/colorMode"), (status, data, device) -> {
			callback.onCompleted(status, data.replace("\"", ""), device);
		});
	}
	
	/**
	 * Gets the current color (HSB/RGB) of the Aurora.<br>
	 * <b>Note: This only works if the Aurora is displaying a solid color.</b>
	 * @return  the color of the Aurora
	 * @throws UnauthorizedException  if the access token is invalid
	 */
	public Color getColor()
			throws NanoleafException, IOException {
		return Color.fromHSB(getHue(), getSaturation(), getBrightness());
	}
	
	/**
	 * Sets the color (HSB/RGB) of the Aurora.
	 * @param color  the new color
	 * @throws UnauthorizedException  if the access token is invalid
	 */
	public void setColor(Color color)
			throws NanoleafException, IOException {
		setHue(color.getHue());
		setSaturation(color.getSaturation());
		setBrightness(color.getBrightness());
	}
	
	/**
	 * Gets the name of the currently selected effect on the Aurora controller.
	 * @return  the name of the effect
	 * @throws UnauthorizedException  if the access token is invalid
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
	 * @return  the effect object
	 * @throws UnauthorizedException  if the access token is invalid
	 * @throws ResourceNotFoundException  if the effect does not exist
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
	 * Sets the selected effect on the Aurora to the effect
	 * specified by <code>effectName</code>.
	 * @param effectName  the name of the effect
	 * @return  (200 OK, 204 No Content, 401 Unauthorized,
	 * 			404 Resource Not Found, 422 Unprocessable Entity)
	 * @throws UnauthorizedException  if the access token is invalid
	 * @throws ResourceNotFoundException  if the effect <code>effectName</code>
	 * 									  is not present on the Aurora controller
	 * @throws UnprocessableEntityException  if <code>effectName</code> is malformed
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
	 * Aurora controller. This includes dynamic as well as Rhythm effects.
	 * @return  (200 OK, 204 No Content, 401 Unauthorized, 404 Resource Not Found)
	 * @throws UnauthorizedException  if the access token is invalid
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
	 * Gets a string array of all the effects installed on the Aurora controller.
	 * This includes static, dynamic, and Rhythm effects.
	 * @return  a string array of all the effects
	 * @throws UnauthorizedException  if the access token is invalid
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
	 * Creates an <code>Effect</code> object from the <code>JSON</code> data
	 * for the effect <code>effectName</code>.
	 * @param effectName  the name of the effect
	 * @return  a new <code>Effect</code> object based on the effect <code>effectName</code>
	 * @throws UnauthorizedException  if the access token is invalid
	 * @throws ResourceNotFoundException  if the effect <code>effectName</code>
	 * 									  is not present on the Aurora controller
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
	 * Gets an array of type <code>Effect</code> containing all of
	 * the effects installed on the Aurora controller.
	 * @return  an array of the effects installed on the Aurora controller
	 * @throws UnauthorizedException   if the access token is invalid
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
	 * Uploads and installs the local effect <code>effect</code> to the Aurora controller.
	 * If the effect does not exist on the Aurora it will be created. If the effect exists
	 * it will be overwritten.
	 * @param effect  the effect to be uploaded
	 * @return  (200 OK, 204 No Content,
	 * 			401 Unauthorized, 422 Unprocessable Entity)
	 * @throws UnauthorizedException  if the access token is invalid
	 * @throws UnprocessableEntityException  if <code>effect</code> contains an
	 * 										 invalid number of instance variables, or has
	 * 										 one or more invalid instance variables (causing
	 * 										 the <code>JSON</code> output to be invalid)
	 */
	public void addEffect(Effect effect)
			throws NanoleafException, IOException {
		writeEffect(effect.toJSON("add").toString());
	}
	
	public void addEffectAsync(Effect effect, NanoleafCallback<String> callback) {
		writeEffectAsync(effect.toJSON("add").toString(), callback);
	}
	
	/**
	 * Deletes an effect from the Aurora controller.
	 * @param effectName  the name of the effect
	 * @return  (200 OK, 204 No Content, 401 Unauthorized,
	 * 				404 Resource Not Found, 422 Unprocessable Entity)
	 * @throws UnauthorizedException  if the access token is invalid
	 * @throws ResourceNotFoundException  if the effect <code>effectName</code>
	 * 									  is not present on the Aurora controller
	 * @throws UnprocessableEntityException  if <code>effectName</code> is malformed
	 */
	public void deleteEffect(String effectName)
			throws NanoleafException, IOException {
		writeEffect(String.format("{\"command\": \"delete\", \"animName\": \"%s\"}", effectName));
	}
	
	public void deleteEffectAsync(String effectName, NanoleafCallback<String> callback) {
		writeEffectAsync(String.format("{\"command\": \"delete\", \"animName\": \"%s\"}", effectName), callback);
	}
	
	/**
	 * Renames an effect on the Aurora controller.
	 * @param effectName  the name of the effect
	 * @param newName  the new name of the effect
	 * @return  (200 OK, 204 No Content, 401 Unauthorized,
	 * 			404 Resource Not Found, 422 Unprocessable Entity)
	 * @throws UnauthorizedException  if the access token is invalid
	 * @throws ResourceNotFoundException  if the effect <code>effectName</code>
	 * 									  is not present on the Aurora controller
	 * @throws UnprocessableEntityException  if <code>effectName</code> or
	 * 										 <code>newName</code> are malformed
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
	 * Uploads and previews the local effect <code>effect</code> on
	 * the Aurora controller without installing it.
	 * @param effect  the effect to be previewed
	 * @return  (200 OK, 204 No Content,
	 * 			401 Unauthorized, 422 Unprocessable Entity)
	 * @throws UnauthorizedException  if the access token is invalid
	 * @throws UnprocessableEntityException  if <code>effect</code> contains an
	 * 										 invalid number of instance variables, or has
	 * 										 one or more invalid instance variables (causing
	 * 										 the <code>JSON</code> output to be invalid)
	 */
	public void displayEffect(Effect effect)
			throws NanoleafException, IOException {
		writeEffect(effect.toJSON("display").toString());
	}
	
	public void displayEffectAsync(Effect effect, NanoleafCallback<String> callback) {
		writeEffectAsync(effect.toJSON("display").toString(), callback);
	}
	
	/**
	 * Uploads and previews the local effect <code>effect</code> on
	 * the Aurora controller for a given duration without installing it.
	 * @param effectName   the name of the effect to be previewed
	 * @param duration  the duration for the effect to be displayed
	 * @return  (200 OK, 204 No Content,
	 * 			401 Unauthorized, 404 Resource Not Found)
	 * @throws UnauthorizedException  if the access token is invalid
	 * @throws ResourceNotFoundException  if the effect <code>effectName</code>
	 * 									  is not found on the Aurora controller
	 */
	public void displayEffectFor(String effectName, int duration)
			throws NanoleafException, IOException {
		writeEffect(String.format("{\"command\": \"displayTemp\", \"duration\": %d, \"animName\": \"%s\"}", duration, effectName));
	}
	
	public void displayEffectForAsync(String effectName, int duration, NanoleafCallback<String> callback) {
		writeEffectAsync(String.format("{\"command\": \"displayTemp\", \"duration\": %d, \"animName\": \"%s\"}", duration, effectName), callback);
	}
	
	/**
	 * Sets the color of a single panel on the Aurora.
	 * @param panel  the target panel
	 * @param red  the red RGB value
	 * @param green  the green RGB value
	 * @param blue  the blue RGB value
	 * @param transitionTime  the time to transition to this frame from
	 * 						  the previous frame (must be 1 or greater)
	 * @return  (200 OK, 204 No Content,
	 * 			401 Unauthorized, 422 UnprocessableEntityException)
	 * @throws UnauthorizedException  if the access token is invalid
	 * @throws UnprocessableEntityException  if the <code>panel</code> is not found on the Aurora
	 * 										 or if the <code>red</code>, <code>green</code>,
	 * 										 or <code>blue</code> values are invalid (must be
	 * 										 0 &#60; x &#60; 255)
	 */
	public void setPanelColor(Panel panel, int red, int green, int blue, int transitionTime)
			throws NanoleafException, IOException {
		setPanelColor(panel.getId(), red, green, blue, transitionTime);
	}
	
	/**
	 * Sets the color of a single panel on the Aurora.
	 * @param panel  the target panel
	 * @param hexColor  the new hex color
	 * @param transitionTime  the time to transition to this frame from
	 * 						  the previous frame (must be 1 or greater)
	 * @return  (200 OK, 204 No Content,
	 * 			401 Unauthorized, 422 UnprocessableEntityException)
	 * @throws UnauthorizedException  if the access token is invalid
	 * @throws UnprocessableEntityException  if the <code>panel</code> is not found on the Aurora
	 * 										 or if the <code>red</code>, <code>green</code>,
	 * 										 or <code>blue</code> values are invalid (must be
	 * 										 0 &#60; x &#60; 255)
	 */
	public void setPanelColor(Panel panel, String hexColor, int transitionTime)
			throws NanoleafException, IOException {
		java.awt.Color color = java.awt.Color.decode(hexColor);
		setPanelColor(panel, color.getRed(),
				color.getGreen(), color.getBlue(), transitionTime);
	}
	
	/**
	 * Sets the color of a single panel on the Aurora.
	 * @param panelId  the target panel id
	 * @param red  the red RGB value
	 * @param green  the green RGB value
	 * @param blue  the blue RGB value
	 * @param transitionTime  the time to transition to this frame from
	 * 						  the previous frame (must be 1 or greater)
	 * @return  (200 OK, 204 No Content,
	 * 			401 Unauthorized, 422 UnprocessableEntityException)
	 * @throws UnauthorizedException  if the access token is invalid
	 * @throws UnprocessableEntityException  if the <code>panelId</code> is not valid
	 * 										 or if the <code>red</code>, <code>green</code>,
	 * 										 or <code>blue</code> values are invalid (must be
	 * 										 0 &#60; x &#60; 255)
	 */
	public void setPanelColor(int panelId, int red, int green, int blue, int transitionTime)
			throws NanoleafException, IOException {
		CustomEffect custom = new CustomEffect();
		custom.setVersion("1.0"); // ?
		custom.setAnimationData("1 " + panelId + " 1 " +
				red + " " + green + " " + blue + " 0 " + transitionTime);
		custom.setLoop(false);
		displayEffect(custom);
	}
//	
//	/**
//	 * Sets the color of a single panel on the Aurora.
//	 * @param panelId  the target panel id
//	 * @param hexColor  the new hex color
//	 * @param transitionTime  the time to transition to this frame from
//	 * 						  the previous frame (must be 1 or greater)
//	 * @return  (200 OK, 204 No Content,
//	 * 			401 Unauthorized, 422 UnprocessableEntityException)
//	 * @throws UnauthorizedException  if the access token is invalid
//	 * @throws UnprocessableEntityException  if the <code>panelId</code> is not valid
//	 * 										 or if the <code>red</code>, <code>green</code>,
//	 * 										 or <code>blue</code> values are invalid (must be
//	 * 										 0 &#60; x &#60; 255)
//	 */
//	public void setPanelColor(int panelId, String hexColor, int transitionTime)
//			throws NanoleafException, IOException, InterruptedException {
//		java.awt.Color color = java.awt.Color.decode(hexColor);
//		setPanelColor(panelId, color.getRed(),
//				color.getGreen(), color.getBlue(), transitionTime);
//	}
//	
//	/**
//	 * Fades all of the panels to an RGB color over a perdiod of time.
//	 * @param red  the red RGB value
//	 * @param green  the green RGB value
//	 * @param blue  the blue RGB value
//	 * @param duration  the fade time <i>in hertz (10Hz = 1sec)</i>
//	 * @return  (200 OK, 204 No Content,
//	 * 			401 Unauthorized, 422 UnprocessableEntityException)
//	 * @throws UnauthorizedException  if the access token is invalid
//	 * @throws UnprocessableEntityException  if the RGB values are outside of
//	 * 										 the range 0-255 or if the duration
//	 * 										 is negative
//	 */
//	public void fadeToColor(int red, int green, int blue, int duration)
//			throws NanoleafException, IOException, InterruptedException {
//		CustomEffectBuilder ceb = new CustomEffectBuilder(Aurora.this);
//		ceb.addFrameToAllPanels(new Frame(red, green, blue, 0, duration));
//		displayEffect(ceb.build("", false));
//	}
//	
//	/**
//	 * Fades all of the panels to a hex color over a perdiod of time.
//	 * @param hexColor the new hex color
//	 * @param duration  the fade time <i>in hertz (frames per second)</i>
//	 * @return  (200 OK, 204 No Content,
//	 * 			401 Unauthorized, 422 UnprocessableEntityException)
//	 * @throws UnauthorizedException  if the access token is invalid
//	 * @throws UnprocessableEntityException  if the hex color is invalid
//	 * 										 or if the duration is negative
//	 */
//	public void fadeToColor(String hexColor, int duration)
//			throws NanoleafException, IOException, InterruptedException {
//		java.awt.Color color = java.awt.Color.decode(hexColor);
//		fadeToColor(color.getRed(),
//				color.getGreen(), color.getBlue(), duration);
//	}
//	
	/**
	 * Gets <i>all</i> the plugins/motions from the Aurora.
	 * <br><b>Note: This method is slow.</b>
	 * @return  an array of plugins from the Aurora
	 * @throws UnauthorizedException  if the access token is invalid
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
	 * <b>(This method works with JSON data)</b><br>
	 * Uploads a <code>JSON</code> string to the Aurora controller.<br>
	 * Calls the <code>write</code> effect command from the 
	 * <a href = "http://forum.nanoleaf.me/docs/openapi#write">OpenAPI</a>. Refer to it
	 * for more information about the commands.
	 * <h1>Commands:</h1>
	 * - add  -  Installs an effect on the Aurora controller or updates
	 * 			 the effect if it already exists.<br>
	 * - delete  -  Permanently removes an effect from the Aurora controller.
	 * - request  -  Requests a single effect by name.<br>
	 * - requestAll  -  Requests all the installed effects from the Aurora controller.
	 * 					Note: this takes a long time, but returns a <code>JSON</code> string.
	 * 					If efficiency is important, use the {@link #getAllEffects()} method.<br>
	 * - display  -  Sets a color mode on the Aurora (used for previewing effects).<br>
	 * - displayTemp  -  Temporarily sets a color mode on the Aurora (typically used for
	 * 					 notifications of visual indicators).<br>
	 * - rename  -  Changes the name of an effect on the Aurora controller.<br><br>
	 * @param command  the operation to perform the write with
	 * @return  (200 OK, 204 No Content,
	 * 			401 Unauthorized, 422 Unprocessable Entity)
	 * @throws UnauthorizedException  if the access token is invalid
	 * @throws UnprocessableEntityException  if <code>command</code> is malformed
	 * 										 or contains invalid effect options
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
	 * Gets the number of panels connected to the Aurora controller.
	 * @param includeRhythm  whether or not to include the Rhythm as a panel
	 * 		   (inluded by default in the OpenAPI)
	 * @return  the number of panels
	 * @throws UnauthorizedException  if the access token is invalid
	 */
	public int getNumPanels(boolean includeRhythm)
			throws NanoleafException, IOException {
		int numPanels = Integer.parseInt(get(getURL("panelLayout/layout/numPanels")));
		if (!includeRhythm)
			numPanels--;
		return numPanels;
	}
	
	public void getNumPanelsAsync(boolean includeRhythm, NanoleafCallback<Integer> callback)
			throws NanoleafException, IOException {
		getAsyncInt(getURL("panelLayout/layout/numPanels"), (status2, data2, device) -> {
			if (!includeRhythm) {
				data2--;
			}
			callback.onCompleted(status2, data2, device);
		});
	}
	
	/**
	 * Gets the side length of each panel connected to the Aurora.
	 * @return  the side length of each panel
	 * @throws UnauthorizedException  if the access token is invalid
	 */
	public int getSideLength()
			throws NanoleafException, IOException {
		return Integer.parseInt(get(getURL("panelLayout/layout/sideLength")));
	}
	
	public void getSideLengthAsync(NanoleafCallback<Integer> callback) {
		getAsyncInt(getURL("panelLayout/layout/sideLength"), callback);
	}
	
	/**
	 * Gets an array of the connected panels.
	 * 
	 * This is the ORIGINAL location data.
	 * Since the original location data is not affected by the global orientation,
	 * this data may not accurately represent the panels if displayed as is. For rotated
	 * panel data, use the {@link NanoleafDevice#getPanelsRotated} method instead.
	 * @return  an array of panels
	 * @throws UnauthorizedException  if the access token is invalid
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
	 * @return  an array of rotated panels
	 * @throws UnauthorizedException  if the access token is invalid
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
	 * Finds a <code>Panel</code> object if you have a panel
	 * id but not the panel object.
	 * @param id  the panel id for the panel
	 * @return  a <code>Panel</code> with the same id, or null if no panel is found
	 * @throws UnauthorizedException  if the access token is invalid
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
	 * Gets the global orientation for the Aurora.
	 * @return  the global orientation
	 * @throws UnauthorizedException  if the access token is invalid
	 */
	public int getGlobalOrientation()
			throws NanoleafException, IOException {
		return Integer.parseInt(get(getURL("panelLayout/globalOrientation/value")));
	}
	
	public void getGlobalOrientationAsync(NanoleafCallback<Integer> callback) {
		getAsyncInt(getURL("panelLayout/globalOrientation/value"), callback);
	}
	
	/**
	 * Sets the global orientation for the Aurora.
	 * @param orientation  the global orientation
	 * @return  (204 No Content, 401 Unauthorized)
	 * @throws UnauthorizedException  if the access token is invalid
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
	 * Gets the maximum global orientation for the Aurora.
	 * @return  the maximum global orientation
	 * @throws UnauthorizedException  if the access token is invalid
	 */
	public int getMaxGlobalOrientation()
			throws NanoleafException, IOException {
		return Integer.parseInt(get(getURL("panelLayout/globalOrientation/max")));
	}
	
	public void getMaxGlobalOrientationAsync(NanoleafCallback<Integer> callback) {
		getAsyncInt(getURL("panelLayout/globalOrientation/max"), callback);
	}
	
	/**
	 * Gets the minimum global orientation for the Aurora.
	 * @return  the mimum global orientation
	 * @throws UnauthorizedException  if the access token is invalid
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
	 * Gets the <code>SocketAddress</code> containing
	 * the host name and port of the external streaming controller.
	 * @return the <code>SocketAddress</code> of the streaming controller
	 */
	public InetSocketAddress getExternalStreamingAddress() {
		return externalAddress;
	}
	
	/**
	 * Enables external streaming mode over UDP.
	 * @throws UnauthorizedException  if the access token is invalid
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
	
//	/**
//	 * Sends a series of frames to the target Aurora.
//	 * <b>Requires external streaming to be enabled. Enable it
//	 * using the {@link #enable()} method.</b>
//	 * @param effect  the custom effect to be sent to the Aurora
//	 * @throws UnauthorizedException  if the access token is invalid
//	 * @throws SocketException  if the target Aurora cannot be found or connected to
//	 * @throws IOException  if an I/O error occurs
//	 */
//	public void sendStaticEffect(Effect effect)
//			throws NanoleafException, IOException
//	{
//		sendAnimData(effect.getAnimData());
//	}
	
	/**
	 * Sends a static animation data string to the target Aurora.<br>
	 * <b>Note: Requires external streaming to be enabled. Enable it
	 * using the {@link #enable()} method.</b>
	 * @param animData  the static animation data to be sent to the Aurora
	 * @throws UnauthorizedException  if the access token is invalid
	 * @throws SocketException  if the target Aurora cannot be found or connected to
	 * @throws IOException  if an I/O error occurs
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
	 * @param panelId  the id of the panel to update
	 * @param red  the red RGB value
	 * @param green  the green RGB value
	 * @param blue  the blue RGB value
	 * @param transitionTime  the time to transition to this frame from
	 * 						  the previous frame (must be 1 or greater)
	 * @throws UnauthorizedException  if the access token is invalid
	 * @throws SocketException  if the target Aurora cannot be found or connected to
	 * @throws IOException  if an I/O error occurs
	 */
	public void setPanelExternalStreaming(int panelId, int red, int green, int blue, int transitionTime)
			throws NanoleafException, IOException {
		String frame = String.format("%s %s %d %d %d 0 %s",
				intToBigEndian(1), intToBigEndian(panelId), red, green,
				blue, intToBigEndian(transitionTime));
		sendAnimData(frame);
	}
	
	/**
	 * Updates the color of a single panel.
	 * @param panel  the panel to update
	 * @param red  the red RGB value
	 * @param green  the green RGB value
	 * @param blue  the blue RGB value
	 * @param transitionTime  the time to transition to this frame from
	 * 						  the previous frame (must be 1 or greater)
	 * @throws UnauthorizedException  if the access token is invalid
	 * @throws SocketException  if the target Aurora cannot be found or connected to
	 * @throws IOException  if an I/O error occurs
	 */
	public void setPanelExternalStreaming(Panel panel, int red, int green, int blue, int transitionTime)
			throws NanoleafException, IOException {
		setPanelExternalStreaming(panel.getId(), red, green, blue, transitionTime);
	}
	
	/**
	 * Updates the color of a single panel.
	 * @param panelId  the id of the panel to update
	 * @param color  the color object
	 * @param transitionTime  the time to transition to this frame from
	 * 						  the previous frame (must be 1 or greater)
	 * @throws UnauthorizedException  if the access token is invalid
	 * @throws SocketException  if the target Aurora cannot be found or connected to
	 * @throws IOException  if an I/O error occurs
	 */
	public void setPanelExternalStreaming(int panelId, Color color, int transitionTime)
			throws NanoleafException, IOException {
		setPanelExternalStreaming(panelId, color.getRed(),
				color.getGreen(), color.getBlue(), transitionTime);
	}
	
	/**
	 * Updates the color of a single panel.
	 * @param panel  the panel to update
	 * @param color  the color object
	 * @param transitionTime  the time to transition to this frame from
	 * 						  the previous frame (must be 1 or greater)
	 * @throws UnauthorizedException  if the access token is invalid
	 * @throws SocketException  if the target Aurora cannot be found or connected to
	 * @throws IOException  if an I/O error occurs
	 */
	public void setPanelExternalStreaming(Panel panel, Color color, int transitionTime)
			throws NanoleafException, IOException {
		setPanelExternalStreaming(panel.getId(), color.getRed(),
				color.getGreen(), color.getBlue(), transitionTime);
	}
	
	/**
	 * Updates the color of a single panel.
	 * @param panelId  the id of the panel to update
	 * @param hexColor  the hex color code
	 * @param transitionTime  the time to transition to this frame from
	 * 						  the previous frame (must be 1 or greater)
	 * @throws UnauthorizedException  if the access token is invalid
	 * @throws SocketException  if the target Aurora cannot be found or connected to
	 * @throws IOException  if an I/O error occurs
	 */
	public void setPanelExternalStreaming(int panelId, String hexColor, int transitionTime)
			throws NanoleafException, IOException {
		java.awt.Color color = java.awt.Color.decode(hexColor);
		setPanelExternalStreaming(panelId, color.getRed(),
				color.getGreen(), color.getBlue(), transitionTime);
	}
	
	/**
	 * Updates the color of a single panel.
	 * @param panel  the panel to update
	 * @param hexColor  the hex color code
	 * @param transitionTime  the time to transition to this frame from
	 * 						  the previous frame (must be 1 or greater)
	 * @throws UnauthorizedException  if the access token is invalid
	 * @throws SocketException  if the target Aurora cannot be found or connected to
	 * @throws IOException  if an I/O error occurs
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
	 * Constructs a full URI to make an API call.
	 * @param endpoint  the final location in the API call (used to navigate <code>JSON</code>)
	 * @return  a completed URI (ready to be sent)
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
