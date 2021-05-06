package io.github.rowak.nanoleafapi;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;

/**
 * The Palette class is a small convenience class for a collection of colors.
 * This class is simply a wrapper for a List.
 */
public class Palette {
	
	private List<Color> colors;

	/**
	 * Creates a new palette from an existing list of colors.
	 * 
	 * @param colors   an existing list of colors
	 */
	public Palette(List<Color> colors) {
		this.colors = new ArrayList<Color>();
		if (colors != null) {
			this.colors.addAll(colors);
		}
	}
	
	/**
	 * A simple builder to make the construction of palette objects
	 * look a little nicer.
	 */
	public static class Builder {
		
		private List<Color> colors;
		
		/**
		 * Creates a new palette builder.
		 */
		public Builder() {
			colors = new ArrayList<Color>();
		}
		
		/**
		 * Adds a color to the palette.
		 * 
		 * @param color   the color to add
		 * @return        the current palette builder
		 */
		public Builder addColor(Color color) {
			colors.add(color);
			return this;
		}
		
		/**
		 * Builds the palette stored in the builder.
		 * 
		 * @return   a new palette
		 */
		public Palette build() {
			return new Palette(colors);
		}
	}
	
	/**
	 * Creates an empty palette.
	 */
	public Palette() {
		this(null);
	}
	
	/**
	 * Gets the list of colors from the palette.
	 * 
	 * @return   the palette colors
	 */
	public List<Color> getColors() {
		ArrayList<Color> temp = new ArrayList<Color>();
		temp.addAll(colors);
		return temp;
	}
	
	/**
	 * Appends a color to the palette.
	 * 
	 * @param color   the new color
	 */
	public void addColor(Color color) {
		colors.add(color);
	}
	
	/**
	 * Removes a color from the palette.
	 * 
	 * @param color   the color to remove
	 * @return        true, if the operation is successful or
	 *                false if the color can't be removed
	 */
	public boolean removeColor(Color color) {
		return colors.remove(color);
	}
	
	/**
	 * Removes a color at an index.
	 * @param i   the index to remove at
	 * @return    true, if the operation is successful or
	 *            false if the color can't be removed
	 */
	public boolean removeColorAt(int i) {
		return colors.remove(i) != null;
	}
	
	/**
	 * Sets the color at an index.
	 * @param i       the index to set
	 * @param color   the new color
	 * @return        true, if the operation is successful or
	 *                false if the color can't be set
	 */
	public boolean setColor(int i, Color color) {
		return colors.set(i, color) != null;
	}
	
	/**
	 * Converts the palette to JSON.
	 * @return   the palette as a JSON array
	 */
	public JSONArray toJSON() {
		JSONArray json = new JSONArray();
		for (int i = 0; i < colors.size(); i++) {
			json.put(colors.get(i).toJSON());
		}
		return json;
	}

	/**
	 * Creates a palette from JSON.
	 * 
	 * @param json   the JSON palette
	 * @return       a new palette
	 */
	public static Palette fromJSON(String json) {
		Palette palette = new Palette();
		JSONArray arr = new JSONArray(json);
		for (int i = 0; i < arr.length(); i++) {
			palette.addColor(Color.fromJSON(arr.getJSONObject(i)));
		}
		return palette;
	}
	
	@Override
	public String toString() {
		return colors.toString();
	}
	
//	@Override
//	public boolean equals(Object obj) {
//		
//	}
}
