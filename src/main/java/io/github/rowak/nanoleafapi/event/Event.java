package io.github.rowak.nanoleafapi.event;

import org.json.JSONObject;

public class Event {
	
	/** Event attribute that is unrecognizable by this library. You should never
	 *  receive this attribute. Please report on GitHub. */
	public static final int UNKNOWN_ATTRIBUTE = -1;
	
	private EventType type;
	private int attribute;
	private Object value;
	
	protected Event(int attribute, Object value) {
		this.attribute = attribute;
		this.value = value;
	}
	
	/**
	 * Gets the event type.
	 * @return
	 */
	public EventType getType() {
		return type;
	}
	
	/**
	 * Gets the event attribute. This is the property that was updated
	 * at the time the event occurred.
	 * @return   the event attribute
	 */
	public int getAttribute() {
		return attribute;
	}
	
	/**
	 * Gets the event value. This is the value of the property that was
	 * updated at the time the event occurred.
	 * 
	 * @return   the event value
	 */
	public Object getValue() {
		return value;
	}
	
	/**
	 * Creates an event from JSON.
	 * 
	 * @param type   the event type
	 * @param json   the event JSON
	 * @return       a new event
	 */
	public static Event fromJSON(EventType type, JSONObject json) {
		switch (type) {
			case STATE: return StateEvent.fromJSON(json);
			case LAYOUT: return LayoutEvent.fromJSON(json);
			case EFFECTS: return EffectsEvent.fromJSON(json);
			case TOUCH: return TouchEvent.fromJSON(json);
			default: return null;
		}
	}
	
	/**
	 * Converts the event to JSON.
	 * 
	 * @return   the event JSON
	 */
	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		json.put("attr", getAttribute());
		json.put("value", getValue());
		return json;
	}
}
