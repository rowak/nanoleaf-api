package com.github.rowak.nanoleafapi;

import org.json.JSONObject;

public class PluginEffect extends Effect {
	private Plugin plugin;
	
	public Plugin getPlugin() {
		return plugin;
	}
	
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
	
	public static PluginEffect fromJSON(JSONObject json) {
		Effect baseEffect = Effect.fromJSON(json);
		PluginEffect effect = new PluginEffect();
		effect.setName(baseEffect.getName());
		effect.setEffectType(baseEffect.getEffectType());
		effect.setVersion(baseEffect.getVersion());
		effect.setColorType(baseEffect.getColorType());
		effect.setPalette(baseEffect.getPalette());
		effect.setPlugin(Plugin.fromJSON(json));
		return effect;
	}
	
	public static PluginEffect fromJSON(String json) {
		return fromJSON(new JSONObject(json));
	}
	
	@Override
	public String toString() {
		return toJSON().toString();
	}
}
