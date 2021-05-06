package io.github.rowak.nanoleafapi.event;

import org.json.JSONObject;

/**
 * Listens for physical interactions with the Nanoleaf panels, such as
 * single/double taps and swipes.
 */
public class TouchEvent extends Event {
	
	/** Triggered when a panel is tapped briefly. */
	public static final int SINGLE_TAP = 0;
	
	/** Triggered when a panel is tapped briefly twice. */
	public static final int DOUBLE_TAP = 1;
	
	/** Triggered when two or more panels are swiped upwards. The swiped
	 *  panels are not recorded, so the panel ID will be -1. */
	public static final int SWIPE_UP = 2;
	
	/** Triggered when two or more panels are swiped downwards. The swiped
	 *  panels are not recorded, so the panel ID will be -1. */
	public static final int SWIPE_DOWN = 3;
	
	/** Triggered when two or more panels are swiped left. The swiped
	 *  panels are not recorded, so the panel ID will be -1. */
	public static final int SWIPE_LEFT = 4;
	
	/** Triggered when two or more panels are swiped right. The swiped
	 *  panels are not recorded, so the panel ID will be -1. */
	public static final int SWIPE_RIGHT = 5;
	
	/** Triggered when a panel is held for about three seconds. */
	public static final int HOLD = 6;
	
	protected TouchEvent(int attribute, Object value) {
		super(attribute, value);
	}

	/**
	 * Creates a touch event from JSON.
	 * 
	 * @param json   the touch event JSON
	 * @return       a new touch event
	 */
	public static TouchEvent fromJSON(JSONObject json) {
		int attribute = UNKNOWN_ATTRIBUTE;
		Object value = null;
		if (json.has("gesture")) {
			attribute = json.getInt("gesture");
		}
		if (json.has("panelId")) {
			value = json.get("panelId");
		}
		return new TouchEvent(attribute, value);
	}
	
	/**
	 * <p>Gets the event gesture (attribute).</p>
	 * 
	 * <p>This will return one of the following:
	 * <ul>
	 * <li>{@link TouchEvent#SINGLE_TAP}</li>
	 * <li>{@link TouchEvent#DOUBLE_TAP}</li>
	 * <li>{@link TouchEvent#SWIPE_UP}</li>
	 * <li>{@link TouchEvent#SWIPE_DOWN}</li>
	 * <li>{@link TouchEvent#SWIPE_LEFT}</li>
	 * <li>{@link TouchEvent#SWIPE_RIGHT}</li>
	 * <li>{@link TouchEvent#HOLD}</li>
	 * </ul></p>
	 * 
	 * @return   the event gesture
	 */
	public int getGesture() {
		return getAttribute();
	}
	
	/**
	 * Gets the ID of the panel that was touched.
	 * 
	 * @return   the panel ID
	 */
	public int getPanelId() {
		return (int)getValue();
	}
	
	@Override
	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		json.put("gesture", getAttribute());
		json.put("panelId", getValue());
		return json;
	}
	
	@Override
	public String toString() {
		switch (getAttribute()) {
			case SINGLE_TAP: return "SINGLE TAP panel " + getValue();
			case DOUBLE_TAP: return "DOUBLE TAP panel " + getValue();
			case SWIPE_UP: return "SWIPE UP";
			case SWIPE_DOWN: return "SWIPE DOWN";
			case SWIPE_LEFT: return "SWIPE LEFT";
			case SWIPE_RIGHT: return "SWIPE RIGHT";
			case HOLD: return "HOLD panel " + getValue();
			default: return "UNKNOWN";
		}
	}
}
