package io.github.rowak.nanoleafapi.event;

import java.util.Arrays;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Listens for changes to either the effect currently being displayed,
 * or changes to the effects installed on the Nanoleaf device.
 */
public class EffectsEvent extends Event {
	
	public static final int SELECTED_EFFECT_ATTRIBUTE = 1;
	public static final int EFFECTS_LIST_ATTRIBUTE = 2;
	
	protected EffectsEvent(int attribute, Object value) {
		super(attribute, value);
	}

	public static EffectsEvent fromJSON(JSONObject json) {
		int attribute = UNKNOWN_ATTRIBUTE;
		Object value = null;
		if (json.has("attr")) {
			attribute = json.getInt("attr");
		}
		if (json.has("value")) {
			if (attribute == SELECTED_EFFECT_ATTRIBUTE) {
				value = json.get("value");
			}
			else if (attribute == EFFECTS_LIST_ATTRIBUTE) {
				JSONArray arr = json.getJSONArray("value");
				String[] effects = new String[arr.length()];
				for (int i = 0; i < arr.length(); i++) {
					effects[i] = arr.getString(i);
				}
				value = effects;
			}
		}
		return new EffectsEvent(attribute, value);
	}
	
	/**
	 * <p>Gets the attribute of the event.</p>
	 * 
	 * <p>This will return one of the following:
	 * <ul>
	 * <li>{@link EffectsEvent#SELECTED_EFFECT_ATTRIBUTE}</li>
	 * <li>{@link EffectsEvent#EFFECTS_LIST_ATTRIBUTE}</li>
	 * </ul></p>
	 * 
	 * @return   the event attribute
	 */
	@Override
	public int getAttribute() {
		// This exists purely to override the javadoc
		return super.getAttribute();
	}
	
	@Override
	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		json.put("attr", getAttribute());
		JSONArray arr = new JSONArray();
		String[] effects = (String[])getValue();
		for (int i = 0; i < effects.length; i++) {
			arr.put(effects[i]);
		}
		json.put("value", effects);
		return json;
	}
	
	@Override
	public String toString() {
		switch (getAttribute()) {
			case SELECTED_EFFECT_ATTRIBUTE: return "SELECTED EFFECT " + getValue();
			case EFFECTS_LIST_ATTRIBUTE: return "EFFECTS LIST " + Arrays.asList((String[])getValue());
			default: return "UNKNOWN";
		}
	}
}
