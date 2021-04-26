package com.github.rowak.nanoleafapi;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Plugins define how plugin effect are rendered. They can't be created directly,
 * but existing plugins can be modified by changing their properties (also called
 * plugin options).
 */
public class Plugin {
	
	private String type;
	private String uuid;
	private JSONArray options;
	
	/**
	 * Gets the plugin type.
	 * 
	 * @return   the plugin type
	 */
	public String getType() {
		return type;
	}
	
	/**
	 * Sets the plugin type.
	 * 
	 * @param type   the plugin type
	 */
	public void setType(String type) {
		this.type = type;
	}
	
	/**
	 * Gets the unique plugin UUID. This is a unique identifier for each plugin
	 * that is stored by Nanoleaf.
	 * 
	 * @return   the plugin UUID
	 */
	public String getUUID() {
		return uuid;
	}
	
	/**
	 * Sets the unique plugin UUID. This is a unique identifier for each plugin
	 * that is stored by Nanoleaf.
	 * 
	 * @param uuid   the plugin UUID
	 */
	public void setUUID(String uuid) {
		this.uuid = uuid;
	}
	
	/**
	 * Gets the plugin options.
	 * 
	 * @return   the plugin options
	 */
	public Map<String, Object> getOptions() {
		Map<String, Object> optionsMap = new HashMap<String, Object>();
		for (int i = 0; i < options.length(); i++) {
			JSONObject obj = options.getJSONObject(i);
			optionsMap.put(obj.getString("name"), obj.get("value"));
		}
		return optionsMap;
	}
	
	/**
	 * Gets the plugin options as JSON.
	 * 
	 * @return   the plugin options JSON
	 */
	public JSONArray getOptionsJSON() {
		return options;
	}
	
	/**
	 * Sets new plugin options for the plugin.
	 * 
	 * @param options   the new plugion options
	 */
	public void setOptions(Map<String, Object> options) {
		this.options = new JSONArray();
		for (String key : options.keySet()) {
			JSONObject obj = new JSONObject();
			obj.put("name", key);
			obj.put("value", options.get(key));
			this.options.put(obj);
		}
	}
	
	/**
	 * Adds a new plugin option.
	 * 
	 * @param name    the plugin option identifier (name)
	 * @param value   the plugin option value
	 */
	public void putOption(String name, Object value) {
		for (int i = 0; i < options.length(); i++) {
			JSONObject obj = options.getJSONObject(i);
			if (obj.getString("name").equals(name)) {
				obj.put("value", value);
				return; // Option updated
			}
		}
		// Create the option if it doesn't exist
		JSONObject obj = new JSONObject();
		obj.put("name", name);
		obj.put("value", value);
		options.put(obj);
	}
	
	/**
	 * Removes a plugin option.
	 * 
	 * @param name   the plugin option identifier (name)
	 * @return       true, if the option is removed or false if it
	 *               couldn't be removed
	 */
	public boolean removeOption(String name) {
		for (int i = 0; i < options.length(); i++) {
			JSONObject obj = options.getJSONObject(i);
			if (obj.getString("name").equals(name)) {
				options.remove(i);
				return true;
			}
		}
		return false;
	}

	/**
	 * Converts the plugin to JSON.
	 * 
	 * @return   the plugin as JSON
	 */
	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		json.put("pluginType", type);
		json.put("pluginUuid", uuid);
		json.put("pluginOptions", options.toString());
		return json;
	}

	/**
	 * Creates a new plugin from JSON.
	 * 
	 * @param json   the plugin JSON
	 * @return       a new plugin
	 */
	public static Plugin fromJSON(JSONObject json) {
		Plugin plugin = new Plugin();
		
		if (json.has("pluginType")) {
			plugin.type = json.getString("pluginType");
		}
		if (json.has("pluginUuid")) {
			plugin.uuid = json.getString("pluginUuid");
		}
		if (json.has("pluginOptions")) {
			plugin.options = json.getJSONArray("pluginOptions");
		}
		return plugin;
	}
}
