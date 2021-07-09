package io.github.rowak.nanoleafapi;

import java.util.List;
import java.util.Map;

import io.github.rowak.nanoleafapi.event.NanoleafEventListener;
import io.github.rowak.nanoleafapi.event.NanoleafTouchEventListener;

import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
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
	
	/**
	 * Adds a device to the group.
	 * 
	 * @param name     a unique name for the device
	 * @param device   the device to add
	 */
	public void addDevice(String name, NanoleafDevice device) {
		if (device == null) {
			throw new NullPointerException("Device cannot be null");
		}
		devices.put(name, device);
	}
	
	/**
	 * Adds a set of devices to the group.
	 * 
	 * @param devices   the devices to add
	 */
	public void addDevices(Map<String, NanoleafDevice> devices) {
		this.devices.putAll(devices);
	}
	
	/**
	 * Removes a device from the group.
	 * 
	 * @param name   the name of the device to remove
	 */
	public void removeDevice(String name) {
		devices.remove(name);
	}
	
	/**
	 * Removes all devices from the group.
	 */
	public void removeAllDevices() {
		devices.clear();
	}
	
	/**
	 * Gets the set of devices in the group.
	 * 
	 * @return   the group devices
	 */
	public Map<String, NanoleafDevice> getDevices() {
		Map<String, NanoleafDevice> devicesCopy = new HashMap<String, NanoleafDevice>();
		for (String s : devices.keySet()) {
			devicesCopy.put(s, devices.get(s));
		}
		return devices;
	}
	
	/**
	 * Gets the number of devices in the group.
	 * 
	 * @return   the number of devices
	 */
	public int getGroupSize() {
		return devices.size();
	}
	
	/**
	 * Synchronously executes a shared action for each device in the group.
	 * 
	 * @param action   the shared action
	 */
	public void forEach(GroupAction action) {
		for (NanoleafDevice d : devices.values()) {
			action.run(d);
		}
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
	 * Closes all event listeners (not including touch event streaming listeners).
	 */
	public void closeEventListeners() {
		devices.forEach((n, d) -> d.closeEventListeners());
	}
	
	/**
	 * Closes all low-latency touch event (streaming) listeners.
	 */
	public void closeTouchEventListeners() {
		devices.forEach((n, d) -> d.closeTouchEventListeners());
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
		for (NanoleafDevice d : devices.values()) {
			d.identify();
		}
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
		devices.forEach((n, d) -> d.identifyAsync(callback));
	}
	
	/**
	 * Sets the on state of each device (true = on, false = off).
	 * 
	 * @param on                   whether the devices should be turned on or off
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public void setOn(boolean on)
			throws NanoleafException, IOException {
		for (NanoleafDevice d : devices.values()) {
			d.setOn(on);
		}
	}
	
	/**
	 * <p>Asynchronously sets the on state of each device (true = on, false = off).</p>
	 * 
	 * <p>The callback status will return {@link NanoleafCallback#SUCCESS} on success,
	 * or {@link NanoleafCallback#UNAUTHORIZED} if the access token is invalid. If an
	 * internal API error occurs, it will instead return {@link NanoleafCallback#FAILURE}.</p>
	 * 
	 * @param on         whether the devices should be turned on or off
	 * @param callback   called when the devices changes power state or when an error occurs
	 */
	public void setOnAsync(boolean on, NanoleafCallback<String> callback) {
		devices.forEach((n, d) -> d.setOnAsync(on, callback));
	}
	
	/**
	 * Toggles the on state of each device (on = off, off = on).
	 * 
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public void toggleOn()
			throws NanoleafException, IOException {
		for (NanoleafDevice d : devices.values()) {
			d.toggleOn();
		}
	}
	
	/**
	 * <p>Toggles the on state of each device (on = off, off = on).</p>
	 * 
	 * <p>The callback status will return {@link NanoleafCallback#SUCCESS} on success,
	 * or {@link NanoleafCallback#UNAUTHORIZED} if the access token is invalid. If an
	 * internal API error occurs, it will instead return {@link NanoleafCallback#FAILURE}.</p>
	 * 
	 * @param callback             called when the the devices changes
	 *                             power state or if error occurs
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public void toggleOnAsync(NanoleafCallback<Boolean> callback)
			throws NanoleafException, IOException {
		for (NanoleafDevice d : devices.values()) {
			d.toggleOnAsync(callback);
		}
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
		for (NanoleafDevice d : devices.values()) {
			d.setBrightness(brightness);
		}
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
		devices.forEach((n, d) -> d.setBrightnessAsync(brightness, callback));
	}
	
	/**
	 * <p>Fades the master brightness of the devices over a period of time.</p>
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
		for (NanoleafDevice d : devices.values()) {
			d.fadeToBrightness(brightness, duration);
		}
	}
	
	/**
	 * <p>Asynchronously fades the master brightness of the devices over a period of time.</p>
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
		devices.forEach((n, d) -> d.fadeToBrightnessAsync(brightness, duration, callback));
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
		for (NanoleafDevice d : devices.values()) {
			d.increaseBrightness(amount);
		}
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
		devices.forEach((n, d) -> d.increaseBrightnessAsync(amount, callback));
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
	 * Sets the hue of the devices (static/custom effects only).
	 * 
	 * @param hue                  the new hue
	 * @throws NanoleafException   If the access token is invalid, or the specified
	 *                             hue is not between 0 and 360 (inclusive).
	 * @throws IOException         If an HTTP exception occurs
	 */
	public void setHue(int hue)
			throws NanoleafException, IOException {
		for (NanoleafDevice d : devices.values()) {
			d.setHue(hue);
		}
	}
	
	/**
	 * <p>Asynchronously sets the hue of the devices (static/custom effects only).</p>
	 * 
	 * <p>The callback status will return {@link NanoleafCallback#SUCCESS} on success,
	 * or {@link NanoleafCallback#UNAUTHORIZED} if the access token is invalid. If an
	 * internal API error occurs, it will instead return {@link NanoleafCallback#FAILURE}.</p>
	 * 
	 * @param hue        the new hue
	 * @param callback   called when the hue changes or when an error occurs
	 */
	public void setHueAsync(int hue, NanoleafCallback<String> callback) {
		devices.forEach((n, d) -> d.setHueAsync(hue, callback));
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
		for (NanoleafDevice d : devices.values()) {
			d.increaseHue(amount);
		}
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
		devices.forEach((n, d) -> d.increaseHueAsync(amount, callback));
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
	 * Sets the saturation of the devices (static/custom effects only).
	 * 
	 * @param saturation           the new saturation
	 * @throws NanoleafException   If the access token is invalid, or the specified
	 *                             saturation is not between 0 and 100 (inclusive).
	 * @throws IOException         If an HTTP exception occurs
	 */
	public void setSaturation(int saturation)
			throws NanoleafException, IOException {
		for (NanoleafDevice d : devices.values()) {
			d.setSaturation(saturation);
		}
	}
	
	/**
	 * <p>Asynchronously sets the saturation of the devices (static/custom effects only).</p>
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
		devices.forEach((n, d) -> d.setSaturationAsync(saturation, callback));
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
		for (NanoleafDevice d : devices.values()) {
			d.increaseSaturation(amount);
		}
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
		devices.forEach((n, d) -> d.increaseSaturationAsync(amount, callback));
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
	 * Sets the color temperature of the devices in Kelvin.
	 * 
	 * @param colorTemperature     color temperature in Kelvin
	 * @throws NanoleafException   If the access token is invalid, or the specified
	 *                             color temperature is not between 1200 and 6500
	 *                             (inclusive)
	 * @throws IOException         If an HTTP exception occurs
	 */
	public void setColorTemperature(int colorTemperature)
			throws NanoleafException, IOException {
		for (NanoleafDevice d : devices.values()) {
			d.setColorTemperature(colorTemperature);
		}
	}
	
	/**
	 * <p>Asynchronously sets the color temperature of the devices in Kelvin.</p>
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
		devices.forEach((n,d) -> d.setColorTemperatureAsync(colorTemperature, callback));
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
		for (NanoleafDevice d : devices.values()) {
			d.increaseColorTemperature(amount);
		}
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
		devices.forEach((n, d) -> d.increaseColorTemperatureAsync(amount, callback));
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
	 * <p>Sets the color (HSB) of the device.</p>
	 * 
	 * <p>The callback status will return {@link NanoleafCallback#SUCCESS} on success,
	 * or {@link NanoleafCallback#UNAUTHORIZED} if the access token is invalid. If an
	 * internal API error occurs, it will instead return
	 * {@link NanoleafCallback#FAILURE}. The returned data will never be meaningful
	 * (either an empty string or null).</p>
	 * 
	 * @param color      the new color
	 * @param callback   called when the color changes or when an error occurs
	 */
	public void setColorAsync(Color color, NanoleafCallback<String> callback) {
		devices.forEach((n,d) -> {
			setHueAsync(color.getHue(), (status, data, device) -> {
				if (status != NanoleafCallback.SUCCESS && callback != null) {
					callback.onCompleted(status, null, d);
					return;
				}
				try {
					setSaturation(color.getSaturation());
					setBrightness(color.getBrightness());
					if (callback != null) {
						callback.onCompleted(NanoleafCallback.SUCCESS, "", device);
					}
				}
				catch (NanoleafException e) {
					if (callback != null) {
						callback.onCompleted(e.getCode(), "", device);
					}
				}
				catch (Exception e) {
					if (callback != null) {
						callback.onCompleted(NanoleafCallback.FAILURE, null, device);
					}
				}
			});
		});
	}
	
	/**
	 * Gets a list of the names of the effects on all the devices.
	 * 
	 * @return                     a list of all effects
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public List<String> getAllEffectsList()
			throws NanoleafException, IOException {
		List<String> effects = new ArrayList<String>();
		for (NanoleafDevice d : devices.values()) {
			for (String ef : d.getEffectsList()) {
				if (!effects.contains(ef)) {
					effects.add(ef);
				}
			}
		}
		return effects;
	}
	
	/**
	 * Sets the selected effect on the devices to the effect specified by
	 * <code>effectName</code>.
	 * 
	 * @param effectName           the name of the effect
	 * @throws NanoleafException   If the access token is invalid, or the
	 *                             effect does not exist on the device
	 * @throws IOException         If an HTTP exception occurs
	 */
	public void setEffect(String effectName)
			throws NanoleafException, IOException {
		for (NanoleafDevice d : devices.values()) {
			d.setEffect(effectName);
		}
	}
	
	/**
	 * <p>Sets the selected effect on the devices to the effect specified by
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
		devices.forEach((n, d) -> d.setEffectAsync(effectName, callback));
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
		for (NanoleafDevice d : devices.values()) {
			d.setRandomEffect();
		}
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
		devices.forEach((n, d) -> d.setRandomEffectAsync(callback));
	}
	
	/**
	 * Gets a list of the effects on all effects.
	 * 
	 * @return                     a list of all effects
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public List<Effect> getAllEffects()
			throws NanoleafException, IOException {
		List<Effect> effects = new ArrayList<Effect>();
		for (NanoleafDevice d : devices.values()) {
			for (Effect ef : d.getAllEffects()) {
				if (!effects.contains(ef)) {
					effects.add(ef);
				}
			}
		}
		return effects;
	}
	
	/**
	 * Uploads and installs an effect to the devices. If the effect does not exist
	 * on the devices it will be created. If the effect exists it will be overwritten.
	 * 
	 * @param effect               the effect to be uploaded
	 * @throws NanoleafException   If the access token is invalid, or the effect parameter
	 *                             is configured incorrectly
	 * @throws IOException         If an HTTP exception occurs
	 */
	public void addEffect(Effect effect)
			throws NanoleafException, IOException {
		for (NanoleafDevice d : devices.values()) {
			d.addEffect(effect);
		}
	}
	
	/**
	 * <p>Asynchronously uploads and installs an effect to the devices. If the effect does
	 * not exist on the devices it will be created. If the effect exists it will be
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
		devices.forEach((n, d) -> d.addEffectAsync(effect, callback));
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
		for (NanoleafDevice d : devices.values()) {
			d.deleteEffect(effectName);
		}
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
		devices.forEach((n, d) -> d.deleteEffectAsync(effectName, callback));
	}
	
	/**
	 * Renames an effect on the device.
	 * 
	 * @param effectName           the name of the effect
	 * @param newName              the new name of the effect
	 * @throws NanoleafException   If the access token is invalid, or the effect
	 *                             does not exist on the devices, or the new name
	 *                             is illegal
	 * @throws IOException         If an HTTP exception occurs
	 */
	public void renameEffect(String effectName, String newName)
			throws NanoleafException, IOException {
		for (NanoleafDevice d : devices.values()) {
			d.renameEffect(effectName, newName);
		}
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
		devices.forEach((n, d) -> d.renameEffectAsync(effectName, newName, callback));
	}
	
	/**
	 * Displays an effect on the devices without installing it.
	 * 
	 * @param effect               the effect to be previewed
	 * @throws NanoleafException   If the access token is invalid, or the effect
	 *                             parameter is configured incorrectly
	 * @throws IOException         If an HTTP exception occurs
	 */
	public void displayEffect(Effect effect)
			throws NanoleafException, IOException {
		for (NanoleafDevice d : devices.values()) {
			d.displayEffect(effect);
		}
	}
	
	/**
	 * <p>Asynchronously displays an effect on the devices without installing it.</p>
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
		devices.forEach((n, d) -> d.displayEffectAsync(effect, callback));
	}
	
	/**
	 * Displays an effect on the devices for a given duration without installing it.
	 * 
	 * @param effectName           the name of the effect to be previewed
	 * @param duration             the duration for the effect to be displayed
	 * @throws NanoleafException   If the access token is invalid, or the specified
	 *                             effect does not exist on the device
	 * @throws IOException         If an HTTP exception occurs
	 */
	public void displayEffectFor(String effectName, int duration)
			throws NanoleafException, IOException {
		for (NanoleafDevice d : devices.values()) {
			d.displayEffectFor(effectName, duration);
		}
	}
	
	/**
	 * <p>Asynchronously displays an effect on the devices for a given duration without
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
		devices.forEach((n, d) -> d.displayEffectForAsync(effectName, duration, callback));
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
		for (NanoleafDevice d : devices.values()) {
			d.setPanelColor(panel.getId(), red, green, blue, transitionTime);
		}
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
		devices.forEach((n, d) -> d.setPanelColorAsync(panel, red, green, blue,
				transitionTime, callback));
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
		for (NanoleafDevice d : devices.values()) {
			d.setPanelColor(panel, hexColor, transitionTime);
		}
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
		devices.forEach((n, d) -> d.setPanelColorAsync(panel, hexColor,
				transitionTime, callback));
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
		for (NanoleafDevice d : devices.values()) {
			d.setPanelColor(panelId, red, green, blue, transitionTime);
		}
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
	public void setPanelColorAsync(int panelId, int red, int green, int blue, int transitionTime, NanoleafCallback<String> callback)
			throws NanoleafException, IOException {
		devices.forEach((n, d) -> d.setPanelColorAsync(panelId, red, green,
				blue, transitionTime, callback));
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
		for (NanoleafDevice d : devices.values()) {
			d.setPanelColor(panelId, hexColor, transitionTime);
		}
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
		devices.forEach((n, d) -> d.setPanelColorAsync(panelId, hexColor,
				transitionTime, callback));
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
		for (NanoleafDevice d : devices.values()) {
			d.fadeToColor(red, green, blue, duration);
		}
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
		devices.forEach((n, d) -> d.fadeToColorAsync(red, green, blue, duration, callback));
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
		for (NanoleafDevice d : devices.values()) {
			d.fadeToColor(hexColor, duration);
		}
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
		devices.forEach((n, d) -> d.fadeToColorAsync(hexColor, duration, callback));
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
		for (NanoleafDevice d : devices.values()) {
			d.fadeToColor(color, duration);
		}
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
		devices.forEach((n, d) -> d.fadeToColorAsync(color, duration, callback));
	}
	
	/**
	 * <p><b>(This method works with JSON data)</b></p>
	 * 
	 * <p>Asynchronously uploads a JSON string to the devices. Calls the write effect
	 * command from the <a href = "https://forum.nanoleaf.me/docs#_u2t4jzmkp8nt">OpenAPI</a>.
	 * Refer to it for more information about the commands.</p>
	 * 
	 * <h1>Commands:</h1>
	 * <ul>
	 *   <li><i>add</i>          -  Installs an effect on the devices or updates the effect
	 *                              if it already exists.</li>
	 *   <li><i>delete</i>       -  Permanently removes an effect from the device.</li>
	 *   <li><i>request</i>      -  Requests a single effect by name.</li>
	 *   <li><i>requestAll</i>   -  Requests all the installed effects from the device.</li>
	 *   <li><i>display</i>      -  Sets a color mode on the devices (used for previewing
	 *                              effects).</li>
	 *   <li><i>displayTemp</i>  -  Temporarily sets a color mode on the devices (typically
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
		devices.forEach((n, d) -> d.writeEffectAsync(command, callback));
	}
	
	/**
	 * Gets an array of the connected panels.
	 * Each <code>Panel</code> contains the <b>original position data.</b>
	 * @return  an array of panels
	 * @throws UnauthorizedException  if the access token is invalid
	 */
	public List<Panel> getAllPanels()
			throws NanoleafException, IOException {
		List<Panel> panels = new ArrayList<Panel>();
		for (NanoleafDevice d : devices.values()) {
			panels.addAll(d.getPanels());
		}
		return panels;
	}
	
//	public void getAllPanelsAsync(NanoleafCallback<Panel[]> callback) {
//		getAsync(getURL("panelLayout/layout"), (status, data) -> {
//			if (status != NanoleafCallback.SUCCESS) {
//				callback.onCompleted(status, null);
//			}
//			Panel[] panels = parsePanelsJSON(data);
//			callback.onCompleted(status, panels);
//		});
//	}
	
	/**
	 * Gets an array of the connected panels that are
	 * rotated to match the global orientation.
	 * Each <code>Panel</code> contains <b>modified position data.</b>
	 * @return an array of rotated panels
	 * @throws UnauthorizedException  if the access token is invalid
	 */
	public List<Panel> getAllPanelsRotated()
			throws NanoleafException, IOException {
		List<Panel> panels = new ArrayList<Panel>();
		for (NanoleafDevice d : devices.values()) {
			panels.addAll(d.getPanelsRotated());
		}
		return panels;
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
		for (NanoleafDevice d : devices.values()) {
			d.setGlobalOrientation(orientation);
		}
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
		devices.forEach((n, d) -> d.setGlobalOrientationAsync(orientation, callback));
	}
	
	/**
	 * Enables external streaming mode over UDP.
	 * 
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public void enableExternalStreaming()
			throws NanoleafException, IOException {
		for (NanoleafDevice d : devices.values()) {
			d.enableExternalStreaming();
		}
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
	public void enableExternalStreamingAsync(NanoleafCallback<String> callback)
			throws NanoleafException, IOException {
		devices.forEach((n, d) -> d.enableExternalStreamingAsync(callback));
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
		for (NanoleafDevice d : devices.values()) {
			d.sendStaticEffectExternalStreaming(effect);
		}
	}
	
	/**
	 * <p>Sends a static animation data string to the target device.</p>
	 * 
	 * <p><b>Note:</b>Requires external streaming to be enabled. Enable it
	 * using the {@link NanoleafDevice#enableExternalStreaming} method.</p>
	 * 
	 * @param animData   the static animation data to be sent to the device
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an I/O exception occurs
	 */
	public void sendAnimData(String animData)
			throws NanoleafException, IOException {
		for (NanoleafDevice d : devices.values()) {
			d.sendAnimData(animData);
		}
	}
	
	/**
	 * <p>Asynchronously sends a static animation data string to the target device.</p>
	 * 
	 * <p><b>Note:</b>Requires external streaming to be enabled. Enable it
	 * using the {@link NanoleafDevice#enableExternalStreaming} method.</p>
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
		devices.forEach((n, d) -> d.sendAnimDataAsync(animData, callback));
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
		for (NanoleafDevice d : devices.values()) {
			d.setPanelExternalStreaming(panelId, red, green, blue, transitionTime);
		}
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
		devices.forEach((n, d) -> d.setPanelExternalStreamingAsync(panelId, red, green,
				blue, transitionTime, callback));
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
		for (NanoleafDevice d : devices.values()) {
			d.setPanelExternalStreaming(panel, red, green, blue, transitionTime);
		}
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
		devices.forEach((n, d) -> d.setPanelExternalStreamingAsync(panel, red, green, blue,
				transitionTime, callback));
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
		for (NanoleafDevice d : devices.values()) {
			d.setPanelExternalStreaming(panelId, color, transitionTime);
		}
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
		devices.forEach((n, d) -> d.setPanelExternalStreamingAsync(panelId, color,
				transitionTime, callback));
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
		for (NanoleafDevice d : devices.values()) {
			d.setPanelExternalStreaming(panel, color, transitionTime);
		}
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
		devices.forEach((n, d) -> d.setPanelExternalStreamingAsync(panel, color,
				transitionTime, callback));
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
		for (NanoleafDevice d : devices.values()) {
			d.setPanelExternalStreaming(panelId, hexColor, transitionTime);
		}
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
		devices.forEach((n, d) -> d.setPanelExternalStreamingAsync(panelId, hexColor,
				transitionTime, callback));
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
		for (NanoleafDevice d : devices.values()) {
			d.setPanelExternalStreaming(panel, hexColor, transitionTime);
		}
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
		devices.forEach((n, d) -> d.setPanelExternalStreamingAsync(panel, hexColor,
				transitionTime, callback));
	}
	
	public void registerEventListener(NanoleafEventListener listener,
			boolean stateEvents, boolean layoutEvents, boolean effectsEvents, boolean touchEvents) {
		for (NanoleafDevice d : devices.values()) {
			d.registerEventListener(listener, stateEvents, layoutEvents, effectsEvents, touchEvents);
		}
	}
	
	public void registerTouchEventStreamingListener(NanoleafTouchEventListener listener) {
		for (NanoleafDevice d : devices.values()) {
			d.registerTouchEventStreamingListener(listener);
		}
	}
}
