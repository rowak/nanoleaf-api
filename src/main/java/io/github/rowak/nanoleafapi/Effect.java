package io.github.rowak.nanoleafapi;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * The Effect class is a major class for representing effects. It allows effects from a
 * Nanoleaf device to be easily manipulated.
 */
public class Effect {
	
	/** This is currently the latest version of the Effect specification, and the
	 *  version that this library uses. Changing the version may break things. */
	public static final String DEFAULT_VERSION = "2.0";
	
	/** As far as I know, the color type should always be HSB. */
	public static final String DEFAULT_COLOR_TYPE = "HSB";
	
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
	 * Gets the color type for the effect. This will almost always
	 * be set to "HSB" (hue-saturation-brightness).
	 * 
	 * @return   the effect color type
	 */
	public String getColorType() {
		return colorType;
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
		if (version != null) {
			json.put("version", version);
		}
		else {
			json.put("version", DEFAULT_VERSION);
		}
		json.put("animName", name);
		json.put("animType", efType);
		if (colorType != null) {
			json.put("colorType", colorType);
		}
		else {
			json.put("colorType", DEFAULT_COLOR_TYPE);
		}
		if (palette != null) {
			json.put("palette", palette.toJSON());
		}
		else {
			json.put("palette", new Palette().toJSON());
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
		else {
			effect.colorType = DEFAULT_COLOR_TYPE;
		}
		if (json.has("palette")) {
			try {
				// The palette may be null
				effect.palette = Palette.fromJSON(json.getJSONArray("palette").toString());
			} catch (JSONException e) {
				effect.palette = new Palette();
			}
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
	public static final Effect createFromJSON(JSONObject json) {
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
	public static final Effect createFromJSON(String json) {
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
		if (json == null) {
			return null;
		}
		return fromJSON(new JSONObject(json));
	}
	
	@Override
	public String toString() {
		return toJSON().toString();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null || obj.getClass() != this.getClass()) {
			return false;
		}
		Effect other = (Effect)obj;
		return objEquals(this.name, other.name) && objEquals(this.efType, other.efType) &&
				objEquals(this.version, other.version) && objEquals(this.colorType, other.colorType) &&
				this.palette.equals(other.palette);
	}
	
	private boolean objEquals(Object obj1, Object obj2) {
		if (obj1 == obj2) {
			return true;
		}
		else if (obj1 == null || obj2 == null) {
			return false;
		}
		return obj1.equals(obj2);
	}
}
