package com.github.rowak.nanoleafapi;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.json.JSONObject;

import okhttp3.OkHttpClient;

/**
 * The Aurora class is one of the three main concrete classes for creating a NanoleafDevice.
 * This class should only be used to connect to physical Aurora devices. For other devices
 * such as the Canvas or Hexagons, use the Canvas and Shapes classes, respectively.
 */
public class Aurora extends NanoleafDevice {
	
	/**
	 * Creates a new instance of the Aurora.
	 * 
	 * @param hostname             the hostname of the Aurora
	 * @param port                 the port of the controller (default=16021)
	 * @param accessToken          a unique authentication token
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public Aurora(String hostname, int port, String accessToken)
			throws NanoleafException, IOException {
		super(hostname, port, accessToken);
	}
	
	/**
	 * Creates a new instance of the Aurora.
	 * 
	 * @param hostname             the hostname of the Aurora
	 * @param accessToken          a unique authentication token
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public Aurora(String hostname, String accessToken)
			throws NanoleafException, IOException {
		super(hostname, DEFAULT_PORT, accessToken);
	}
	
	/**
	 * Asynchronously creates a new instance of the Aurora.
	 * 
	 * @param hostname             the hostname of the Aurora
	 * @param port                 the port of the Aurora (default=16021)
	 * @param accessToken          a unique authentication token
	 * @param callback             the callback that is called when the device has
	 *                             been initialized
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public Aurora(String hostname, int port, String accessToken, NanoleafCallback<Aurora> callback) {
		super(hostname, port, accessToken, callback);
	}
	
	/**
	 * Asynchronously creates a new instance of the Aurora.
	 * 
	 * @param hostname             the hostname of the Aurora
	 * @param accessToken          a unique authentication token
	 * @param callback             the callback that is called when the device has
	 *                             been initialized
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public Aurora(String hostname, String accessToken, NanoleafCallback<Aurora> callback) {
		super(hostname, DEFAULT_PORT, accessToken, callback);
	}
	
	protected Aurora(String hostname, int port, String accessToken, OkHttpClient client)
			throws NanoleafException, IOException {
		super(hostname, port, accessToken, client);
	}
	
	/**
	 * Indicates if the Rhythm is connected to the Aurora or not.
	 * @return  true, if the Rhythm is connected
	 * @throws UnauthorizedException  if the access token is invalid
	 */
	public boolean isRhythmConnected()
			throws NanoleafException, IOException {
		return Boolean.parseBoolean(get(getURL("rhythm/rhythmConnected")));
	}
	
	public void isRhythmConnectedAsync(NanoleafCallback<Boolean> callback)
			throws NanoleafException, IOException {
		getAsyncBool(getURL("rhythm/rhythmConnected"), callback);
	}
	
	/**
	 * Indicates if the Rhythm's microphone is currently active or not.
	 * @return  true, if the Rhythm is active
	 * @throws UnauthorizedException  if the access token is invalid
	 */
	public boolean isRhythmMicActive()
			throws NanoleafException, IOException {
		return Boolean.parseBoolean(get(getURL("rhythm/rhythmActive")));
	}
	
	public void isRhythmMicActiveAsync(NanoleafCallback<Boolean> callback) {
		getAsyncBool(getURL("rhythm/rhythmActive"), callback);
	}
	
	/**
	 * Indicates the Rhythm's Id in the Light Panel system.
	 * @return  the Rhythm's Id as an integer
	 * @throws UnauthorizedException  if the access token is invalid
	 */
	public int getRhythmId()
			throws NanoleafException, IOException {
		return Integer.parseInt(get(getURL("rhythm/rhythmId")));
	}
	
	public void getRhythmIdAsync(NanoleafCallback<Integer> callback) {
		getAsyncInt(getURL("rhythm/rhythmId"), callback);
	}
	
	/**
	 * Indicates the Rhythm's hardware version.
	 * @return  the Rhythm's hardware version
	 * @throws UnauthorizedException  if the access token is invalid
	 */
	public String getRhythmHardwareVersion()
			throws NanoleafException, IOException {
		return get(getURL("rhythm/hardwareVersion"));
	}
	
	public void getRhythmHardwareVersionAsync(NanoleafCallback<String> callback) {
		getAsync(getURL("rhythm/hardwareVersion"), callback);
	}
	
	/**
	 * Indicates the Rhythm's firmware version.
	 * @return  the Rhythm's firmware version
	 * @throws UnauthorizedException  if the access token is invalid
	 */
	public String getRhythmFirmwareVersion()
			throws NanoleafException, IOException {
		return get(getURL("rhythm/firmwareVersion"));
	}
	
	public void getRhythmFirmwareVersionAsync(NanoleafCallback<String> callback) {
		getAsync(getURL("rhythm/firmwareVersion"), callback);
	}
	
	/**
	 * Indicates if an aux cable (3.5mm) is currently connected to the Rhythm.
	 * @return  true, if an aux cable is connected
	 * @throws UnauthorizedException  if the access token is invalid
	 */
	public boolean isRhythmAuxAvailable()
			throws NanoleafException, IOException {
		return Boolean.parseBoolean(get(getURL("rhythm/auxAvailable")));
	}
	
	public void isRhythmAuxAvailableAsync(NanoleafCallback<Boolean> callback) {
		getAsyncBool(getURL("rhythm/auxAvailable"), callback);
	}
	
	/**
	 * Allows the user to control the sound source for the Rhythm.
	 * Mode 0 is the microphone. Mode 1 is the aux (3.5mm) cable.
	 * @return  the Rhythm mode (0 or 1)
	 * @throws UnauthorizedException  if the access token is invalid
	 */
	public int getRhythmMode()
			throws NanoleafException, IOException {
		return Integer.parseInt(get(getURL("rhythm/rhythmMode")));
	}
	
	public void getRhythmModeAsync(NanoleafCallback<String> callback) {
		getAsync(getURL("rhythm/rhythmMode"), callback);
	}
	
	/**
	 * Writing 0 to this field sets the Rhythm's sound source to the microphone,
	 * and writing 1 to the field sets the sound source to the aux cable.
	 * @param mode  the Rhythm mode (0 or 1 only)
	 * @return  (204 No Content, 401 Unauthorized, 422 Unprocessable Entity)
	 * @throws UnauthorizedException  if the access token is invalid
	 * @throws UnprocessableEntityException  if <code>mode</code> is not either 0 or 1
	 */
	public void setRhythmMode(int mode)
			throws NanoleafException, IOException {
		String body = String.format("{\"rhythmMode\": %d}", mode);
		put(getURL("rhythm"), body);
	}
	
	public void setRhythmModeAsync(int mode, NanoleafCallback<String> callback) {
		String body = String.format("{\"rhythmMode\": %d}", mode);
		putAsync(getURL("rhythm"), body, callback);
	}
	
	/**
	 * Indicates the position and orientation of the Rhythm in the Light Panels' layout.
	 * @return  the <code>Position</code> of the Rhythm
	 * @throws UnauthorizedException  if the access token is invalid
	 */
	public Panel getRhythmPanel()
			throws NanoleafException, IOException {
		return parseRhythmPanelJSON(get(getURL("rhythm")));
	}
	
	public void getRhythmPanelAsync(NanoleafCallback<Panel> callback) {
		getAsync(getURL("rhythm"), (status, data, device) -> {
			Panel rhythmPanel = parseRhythmPanelJSON(data);
			callback.onCompleted(status, rhythmPanel, device);
		});
	}
	
	private Panel parseRhythmPanelJSON(String jsonStr) {
		JSONObject json = new JSONObject(jsonStr);
		JSONObject pos = json.getJSONObject("rhythmPos");
		int id = json.getInt("rhythmId");
		int x = pos.getInt("x");
		int y = pos.getInt("y");
		int o = pos.getInt("o");
		return new Panel(id, x, y, o, ShapeType.rhythm());
	}
	
	/**
	 * Enables external streaming mode over UDP.
	 * @throws UnauthorizedException  if the access token is invalid
	 */
	public void enableExternalStreaming()
			throws NanoleafException, IOException {
		String body = "{\"write\": {\"command\": \"display\", \"animType\": \"extControl\"}}";
		JSONObject response = new JSONObject(put(getURL("effects"), body));
		String host = response.getString("streamControlIpAddr");
		int port = response.getInt("streamControlPort");
		externalAddress = new InetSocketAddress(host, port);
	}
	
	public void enableExternalStreamingAsync(NanoleafCallback<String> callback)
			throws NanoleafException, IOException {
		String body = "{\"write\": {\"command\": \"display\", \"animType\": \"extControl\"}}";
		putAsync(getURL("effects"), body, (status, data, device) -> {
			JSONObject response = new JSONObject(data);
			String host = response.getString("streamControlIpAddr");
			int port = response.getInt("streamControlPort");
			externalAddress = new InetSocketAddress(host, port);
			callback.onCompleted(status, null, device);
		});
	}
	
	@Override
	public void setPanelExternalStreaming(int panelId, int red, int green, int blue, int transitionTime)
			throws NanoleafException, IOException {
		String frame = String.format("1 %d 1 %d %d %d 0 %d",
				panelId, red, green, blue, transitionTime);
		sendAnimData(frame);
	}
}
