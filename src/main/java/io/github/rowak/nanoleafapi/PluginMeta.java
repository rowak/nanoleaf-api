package io.github.rowak.nanoleafapi;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * A simple convenience class for storing detailed plugin metadata retrieved
 * from a NanoleafDevice.
 */
public class PluginMeta {
	
	private PluginMeta() {}
	
	private String name;
	private String uuid;
	private String description;
	private String author;
	private String type;
	private List<String> tags;
	private List<String> features;
	private JSONArray config;

	/**
	 * Gets the name of the plugin.
	 * 
	 * @return   the plugin name
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Gets the UUID of the plugin.
	 * 
	 * @return   the plugin UUID
	 */
	public String getUUID() {
		return uuid;
	}
	
	/**
	 * Gets the plugin description.
	 * 
	 * @return   the plugin description
	 */
	public String getDescription() {
		return description;
	}
	
	/**
	 * Gets the author of the plugin.
	 * 
	 * @return   the plugin author
	 */
	public String getAuthor() {
		return author;
	}
	
	/**
	 * Gets the plugin type.
	 * 
	 * @return   the plugin type
	 */
	public String getType() {
		return type;
	}
	
	/**
	 * Gets the category tags for the plugin.
	 * 
	 * @return   the plugin tags
	 */
	public List<String> getTags() {
		return tags;
	}
	
	/**
	 * Gets the features for the plugin.
	 * Features may include touch or rhythm.
	 * 
	 * @return   the plugin features
	 */
	public List<String> getFeatures() {
		return features;
	}
	
//	public List<Map<String, Object>> getConfig() {
//		
//	}
	
	/**
	 * Creates a plugin metadata object from JSON.
	 * 
	 * @param json   the plugin metadata JSON
	 * @return       a new plugin metadata object
	 */
	public static PluginMeta fromJSON(JSONObject json) {
		PluginMeta meta = new PluginMeta();
		if (json.has("uuid")) {
			meta.uuid = json.getString("uuid");
		}
		if (json.has("name")) {
			meta.name = json.getString("name");
		}
		if (json.has("description")) {
			meta.description = json.getString("description");
		}
		if (json.has("author")) {
			meta.author = json.getString("author");
		}
		if (json.has("type")) {
			meta.type = json.getString("type");
		}
		meta.tags = new ArrayList<String>();
		if (json.has("tags")) {
			JSONArray tags = json.getJSONArray("tags");
			for (int i = 0; i < tags.length(); i++) {
				meta.tags.add(tags.getString(i));
			}
		}
		meta.features = new ArrayList<String>();
		if (json.has("features")) {
			JSONArray features = json.getJSONArray("features");
			for (int i = 0; i < features.length(); i++) {
				meta.features.add(features.getString(i));
			}
		}
		if (json.has("pluginConfig")) {
			meta.config = json.getJSONArray("pluginConfig");
		}
		return meta;
	}
	
	/**
	 * Converts the plugin metadata to JSON.
	 * 
	 * @return   the plugin metadata as JSON
	 */
	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		if (uuid != null) {
			json.put("uuid", uuid);
		}
		if (name != null) {
			json.put("name", name);
		}
		if (description != null) {
			json.put("description", description);
		}
		if (author != null) {
			json.put("author", author);
		}
		if (type != null) {
			json.put("type", type);
		}
		if (tags != null) {
			json.put("tags", listToJSON(tags));
		}
		if (features != null) {
			json.put("features", listToJSON(features));
		}
		if (config != null) {
			json.put("pluginConfig", config);
		}
		return json;
	}
	
	private JSONArray listToJSON(List<String> list) {
		JSONArray json = new JSONArray();
		for (String s : list) {
			json.put(s);
		}
		return json;
	}
	
	@Override
	public String toString() {
		return toJSON().toString();
	}
}
