package io.github.rowak.nanoleafapi;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;

import org.json.JSONObject;

import java.util.List;
import java.util.ArrayList;

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
	
	public List<Panel> getNeighborPanels(Panel panel, List<Panel> panels) {
		// The centroid distance is the (roughly) the distance between the
		// centroids of two neighboring panels
		final int CENTROID_DISTANCE = 90; // normally: 84
		List<Panel> neighbors = new ArrayList<Panel>();
		int p1x = panel.getX();
		int p1y = panel.getY();
		for (Panel p2 : panels) {
			if (!panel.equals(p2)) {
				int p2x = p2.getX();
				int p2y = p2.getY();
				if (Math.floor(Math.sqrt(Math.pow((p1x - p2x), 2) +
						Math.pow((p1y - p2y), 2))) <= CENTROID_DISTANCE) {
					neighbors.add(p2);
				}
			}
		}
		return neighbors;
	}
	
	/**
	 * Gets the shape of the Aurora panels. <b>This will always return
	 * a shape type with type {@link ShapeType#TRIANGLE_AURORA}.</b>
	 * 
	 * @return   an aurora triangle shape type
	 */
	@Override
	public ShapeType getShapeType() {
		return ShapeType.triangleAurora();
	}
	
	/**
	 * Gets the shape of the Aurora controller. <b>This will always return
	 * <code>null</code>, since the Aurora controller doesn't have a
	 * shape type mapping.</b>
	 * 
	 * @return   null
	 */
	@Override
	public ShapeType getControllerShapeType() {
		return null;
	}
	
	/**
	 * Indicates if the Rhythm is connected to the Aurora or not.
	 * 
	 * @return                     true, if the Rhythm is connected
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public boolean isRhythmConnected()
			throws NanoleafException, IOException {
		return Boolean.parseBoolean(get(getURL("rhythm/rhythmConnected")));
	}
	
	/**
	 * <p>Asynchronously indicates if the Rhythm is connected to the Aurora or not.</p>
	 * 
	 * <p>The callback status will return {@link NanoleafCallback#SUCCESS} on success,
	 * or {@link NanoleafCallback#UNAUTHORIZED} if the access token is invalid.
	 * If an internal API error occurs, it will instead return
	 * {@link NanoleafCallback#FAILURE}.</p>
	 * 
	 * @param callback   returns true, if the Rhythm module is connected, or false
	 *                   if it is not connected
	 */
	public void isRhythmConnectedAsync(NanoleafCallback<Boolean> callback)
			throws NanoleafException, IOException {
		getAsyncBool(getURL("rhythm/rhythmConnected"), callback);
	}
	
	/**
	 * Indicates if the Rhythm's microphone is currently active or not.
	 * 
	 * @return                     true, if the Rhythm is active
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public boolean isRhythmMicActive()
			throws NanoleafException, IOException {
		return Boolean.parseBoolean(get(getURL("rhythm/rhythmActive")));
	}
	
	/**
	 * <p>Indicates if the Rhythm's microphone is currently active or not.</p>
	 * 
	 * <p>The callback status will return {@link NanoleafCallback#SUCCESS} on success,
	 * or {@link NanoleafCallback#UNAUTHORIZED} if the access token is invalid.
	 * If an internal API error occurs, it will instead return
	 * {@link NanoleafCallback#FAILURE}.</p>
	 * 
	 * @param callback   returns true, if the Rhythm microphone is active,
	 *                   or false if it is not active
	 */
	public void isRhythmMicActiveAsync(NanoleafCallback<Boolean> callback) {
		getAsyncBool(getURL("rhythm/rhythmActive"), callback);
	}
	
	/**
	 * Indicates the Rhythm's ID in the Aurora system.
	 * 
	 * @return                     the Rhythm's ID as an integer
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public int getRhythmId()
			throws NanoleafException, IOException {
		return Integer.parseInt(get(getURL("rhythm/rhythmId")));
	}
	
	/**
	 * <p>Asynchronously indicates the ID of the Rhythm module in the Aurora system.</p>
	 * 
	 * <p>The callback status will return {@link NanoleafCallback#SUCCESS} on success,
	 * or {@link NanoleafCallback#UNAUTHORIZED} if the access token is invalid.
	 * If an internal API error occurs, it will instead return
	 * {@link NanoleafCallback#FAILURE}.</p>
	 * 
	 * @param callback   returns the ID of the Rhythm module
	 */
	public void getRhythmIdAsync(NanoleafCallback<Integer> callback) {
		getAsyncInt(getURL("rhythm/rhythmId"), callback);
	}
	
	/**
	 * Indicates the hardware version of the Rhythm module.
	 * 
	 * @return                     the hardware version of the Rhythm module
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public String getRhythmHardwareVersion()
			throws NanoleafException, IOException {
		return get(getURL("rhythm/hardwareVersion"));
	}
	
	/**
	 * <p>Asynchronously indicates hardware version of the Rhythm module.</p>
	 * 
	 * <p>The callback status will return {@link NanoleafCallback#SUCCESS} on success,
	 * or {@link NanoleafCallback#UNAUTHORIZED} if the access token is invalid.
	 * If an internal API error occurs, it will instead return
	 * {@link NanoleafCallback#FAILURE}.</p>
	 * 
	 * @param callback   returns the hardware version of the Rhythm module
	 */
	public void getRhythmHardwareVersionAsync(NanoleafCallback<String> callback) {
		getAsync(getURL("rhythm/hardwareVersion"), callback);
	}
	
	/**
	 * Indicates the firmware version of the Rhythm module.
	 * 
	 * @return                     the firmware version of the Rhythm module
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public String getRhythmFirmwareVersion()
			throws NanoleafException, IOException {
		return get(getURL("rhythm/firmwareVersion"));
	}
	
	/**
	 * <p>Asynchronously indicates the firmware version of the Rhythm module.</p>
	 * 
	 * <p>The callback status will return {@link NanoleafCallback#SUCCESS} on success,
	 * or {@link NanoleafCallback#UNAUTHORIZED} if the access token is invalid.
	 * If an internal API error occurs, it will instead return
	 * {@link NanoleafCallback#FAILURE}.</p>
	 * 
	 * @param callback   returns the firmware version of the Rhythm module
	 */
	public void getRhythmFirmwareVersionAsync(NanoleafCallback<String> callback) {
		getAsync(getURL("rhythm/firmwareVersion"), callback);
	}
	
	/**
	 * Indicates if an aux cable (3.5mm) is currently connected to the Rhythm.
	 * 
	 * @return                     true, if an aux cable is connected
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public boolean isRhythmAuxAvailable()
			throws NanoleafException, IOException {
		return Boolean.parseBoolean(get(getURL("rhythm/auxAvailable")));
	}
	
	public void isRhythmAuxAvailableAsync(NanoleafCallback<Boolean> callback) {
		getAsyncBool(getURL("rhythm/auxAvailable"), callback);
	}
	
	/**
	 * Allows the user to control the sound source for the Rhythm. Mode 0 is
	 * the microphone. Mode 1 is the aux (3.5mm) cable.
	 * 
	 * @return                     the Rhythm mode (0 or 1)
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public int getRhythmMode()
			throws NanoleafException, IOException {
		return Integer.parseInt(get(getURL("rhythm/rhythmMode")));
	}
	
	/**
	 * <p>Asynchronously allows the user to control the sound source for the Rhythm.
	 * Mode 0 is the microphone. Mode 1 is the aux (3.5mm) cable.</p>
	 * 
	 * <p>The callback status will return {@link NanoleafCallback#SUCCESS} on success,
	 * or {@link NanoleafCallback#UNAUTHORIZED} if the access token is invalid.
	 * If an internal API error occurs, it will instead return
	 * {@link NanoleafCallback#FAILURE}.</p>
	 * 
	 * @param callback   returns the Rhythm mode (0 or 1)
	 */
	public void getRhythmModeAsync(NanoleafCallback<String> callback) {
		getAsync(getURL("rhythm/rhythmMode"), callback);
	}
	
	/**
	 * Writing 0 to this field sets the Rhythm's sound source to the microphone,
	 * and writing 1 to the field sets the sound source to the aux cable.
	 * 
	 * @param mode                 the Rhythm mode (0 or 1 only)
	 * @throws NanoleafException   If the access token is invalid, or the
	 *                             mode is not either 0 or 1
	 * @throws IOException         If an HTTP exception occurs
	 */
	public void setRhythmMode(int mode)
			throws NanoleafException, IOException {
		String body = String.format("{\"rhythmMode\": %d}", mode);
		put(getURL("rhythm"), body);
	}
	
	/**
	 * <p>Writing 0 to this field sets the Rhythm's sound source to the microphone,
	 * and writing 1 to the field sets the sound source to the aux cable.</p>
	 * 
	 * <p>The callback status will return {@link NanoleafCallback#SUCCESS} on success,
	 * or {@link NanoleafCallback#UNAUTHORIZED} if the access token is invalid.
	 * If an internal API error occurs, it will instead return
	 * {@link NanoleafCallback#FAILURE}.</p>
	 * 
	 * @param mode       the Rhythm mode (0 or 1 only)
	 * @param callback   called when the Rhythm mode is changed or when
	 *                   an error occurs
	 */
	public void setRhythmModeAsync(int mode, NanoleafCallback<String> callback) {
		String body = String.format("{\"rhythmMode\": %d}", mode);
		putAsync(getURL("rhythm"), body, callback);
	}
	
	/**
	 * Indicates the position and orientation of the Rhythm in the Aurora's layout.
	 * 
	 * @return                     the Rhythm module panel
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public Panel getRhythmPanel()
			throws NanoleafException, IOException {
		return parseRhythmPanelJSON(get(getURL("rhythm")));
	}
	
	/**
	 * <p>Asynchronously indicates the position and orientation of the Rhythm in
	 * the Aurora's layout.</p>
	 * 
	 * 
	 * 
	 * @param callback             returns the Rhythm panel
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
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
	 * 
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public void enableExternalStreaming()
			throws NanoleafException, IOException {
		String body = "{\"write\": {\"command\": \"display\", \"animType\": \"extControl\"}}";
		JSONObject response = new JSONObject(put(getURL("effects"), body));
		String host = response.getString("streamControlIpAddr");
		int port = response.getInt("streamControlPort");
		externalAddress = new InetSocketAddress(host, port);
	}
	
	/**
	 * <p>Enables external streaming mode over UDP.</p>
	 * 
	 * @param callback   called when external streaming has been enabled or
	 *                   when an error has occurred
	 */
	public void enableExternalStreamingAsync(NanoleafCallback<String> callback) {
		String body = "{\"write\": {\"command\": \"display\", \"animType\": \"extControl\"}}";
		putAsync(getURL("effects"), body, (status, data, device) -> {
			JSONObject response = new JSONObject(data);
			String host = response.getString("streamControlIpAddr");
			int port = response.getInt("streamControlPort");
			externalAddress = new InetSocketAddress(host, port);
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
	public void sendStaticEffectExternalStreaming(StaticEffect effect)
			throws NanoleafException, IOException {
		sendAnimData(effect.getAnimationData());
	}
	
	/**
	 * <p>Asynchronously sends a series of frames to the target device.</p>
	 * 
	 * <p><b>Note:</b>Requires external streaming to be enabled. Enable it
	 * using the {@link NanoleafDevice#enableExternalStreaming} method.</p>
	 * 
	 * <p>The callback status will return {@link NanoleafCallback#SUCCESS} on success,
	 * or {@link NanoleafCallback#UNAUTHORIZED} if the access token is invalid.
	 * If an internal API error occurs, it will instead return
	 * {@link NanoleafCallback#FAILURE}.</p>
	 * 
	 * @param effect     the custom effect to be sent to the device
	 * @param callback   called when the effect is sent or when an error occurs
	 */
	public void sendStaticEffectExternalStreamingAsync(StaticEffect effect,
			NanoleafCallback<String> callback) {
		sendAnimDataAsync(effect.getAnimationData(), callback);
	}
	
	@Override
	public void setPanelExternalStreaming(int panelId, int red, int green, int blue, int transitionTime)
			throws NanoleafException, IOException {
		String frame = String.format("1 %d 1 %d %d %d 0 %d",
				panelId, red, green, blue, transitionTime);
		sendAnimData(frame);
	}
	
	@Override
	public void setPanelExternalStreamingAsync(int panelId, int red, int green,
			int blue, int transitionTime, NanoleafCallback<String> callback) {
		String frame = String.format("1 %d 1 %d %d %d 0 %d",
				panelId, red, green, blue, transitionTime);
		sendAnimDataAsync(frame, callback);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null || obj.getClass() != this.getClass()) {
			return false;
		}
		Aurora other = (Aurora)obj;
		return this.getHostname().equals(other.getHostname()) && this.getPort() == other.getPort() &&
				this.getName().equals(other.getName()) && this.getSerialNumber().equals(other.getSerialNumber()) &&
				this.getManufacturer().equals(other.getManufacturer()) && this.getModel().equals(other.getModel());
	}
}
