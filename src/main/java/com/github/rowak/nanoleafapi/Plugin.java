package com.github.rowak.nanoleafapi;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

public class Plugin {
	
	private String type;
	private String uuid;
	private JSONArray options;
	
	public String getType() {
		return type;
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
	public String getUUID() {
		return uuid;
	}
	
	public void setUUID(String uuid) {
		this.uuid = uuid;
	}
	
	public Map<String, Object> getOptions() {
		Map<String, Object> optionsMap = new HashMap<String, Object>();
		for (int i = 0; i < options.length(); i++) {
			JSONObject obj = options.getJSONObject(i);
			optionsMap.put(obj.getString("name"), obj.get("value"));
		}
		return optionsMap;
	}
	
	public JSONArray getOptionsJSON() {
		return options;
	}
	
	public void setOptions(Map<String, Object> options) {
		this.options = new JSONArray();
		for (String key : options.keySet()) {
			JSONObject obj = new JSONObject();
			obj.put("name", key);
			obj.put("value", options.get(key));
			this.options.put(obj);
		}
	}
	
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

	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		json.put("pluginType", type);
		json.put("pluginUuid", uuid);
		json.put("pluginOptions", options.toString());
		return json;
	}

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
