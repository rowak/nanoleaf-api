package io.github.rowak.nanoleafapi.util;

import java.util.HashMap;
import java.util.Map;

import io.github.rowak.nanoleafapi.Plugin;

/**
 * This class provides templates for some existing plugins to make effect creation easier.
 */
public class PluginTemplates {
	
	/** UUID for the Wheel legacy plugin. */
	public static final String WHEEL_UUID = "6970681a-20b5-4c5e-8813-bdaebc4ee4fa";
	
	/** UUID for the Flow legacy plugin */
	public static final String FLOW_UUID = "027842e4-e1d6-4a4c-a731-be74a1ebd4cf";
	
	/** UUID for the Explode legacy plugin */
	public static final String EXPLODE_UUID = "713518c1-d560-47db-8991-de780af71d1e";
	
	/** UUID for the Fade legacy plugin */
	public static final String FADE_UUID = "b3fd723a-aae8-4c99-bf2b-087159e0ef53";
	
	/** UUID for the Random legacy plugin */
	public static final String RANDOM_UUID = "ba632d3e-9c2b-4413-a965-510c839b3f71";
	
	/** UUID for the Highlight legacy plugin */
	public static final String HIGHLIGHT_UUID = "70b7c636-6bf8-491f-89c1-f4103508d642";
	
	/**
	 * Creates a plugin template for the Wheel legacy plugin. This plugin cycles
	 * across panels with a gradient in a given direction.
	 * 
	 * @return   a wheel plugin template
	 */
	public static Plugin getWheelTemplate() {
		Map<String, Object> options = new HashMap<String, Object>();
		options.put("linDirection", "left");
		options.put("nColorsPerFrame", 3);
		options.put("loop", true);
		options.put("transTime", 25);
		return createPlugin(WHEEL_UUID, "color", options);
	}
	
	/**
	 * Creates a plugin template for the Flow legacy plugin. This plugin mixes two
	 * or more colors into each other gradually (as if flowing paint is mixing).
	 * 
	 * @return   a flow plugin template
	 */
	public static Plugin getFlowTemplate() {
		Map<String, Object> options = new HashMap<String, Object>();
		options.put("linDirection", "left");
		options.put("delayTime", 3);
		options.put("loop", true);
		options.put("transTime", 25);
		return createPlugin(WHEEL_UUID, "color", options);
	}
	
	/**
	 * Creates a plugin template for the Explode legacy plugin. This plugin is
	 * similar to the Flow plugin, but the colors move out from the center of
	 * the panel system, rather than the edge.
	 * @return   an explode plugin template
	 */
	public static Plugin getExplodeTemplate() {
		Map<String, Object> options = new HashMap<String, Object>();
		options.put("delayTime", 3);
		options.put("loop", true);
		options.put("transTime", 25);
		return createPlugin(EXPLODE_UUID, "color", options);
	}
	
	/**
	 * Creates a plugin template for the Fade legacy plugin. This plugin cycles
	 * across panels in sync.
	 * 
	 * @return   a fade plugin template
	 */
	public static Plugin getFadeTemplate() {
		Map<String, Object> options = new HashMap<String, Object>();
		options.put("delayTime", 3);
		options.put("loop", true);
		options.put("transTime", 25);
		return createPlugin(EXPLODE_UUID, "color", options);
	}
	
	/**
	 * Creates a plugin template for the Random legacy plugin. This plugin randomly
	 * transitions between different colors and different levels of brightness.
	 * 
	 * @return   a random plugin template
	 */
	public static Plugin getRandomTemplate() {
		Map<String, Object> options = new HashMap<String, Object>();
		options.put("delayTime", 3);
		options.put("loop", true);
		options.put("transTime", 25);
		return createPlugin(RANDOM_UUID, "color", options);
	}
	
	/**
	 * Creates a plugin template for the Highlight legacy plugin. This plugin is
	 * dominated by a primary color and has one or more secondary colors that
	 * occasionally transition in and out.
	 * 
	 * @return   a highlight plugin template
	 */
	public static Plugin getHighlightTemplate() {
		Map<String, Object> options = new HashMap<String, Object>();
		options.put("mainColorProb", 0.5f);
		options.put("delayTime", 3);
		options.put("loop", true);
		options.put("transTime", 25);
		return createPlugin(HIGHLIGHT_UUID, "color", options);
	}
	
	private static Plugin createPlugin(String uuid, String type, Map<String, Object> options) {
		Plugin plugin = new Plugin();
		plugin.setUUID(uuid);
		plugin.setType(type);
		plugin.setOptions(options);
		return plugin;
	}
}
