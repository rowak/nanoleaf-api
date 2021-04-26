package com.github.rowak.nanoleafapi;

import org.json.JSONObject;

/**
 * The Effect class is a major class for representing effects. It allows effects from a
 * Nanoleaf device to be easily manipulated.
 */
public class Effect {
	
	/** This is currently the latest version of the Effect specification, and the
	 *  version that this library uses. Changing the version may break things. */
	public static final String DEFAULT_VERSION = "2.0";
	
	private String name;
	private String efType;
	private String version;
	private String colorType;
	private Palette palette;
	
	/**
	 * Gets the name of the effect.
	 * 
	 * @return   the effect name
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Sets the name of the effect. Must not be "*static*", "*dynamic*",
	 * or "*solid*", as these names are reserved by Nanoleaf.
	 * 
	 * @param name   the new name for the effect
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * Gets the effect type. This will always be either "plugin",
	 * "static", or "custom".
	 * 
	 * @return   the effect type
	 */
	public String getEffectType() {
		return efType;
	}
	
	/**
	 * Sets the effect type. This must be set to "plugin",
	 * "static", or "custom".
	 * 
	 * @param type   the effect type
	 */
	public void setEffectType(String type) {
		this.efType = type;
	}
	
	/**
	 * Gets the effect version.
	 * 
	 * @return   the effect version
	 */
	public String getVersion() {
		return version;
	}
	
	/**
	 * Sets the effect version. This should usually be the same as
	 * {@link Effect#DEFAULT_VERSION}.
	 * 
	 * @param version   the effect version
	 */
	public void setVersion(String version) {
		this.version = version;
	}
	
	/**
	 * Gets the color type of the effect. This will either be
	 * "HSB" or "RGB", although it will almost always be HSB.
	 * 
	 * @return   the effect color type
	 */
	public String getColorType() {
		return colorType;
	}
	
	/**
	 * Sets the color type of the effect. Must be set to either
	 * "HSB" or "RGB". This will almost always be set to HSB.
	 * 
	 * @param colorType   the effect color type
	 */
	public void setColorType(String colorType) {
		this.colorType = colorType;
	}
	
	/**
	 * Gets the palette of the effect.
	 * 
	 * @return   the effect palette
	 */
	public Palette getPalette() {
		return palette;
	}
	
	/**
	 * Sets the palette for the effect (required).
	 * 
	 * @param palette   a new palette
	 */
	public void setPalette(Palette palette) {
		this.palette = palette;
	}
	
	/**
	 * Converts the effect to JSON and adds a WRITE command to the object,
	 * for use with the {@link NanoleafDevice#writeEffect} method.
	 * 
	 * @param command   the command type
	 * @return          the effect JSON with a command
	 */
	public JSONObject toJSON(String command) {
		JSONObject json = toJSON();
		json.put("command", command);
		return json;
	}
	
	/**
	 * Converts the effect to JSON.
	 * 
	 * @return   the effect JSON
	 */
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
	
	/**
	 * Creates a basic effect from JSON. This method really shouldn't be used
	 * because it only initializes the effect with basic info.
	 * {@link Effect#createFromJSON} should be used instead.
	 * 
	 * @param json   the effect JSON
	 * @return       a new effect
	 */
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
		else {
			effect.version = DEFAULT_VERSION;
		}
		if (json.has("colorType")) {
			effect.colorType = json.getString("colorType");
		}
		if (json.has("palette")) {
			effect.palette = Palette.fromJSON(json.getJSONArray("palette").toString());
		}
		else {
			effect.palette = new Palette();
		}
		
		return effect;
	}
	
	/**
	 * Creates an effect from JSON. Effect properties the specific type
	 * of effect will be initialized for plugin, static, and custom effects.
	 * 
	 * @param json   the effect JSON
	 * @return       a new effect
	 */
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
	
	/**
	 * Creates an effect from JSON. Effect properties the specific type
	 * of effect will be initialized for plugin, static, and custom effects.
	 * 
	 * @param json   the effect JSON
	 * @return       a new effect
	 */
	public static Effect createFromJSON(String json) {
		return createFromJSON(new JSONObject(json));
	}
	
	/**
	 * Creates a basic effect from JSON. This method really shouldn't be used
	 * because it only initializes the effect with basic info.
	 * {@link Effect#createFromJSON} should be used instead.
	 * 
	 * @param json   the effect JSON
	 * @return       a new effect
	 */
	public static Effect fromJSON(String json) {
		return fromJSON(new JSONObject(json));
	}
	
	@Override
	public String toString() {
		return toJSON().toString();
	}
}
