package com.github.rowak.nanoleafapi;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;

public class Palette {
	
	private List<Color> colors;

	public Palette() {
		colors = new ArrayList<Color>();
	}
	
	public List<Color> getColors() {
		ArrayList<Color> temp = new ArrayList<Color>();
		temp.addAll(colors);
		return temp;
	}
	
	public void addColor(Color color) {
		colors.add(color);
	}
	
	public boolean removeColor(Color color) {
		return colors.remove(color);
	}
	
	public boolean setColor(int i, Color color) {
		return colors.set(i, color) != null;
	}
	
	public JSONArray toJSON() {
		JSONArray json = new JSONArray();
		for (int i = 0; i < colors.size(); i++) {
			json.put(colors.get(i).toJSON());
		}
		return json;
	}

	public static Palette fromJSON(String json) {
		Palette palette = new Palette();
		JSONArray arr = new JSONArray(json);
		for (int i = 0; i < arr.length(); i++) {
			palette.addColor(Color.fromJSON(arr.getJSONObject(i)));
		}
		return palette;
	}
}
