package com.github.rowak.nanoleafapi.event;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

public class StateEvent extends Event {
	
	/** Event attribute that is sent when the device is turned on or off.
	 *  The event value will be a boolean value. */
	public static final int ON_ATTRIBUTE = 1;
	
	/** Event attribute that is sent when the device brightness changes.
	 *  The event value will be an integer. */
	public static final int BRIGHTNESS_ATTRIBUTE = 2;
	
	/** Event attribute that is sent when the device hue changes.
	 *  The event value will be an integer. */
	public static final int HUE_ATTRIBUTE = 3;
	
	/** Event attribute that is sent when the device saturation changes.
	 *  The event value will be an integer. */
	public static final int SATURATION_ATTRIBUTE = 4;
	
	/** Event attribute that is sent when the device color temperature changes.
	 *  The event value will be an integer. */
	public static final int CCT_ATTRIBUTE = 5;
	
	/** Event attribute that is sent when the device color mode changes.
	 *  The event value will be an integer. */
	public static final int COLORMODE_ATTRIBUTE = 6;
	
	private StateEvent(int attribute, Object value) {
		super(attribute, value);
	}
	
	public static StateEvent fromJSON(JSONObject json) {
		int attribute = json.getInt("attr");
		Object value = json.get("value");
		return new StateEvent(attribute, value);
	}
//	
//	public JSONObject toJSON() {
//		JSONObject json = new JSONObject();
//		json.put("attr", getAttribute());
//		json.put("value", getValue());
//	}
}
