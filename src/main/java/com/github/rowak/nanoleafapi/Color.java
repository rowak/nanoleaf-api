package com.github.rowak.nanoleafapi;

import org.json.JSONObject;

public class Color {
	private int hue;
	private int saturation;
	private int brightness;
	private float probability;
	
	/**
	 * The color black as RGB(0, 0, 0).
	 */
	public static final Color BLACK = fromRGB(0, 0, 0);
	
	/**
	 * The color white as RGB(255, 255, 255).
	 */
	public static final Color WHITE = fromRGB(255, 255, 255);
	
	/**
	 * The color red as RGB(255, 0, 0).
	 */
	public static final Color RED = fromRGB(255, 0, 0);
	
	/**
	 * The color blue as RGB(0, 255, 0).
	 */
	public static final Color GREEN = fromRGB(0, 255, 0);
	
	/**
	 * The color green as RGB(0, 0, 255).
	 */
	public static final Color BLUE = fromRGB(0, 0, 255);
	
	/**
	 * The color magenta as RGB(255, 0, 255).
	 */
	public static final Color MAGENTA = fromRGB(255, 0, 255);
	
	/**
	 * The color yellow as RGB(255, 255, 0).
	 */
	public static final Color YELLOW = fromRGB(255, 255, 0);
	
	/**
	 * The color cyan as RGB(0, 255, 255).
	 */
	public static final Color CYAN = fromRGB(0, 255, 255);
	
	/**
	 * The color gray as RGB(128, 128, 128).
	 */
	public static final Color GRAY = fromRGB(128, 128, 128);
	
	/**
	 * The color light gray as RGB(192, 192, 192).
	 */
	public static final Color LIGHT_GRAY = fromRGB(192, 192, 192);
	
	/**
	 * The color dark gray as RGB(64, 64, 64).
	 */
	public static final Color DARK_GRAY = fromRGB(64, 64, 64);
	
	/**
	 * The color pink as RGB(255, 175, 175).
	 */
	public static final Color PINK = fromRGB(255, 175, 175);
	
	/**
	 * The color orange as RGB(255, 200, 0).
	 */
	public static final Color ORANGE = fromRGB(255, 200, 0);
	
	/**
	 * Creates an HSB instance of <code>Color</code>.
	 * @param hue  the hue of the color
	 * @param saturation  the saturation of the color
	 * @param brightness  the brightness of the color
	 * @return  a new <code>Color</code>
	 */
	public static Color fromHSB(int hue, int saturation, int brightness) {
		Color color = new Color();
		color.hue = hue;
		color.saturation = saturation;
		color.brightness = brightness;
		return color;
	}
	
	/**
	 * Creates an RGB instance of <code>Color</code>.
	 * @param red  the red RGB value of the desired color
	 * @param green  the green RGB value of the desired color
	 * @param blue  the blue RGB value of the desired color
	 * @return  a new <code>Color</code>
	 */
	public static Color fromRGB(int red, int green, int blue) {
		float[] hsb = new float[3];
		java.awt.Color.RGBtoHSB(red, green, blue, hsb);
		Color color = new Color();
		color.hue = (int)(hsb[0] * 360);
		color.saturation = (int)(hsb[1] * 100);
		color.brightness = (int)(hsb[2] * 100);
		return color;
	}
	
	private java.awt.Color getRGB() {
		return new java.awt.Color(java.awt.Color.HSBtoRGB(hue/360f,
				saturation/100f, brightness/100f));
	}
	
	/**
	 * Gets the red RGB value of this color.
	 * @return  the red RGB value
	 */
	public int getRed() {
		return getRGB().getRed();
	}
	
	/**
	 * Gets the green RGB value of this color.
	 * @return  the green RGB value
	 */
	public int getGreen() {
		return getRGB().getGreen();
	}
	
	/**
	 * Gets the blue RGB value of this color.
	 * @return  the blue value
	 */
	public int getBlue() {
		return getRGB().getBlue();
	}
	
	/**
	 * Get the hue HSB value of this color.
	 * @return  the hue
	 */
	public int getHue() {
		return hue;
	}
	
	/**
	 * Set the hue HSB value of this color.
	 * @param hue  the desired hue
	 */
	public void setHue(int hue) {
		this.hue = hue;
	}
	
	/**
	 * Get the saturation HSB value of this color.
	 * @return  the saturation
	 */
	public int getSaturation() {
		return saturation;
	}
	
	/**
	 * Set the saturation HSB value of this color.
	 * @param  saturation  the saturation
	 */
	public void setSaturation(int saturation) {
		this.saturation = saturation;
	}
	
	/**
	 * Get the brightness HSB value  of this color.
	 * @return  the brightness
	 */
	public int getBrightness() {
		return brightness;
	}
	
	/**
	 * Set the brightness HSB value of this color.
	 * @param  brightness  the brightness
	 */
	public void setBrightness(int brightness) {
		this.brightness = brightness;
	}
	
	public float getProbability() {
		return probability;
	}
	
	public void setProbability(float probability) {
		this.probability = probability;
	}
	
	@Override
	public String toString() {
		return toJSON().toString();
	}
	
	/**
	 * Checks if two colors are equal based on their h,s,v, and p values.
	 * @param other  the color to compare this color to
	 * @return  true, if the colors are equal
	 */
	@Override
	public boolean equals(Object other) {
		if (other instanceof Color) {
			return this.hue == ((Color)other).hue &&
					this.saturation == ((Color)other).saturation &&
					this.brightness == ((Color)other).brightness &&
					this.probability == ((Color)other).probability;
		}
		return false;
	}

	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		json.put("hue", hue);
		json.put("saturation", saturation);
		json.put("brightness", brightness);
		json.put("probability", probability);
		return json;
	}

	public static Color fromJSON(JSONObject json) {
		Color color = new Color();
		if (!json.has("hue")) {
			throw new JSONParserException("missing hue in color");
		}
		if (!json.has("saturation")) {
			throw new JSONParserException("missing saturation in color");
		}
		if (!json.has("brightness")) {
			throw new JSONParserException("missing brightness in color");
		}
		color.setHue(json.getInt("hue"));
		color.setSaturation(json.getInt("saturation"));
		color.setBrightness(json.getInt("brightness"));
		if (json.has("probability")) {
			color.setProbability(json.getFloat("probability"));
		}
		return color;
	}
}
