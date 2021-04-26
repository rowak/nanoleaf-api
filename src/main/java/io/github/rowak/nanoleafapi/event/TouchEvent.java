package io.github.rowak.nanoleafapi.event;

import org.json.JSONObject;

public class TouchEvent extends Event {
	
	protected TouchEvent(int attribute, Object value) {
		super(attribute, value);
		// TODO Auto-generated constructor stub
	}

	public static TouchEvent fromJSON(JSONObject json) {
		return null;
	}
//	
//	public JSONObject toJSON() {
//		
//	}
}
