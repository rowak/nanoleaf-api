package com.github.rowak.nanoleafapi;

import org.json.JSONObject;

public class Effect {
	private String name;
	private String efType;
	private String version;
	private String colorType;
	private Palette palette;
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getEffectType() {
		return efType;
	}
	
	public void setEffectType(String type) {
		this.efType = type;
	}
	
	public String getVersion() {
		return version;
	}
	
	public void setVersion(String version) {
		this.version = version;
	}
	
	public String getColorType() {
		return colorType;
	}
	
	public void setColorType(String colorType) {
		this.colorType = colorType;
	}
	
	public Palette getPalette() {
		return palette;
	}
	
	public void setPalette(Palette palette) {
		this.palette = palette;
	}
	
	public JSONObject toJSON(String command) {
		JSONObject json = toJSON();
		json.put("command", command);
		return json;
	}
	
	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		json.put("version", version);
		json.put("animName", name);
		json.put("animType", efType);
		json.put("colorType", colorType);
		if (palette != null) {
			json.put("palette", palette.toJSON());
		}
		return json;
	}
	
	public static Effect fromJSON(JSONObject json) {
		Effect effect = new Effect();
		
		if (json.has("animName")) {
			effect.name = json.getString("animName");
		}
		if (json.has("animType")) {
			effect.efType = json.getString("animType");
		}
		if (json.has("version")) {
			effect.version = json.getString("version");
		}
		if (json.has("colorType")) {
			effect.colorType = json.getString("colorType");
		}
		if (json.has("palette")) {
			effect.palette = Palette.fromJSON(json.getJSONArray("palette").toString());
		}
		
		return effect;
	}
	
	public static Effect createFromJSON(JSONObject json) {
		if (json.has("animType")) {
			String type = json.getString("animType");
			if (type.equals("plugin") || type.equals("rhythm")) {
				return PluginEffect.fromJSON(json);
			}
			else if (type.equals("custom")) {
				return CustomEffect.fromJSON(json);
			}
			else if (type.equals("static")) {
				return StaticEffect.fromJSON(json);
			}
			else {
				return Effect.fromJSON(json);
			}
		}
		return null;
	}
	
	public static Effect createFromJSON(String json) {
		return createFromJSON(new JSONObject(json));
	}
	
	public static Effect fromJSON(String json) {
		return fromJSON(new JSONObject(json));
	}
	
	@Override
	public String toString() {
		return toJSON().toString();
	}
}
