package io.github.rowak.nanoleafapi.event;

import java.util.Arrays;

import org.json.JSONArray;
import org.json.JSONObject;

import io.github.rowak.nanoleafapi.Panel;

/**
 * Listens for changes to the physical layout of the Nanoleaf panels.
 */
public class LayoutEvent extends Event {

	/** Triggered when the panel layout changes (panels are added/removed/moved). */
	public static final int LAYOUT_ATTRIBUTE = 1;
	
	/** Triggered when the global orientation changes. */
	public static final int GLOBAL_ORIENTATION_ATTRIBUTE = 2;
	
	protected LayoutEvent(int attribute, Object value) {
		super(attribute, value);
		// TODO Auto-generated constructor stub
	}

	/**
	 * Creates a layout event from JSON.
	 * 
	 * @param json   the event JSON
	 * @return       a new layout event
	 */
	public static LayoutEvent fromJSON(JSONObject json) {
		int attribute = UNKNOWN_ATTRIBUTE;
		Object value = null;
		if (json.has("attr")) {
			attribute = json.getInt("attr");
		}
		if (json.has("value")) {
			if (attribute == LAYOUT_ATTRIBUTE) {
				JSONArray arr = json.getJSONObject("value").getJSONArray("positionData");
				Panel[] panels = new Panel[arr.length()];
				for (int i = 0; i < arr.length(); i++) {
					panels[i] = Panel.fromJSON(new JSONObject(arr.getString(i)));
				}
				value = panels;
			}
			else if (attribute == GLOBAL_ORIENTATION_ATTRIBUTE) {
				value = json.get("value");
			}
		}
		return new LayoutEvent(attribute, value);
	}
	
	/**
	 * <p>Gets the attribute of the event.</p>
	 * 
	 * <p>This will return one of the following:
	 * <ul>
	 * <li>{@link LayoutEvent#LAYOUT_ATTRIBUTE}</li>
	 * <li>{@link LayoutEvent#GLOBAL_ORIENTATION_ATTRIBUTE}</li>
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
	public String toString() {
		switch (getAttribute()) {
			case LAYOUT_ATTRIBUTE: return "LAYOUT " + Arrays.asList((Panel[])getValue());
			case GLOBAL_ORIENTATION_ATTRIBUTE: return "GLOBAL ORIENTATION " + getValue();
			default: return "UNKNOWN";
		}
	}
}
