package io.github.rowak.nanoleafapi;

import java.util.List;
import java.util.Map;
import java.util.Random;

import org.json.JSONArray;
import org.json.JSONObject;

import com.here.oksse.OkSse;
import com.here.oksse.ServerSentEvent;

import io.github.rowak.nanoleafapi.event.NanoleafEventListener;
import okhttp3.Request;

import java.awt.Point;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * This class simplifies the interface for controlling multiple
 * devices together.
 */
public class NanoleafGroup {
	
	private Map<String, NanoleafDevice> devices;
	
	/**
	 * Creates a new empty group. This operation is not networked.
	 */
	public NanoleafGroup() {
		init(null);
	}
	
	/**
	 * Creates a new group from a set of existing devices. This operation is
	 * not networked.
	 * 
	 * @param devices   a map containing devices mapped to their unique names
	 */
	public NanoleafGroup(Map<String, NanoleafDevice> devices) {
		init(devices);
	}
	
	private void init(Map<String, NanoleafDevice> devices) {
		this.devices = new HashMap<String, NanoleafDevice>();
		if (devices != null) {
			for (String d : devices.keySet()) {
				this.devices.put(d, devices.get(d));
			}
		}
	}
	
	public void addDevice(String name, NanoleafDevice device) {
		if (device == null) {
			throw new NullPointerException("Device cannot be null");
		}
		devices.put(name, device);
	}
	
	public void addDevices(Map<String, NanoleafDevice> devices) {
		this.devices.putAll(devices);
	}
	
	public void removeDevice(String name) {
		devices.remove(name);
	}
	
	public Map<String, NanoleafDevice> getDevices() {
		Map<String, NanoleafDevice> devicesCopy = new HashMap<String, NanoleafDevice>();
		for (String s : devices.keySet()) {
			devicesCopy.put(s, devices.get(s));
		}
		return devices;
	}
	
	/**
	 * Force a shutdown of the internal HTTP client.
	 * <b>Note:</b> This method only has effect if asynchronous calls have
	 * been made. The HTTP client will eventually shut down on its own, but
	 * the application will likely hang until it does (unless this method
	 * is called).
	 */
	public void closeAsyncForAll() {
		devices.forEach((n, d) -> d.closeAsync());
	}
	
	/**
	 * Closes all event listeners.
	 */
	public void closeEventListeners() {
		devices.forEach((n, d) -> d.closeEventListener());
	}
	
	/**
	 * Causes the panels to flash in unison.
	 * This is typically used to help users differentiate between multiple panels.
	 * @return  (204 No Content, 401 Unauthorized)
	 * @throws UnauthorizedException  if the access token is invalid
	 */
	public void identify()
			throws NanoleafException, IOException {
		for (NanoleafDevice d : devices.values()) {
			d.identify();
		}
	}
	
	public void identifyAsync(NanoleafCallback<String> callback) {
		devices.forEach((n, d) -> d.identifyAsync(callback));
	}
	
	/**
	 * Sets the on state of the Aurora (true = on, false = off).
	 * @param on  whether the Aurora should be turned on or off
	 * @return  (200 OK, 401 Unauthorized, 422 Unprocessable Entity)
	 * @throws UnauthorizedException  if the access token is invalid
	 */
	public void setOn(boolean on)
			throws NanoleafException, IOException {
		for (NanoleafDevice d : devices.values()) {
			d.setOn(on);
		}
	}
	
	public void setOnAsync(boolean on, NanoleafCallback<String> callback) {
		devices.forEach((n, d) -> d.setOnAsync(on, callback));
	}
	
	/**
	 * Toggles the on state of the Aurora (on = off, off = on).
	 * @return  (200 OK, 401 Unauthorized)
	 * @throws UnauthorizedException  if the access token is invalid
	 */
	public void toggleOn()
			throws NanoleafException, IOException {
		for (NanoleafDevice d : devices.values()) {
			d.toggleOn();
		}
	}
	
	public void toggleOnAsync(NanoleafCallback<String> callback)
			throws NanoleafException, IOException {
		for (NanoleafDevice d : devices.values()) {
			d.toggleOnAsync(callback);
		}
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
		for (NanoleafDevice d : devices.values()) {
			d.setBrightness(brightness);
		}
	}
	
	public void setBrightnessAsync(int brightness, NanoleafCallback<String> callback) {
		devices.forEach((n, d) -> d.setBrightnessAsync(brightness, callback));
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
		for (NanoleafDevice d : devices.values()) {
			d.fadeToBrightness(brightness, duration);
		}
	}
	
	public void fadeToBrightnessAsync(int brightness, int duration, NanoleafCallback<String> callback) {
		devices.forEach((n, d) -> d.fadeToBrightnessAsync(brightness, duration, callback));
	}
	
	/**
	 * Increases the brightness by an amount as a percent.
	 * @param amount  the amount to increase by
	 * @return  (204 No Content, 401 Unauthorized, 422 Unprocessable Entity)
	 * @throws UnauthorizedException  if the access token is invalid
	 */
	public void increaseBrightness(int amount)
			throws NanoleafException, IOException {
		for (NanoleafDevice d : devices.values()) {
			d.increaseBrightness(amount);
		}
	}
	
	public void increaseBrightnessAsync(int amount, NanoleafCallback<String> callback) {
		devices.forEach((n, d) -> d.increaseBrightnessAsync(amount, callback));
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
	 * Sets the hue of the Aurora (static/custom effects only).
	 * @param hue  the new hue
	 * @return  (204 No Content, 401 Unauthorized, 422 Unprocessable Entity)
	 * @throws UnauthorizedException  if the access token is invalid
	 * @throws UnprocessableEntityException  if <code>hue</code> is not within the
	 * 										 maximum and minimum restrictions
	 */
	public void setHue(int hue)
			throws NanoleafException, IOException {
		for (NanoleafDevice d : devices.values()) {
			d.setHue(hue);
		}
	}
	
	public void increaseHue(int amount)
			throws NanoleafException, IOException {
		for (NanoleafDevice d : devices.values()) {
			d.increaseHue(amount);
		}
	}
	
	public void decreaseHue(int amount)
			throws NanoleafException, IOException {
		increaseHue(-amount);
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
		for (NanoleafDevice d : devices.values()) {
			d.setSaturation(saturation);
		}
	}
	
	public void setSaturationAsync(int saturation, NanoleafCallback<String> callback) {
		devices.forEach((n, d) -> d.setSaturationAsync(saturation, callback));
	}
	
	public void increaseSaturation(int amount)
			throws NanoleafException, IOException {
		for (NanoleafDevice d : devices.values()) {
			d.increaseSaturation(amount);
		}
	}
	
	public void increaseSaturationAsync(int amount, NanoleafCallback<String> callback) {
		devices.forEach((n, d) -> d.increaseSaturationAsync(amount, callback));
	}
	
	public void decreaseSaturation(int amount)
			throws NanoleafException, IOException {
		increaseSaturation(-amount);
	}
	
	public void decreaseSaturationAsync(int amount, NanoleafCallback<String> callback) {
		increaseSaturationAsync(-amount, callback);
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
		for (NanoleafDevice d : devices.values()) {
			d.setColorTemperature(colorTemperature);
		}
	}
	
	public void setColorTemperatureAsync(int colorTemperature, NanoleafCallback<String> callback) {
		devices.forEach((n,d) -> d.setColorTemperatureAsync(colorTemperature, callback));
	}
	
	public void increaseColorTemperature(int amount)
			throws NanoleafException, IOException {
		for (NanoleafDevice d : devices.values()) {
			d.increaseColorTemperature(amount);
		}
	}
	
	public void increaseColorTemperatureAsync(int amount, NanoleafCallback<String> callback) {
		devices.forEach((n, d) -> d.increaseColorTemperatureAsync(amount, callback));
	}
	
	public void decreaseColorTemperature(int amount)
			throws NanoleafException, IOException {
		increaseColorTemperature(-amount);
	}
	
	public void decreaseColorTemperatureAsync(int amount, NanoleafCallback<String> callback) {
		increaseColorTemperatureAsync(-amount, callback);
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
		for (NanoleafDevice d : devices.values()) {
			d.setEffect(effectName);
		}
	}
	
	public void setEffectAsync(String effectName, NanoleafCallback<String> callback) {
		devices.forEach((n, d) -> d.setEffectAsync(effectName, callback));
	}
	
	/**
	 * Sets a random effect based on the effects installed on the
	 * Aurora controller. This includes dynamic as well as Rhythm effects.
	 * @return  (200 OK, 204 No Content, 401 Unauthorized, 404 Resource Not Found)
	 * @throws UnauthorizedException  if the access token is invalid
	 */
	public void setRandomEffect()
			throws NanoleafException, IOException {
		for (NanoleafDevice d : devices.values()) {
			d.setRandomEffect();
		}
	}
	
//	/**
//	 * Gets an array of type <code>Effect</code> containing all of
//	 * the effects installed on the Aurora controller.
//	 * @return  an array of the effects installed on the Aurora controller
//	 * @throws UnauthorizedException   if the access token is invalid
//	 */
//	public Effect[] getAllEffects()
//			throws NanoleafException, IOException {
//		JSONObject json = new JSONObject(writeEffect("{\"command\": \"requestAll\"}"));
//		JSONArray animations = json.getJSONArray("animations");
//		Effect[] effects = new Effect[animations.length()];
//		for (int i = 0; i < animations.length(); i++) {
//			effects[i] = Effect.createFromJSON(animations.getJSONObject(i));
//		}
//		return effects;
//	}
	
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
		for (NanoleafDevice d : devices.values()) {
			d.addEffect(effect);
		}
	}
	
	public void addEffectAsync(Effect effect, NanoleafCallback<String> callback) {
		devices.forEach((n, d) -> d.addEffectAsync(effect, callback));
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
		for (NanoleafDevice d : devices.values()) {
			d.deleteEffect(effectName);
		}
	}
	
	public void deleteEffectAsync(String effectName, NanoleafCallback<String> callback) {
		devices.forEach((n, d) -> d.deleteEffectAsync(effectName, callback));
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
		for (NanoleafDevice d : devices.values()) {
			d.renameEffect(effectName, newName);
		}
	}
	
	public void renameEffectAsync(String effectName, String newName, NanoleafCallback<String> callback) {
		devices.forEach((n, d) -> d.renameEffectAsync(effectName, newName, callback));
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
		for (NanoleafDevice d : devices.values()) {
			d.displayEffect(effect);
		}
	}
	
	public void displayEffectAsync(Effect effect, NanoleafCallback<String> callback) {
		devices.forEach((n, d) -> d.displayEffectAsync(effect, callback));
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
		for (NanoleafDevice d : devices.values()) {
			d.displayEffectFor(effectName, duration);
		}
	}
	
	public void displayEffectForAsync(String effectName, int duration, NanoleafCallback<String> callback) {
		devices.forEach((n, d) -> d.displayEffectForAsync(effectName, duration, callback));
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
		custom.setLoopEnabled(false);
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
//	/**
//	 * Gets <i>all</i> the plugins/motions from the Aurora.
//	 * <br><b>Note: This method is slow.</b>
//	 * @return  an array of plugins from the Aurora
//	 * @throws UnauthorizedException  if the access token is invalid
//	 */
//	public Plugin[] getPlugins() throws NanoleafException, IOException, InterruptedException {
//		String body = String.format("{\"write\": {\"command\": \"requestPlugins\"}}");
//		JSONObject json = new JSONObject(put(getURL("effects"), body));
//		JSONArray arr = json.getJSONArray("plugins");
//		Plugin[] plugins = new Plugin[arr.length()];
//		for (int i = 0; i < arr.length(); i++)
//		{
//			plugins[i] = Plugin.fromJSON(arr.getJSONObject(i).toString());
//		}
//		return plugins;
//	}
	
	public void writeEffectAsync(String command, NanoleafCallback<String> callback) {
		devices.forEach((n, d) -> d.writeEffectAsync(command, callback));
	}
	
//	/**
//	 * Gets an array of the connected panels.
//	 * Each <code>Panel</code> contains the <b>original position data.</b>
//	 * @return  an array of panels
//	 * @throws UnauthorizedException  if the access token is invalid
//	 */
//	public Panel[] getAllPanels()
//			throws NanoleafException, IOException {
//		return parsePanelsJSON(get(getURL("panelLayout/layout")));
//	}
//	
//	public void getAllPanelsAsync(NanoleafCallback<Panel[]> callback) {
//		getAsync(getURL("panelLayout/layout"), (status, data) -> {
//			if (status != NanoleafCallback.SUCCESS) {
//				callback.onCompleted(status, null);
//			}
//			Panel[] panels = parsePanelsJSON(data);
//			callback.onCompleted(status, panels);
//		});
//	}
	
//	/**
//	 * Gets an array of the connected panels that are
//	 * rotated to match the global orientation.
//	 * Each <code>Panel</code> contains <b>modified position data.</b>
//	 * @return an array of rotated panels
//	 * @throws UnauthorizedException  if the access token is invalid
//	 */
//	public Panel[] getAllPanelsRotated()
//			throws NanoleafException, IOException {
//		Panel[] panels = getPanels();
//		Point origin = getLayoutCentroid(panels);
//		int globalOrientation = getGlobalOrientation();
//		globalOrientation = globalOrientation == 360 ? 0 : globalOrientation;
//		double radAngle = Math.toRadians(globalOrientation);
//		for (Panel p : panels) {
//			int x = p.getX() - origin.x;
//			int y = p.getY() - origin.y;
//			
//			double newX = x * Math.cos(radAngle) - y * Math.sin(radAngle);
//			double newY = x * Math.sin(radAngle) + y * Math.cos(radAngle);
//			
//			x = (int)(newX + origin.x);
//			y = (int)(newY + origin.y);
//			p.setX(x);
//			p.setY(y);
//		}
//		return panels;
//	}
	
	/**
	 * Sets the global orientation for the Aurora.
	 * @param orientation  the global orientation
	 * @return  (204 No Content, 401 Unauthorized)
	 * @throws UnauthorizedException  if the access token is invalid
	 */
	public void setGlobalOrientation(int orientation)
			throws NanoleafException, IOException {
		for (NanoleafDevice d : devices.values()) {
			d.setGlobalOrientation(orientation);
		}
	}
	
	public void setGlobalOrientation(int orientation, NanoleafCallback<String> callback) {
		devices.forEach((n, d) -> d.setGlobalOrientation(orientation, callback));
	}
	
	/**
	 * Enables external streaming mode over UDP.
	 * @throws UnauthorizedException  if the access token is invalid
	 */
	public void enableExternalStreaming()
			throws NanoleafException, IOException {
		for (NanoleafDevice d : devices.values()) {
			d.enableExternalStreaming();
		}
	}
	
	public void enableExternalStreamingAsync(NanoleafCallback<String> callback)
			throws NanoleafException, IOException {
		for (NanoleafDevice d : devices.values()) {
			d.enableExternalStreamingAsync(callback);
		}
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
		for (NanoleafDevice d : devices.values()) {
			d.sendAnimData(animData);
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
		for (NanoleafDevice d : devices.values()) {
			d.setPanelExternalStreaming(panelId, red, green, blue, transitionTime);
		}
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
	
//	public ServerSentEvent registerTouchEventListener(NanoleafEventListener listener,
//			boolean stateEvents, boolean layoutEvents, boolean effectsEvents, boolean touchEvents) {
//		String url = getURL("events" + getEventsQueryString(stateEvents, layoutEvents, effectsEvents, touchEvents));
//		Request req = new Request.Builder()
//				.url(url)
//				.get()
//				.build();
//		OkSse okSse = new OkSse(client);
//		ServerSentEvent s = okSse.newServerSentEvent(req, listener);
//		sse.add(s);
//		return s;
//	}
}
