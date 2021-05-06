package io.github.rowak.nanoleafapi;

import org.json.JSONObject;

/**
 * Plugin effects are the most common type of effect. The plugin (also called motion)
 * describes how the base effect is rendered, and plugin options allow some modifications
 * to be made, such as speed and direction.
 */
public class PluginEffect extends Effect {
	
	private Plugin plugin;
	
	/**
	 * Gets the plugin associated with the effect.
	 * 
	 * @return   the effect plugin
	 */
	public Plugin getPlugin() {
		return plugin;
	}
	
	/**
	 * Sets the plugin associated with the effect.
	 * 
	 * @param plugin   the new effect plugin
	 */
	public void setPlugin(Plugin plugin) {
		this.plugin = plugin;
	}
	
	public JSONObject toJSON(String command) {
		JSONObject json = toJSON();
		json.put("command", command);
		return json;
	}
	

	public JSONObject toJSON() {
		JSONObject json = super.toJSON();
		
		json.put("pluginType", plugin.getType());
		json.put("pluginUuid", plugin.getUUID());
		json.put("pluginOptions", plugin.getOptionsJSON());
		
		return json;
	}
	
	/**
	 * Creates a plugin effect from JSON.
	 * 
	 * @param json   the effect JSON
	 * @return       a new plugin effect
	 */
	public static PluginEffect fromJSON(JSONObject json) {
		Effect baseEffect = Effect.fromJSON(json);
		PluginEffect effect = new PluginEffect();
		effect.setName(baseEffect.getName());
		effect.setEffectType(baseEffect.getEffectType());
		effect.setVersion(baseEffect.getVersion());
		effect.setPalette(baseEffect.getPalette());
		effect.setPlugin(Plugin.fromJSON(json));
		return effect;
	}
	
	/**
	 * Creates a plugin effect from JSON.
	 * 
	 * @param json   the effect JSON
	 * @return       a new plugin effect
	 */
	public static PluginEffect fromJSON(String json) {
		return fromJSON(new JSONObject(json));
	}
	
	@Override
	public String toString() {
		return toJSON().toString();
	}
}
