package com.github.rowak.nanoleafapi.event;

import java.util.Map;

import org.json.JSONObject;

public class Event {
	
	private EventType type;
	private int attribute;
//	private EffectValue values;
	private Object value;
	
	protected Event(int attribute, Object value) {
		this.attribute = attribute;
		this.value = value;
	}
	
	public EventType getType() {
		return type;
	}
	
	public int getAttribute() {
		return attribute;
	}
	
	public Object getValue() {
		return value;
	}
	
	public static Event fromJSON(EventType type, JSONObject json) {
		switch (type) {
			case STATE: return StateEvent.fromJSON(json);
			case LAYOUT: return LayoutEvent.fromJSON(json);
			case EFFECTS: return EffectsEvent.fromJSON(json);
			case TOUCH: return TouchEvent.fromJSON(json);
			default: return null;
		}
	}
	
//	public abstract JSONObject toJSON();
}
