package io.github.rowak.nanoleafapi.event;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

/**
 * Listens for changes to the state of the Nanoleaf device, such as changes
 * in brightness.
 */
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
		int attribute = UNKNOWN_ATTRIBUTE;
		Object value = null;
		if (json.has("attr")) {
			attribute = json.getInt("attr");
		}
		if (json.has("value")) {
			value = json.get("value");
		}
		return new StateEvent(attribute, value);
	}
	
	/**
	 * <p>Gets the attribute of the event.</p>
	 * 
	 * <p>This will return one of the following:
	 * <ul>
	 * <li>{@link StateEvent#ON_ATTRIBUTE}</li>
	 * <li>{@link StateEvent#BRIGHTNESS_ATTRIBUTE}</li>
	 * <li>{@link StateEvent#HUE_ATTRIBUTE}</li>
	 * <li>{@link StateEvent#SATURATION_ATTRIBUTE}</li>
	 * <li>{@link StateEvent#CCT_ATTRIBUTE}</li>
	 * <li>{@link StateEvent#COLORMODE_ATTRIBUTE}</li>
	 * </ul></p>
	 * 
	 * @return   the event attribute
	 */
	@Override
	public int getAttribute() {
		return super.getAttribute();
	}
	
//	
//	public JSONObject toJSON() {
//		JSONObject json = new JSONObject();
//		json.put("attr", getAttribute());
//		json.put("value", getValue());
//	}
	
	@Override
	public String toString() {
		switch (getAttribute()) {
			case ON_ATTRIBUTE: return "ON: " + getValue();
			case BRIGHTNESS_ATTRIBUTE: return "BRIGHTNESS: " + getValue();
			case HUE_ATTRIBUTE: return "HUE: " + getValue();
			case SATURATION_ATTRIBUTE: return "SATURATION: " + getValue();
			case CCT_ATTRIBUTE: return "COLOR TEMP: " + getValue();
			case COLORMODE_ATTRIBUTE: return "COLOR MODE: " + getValue();
			default: return "UNKNOWN";
		}
	}
}
