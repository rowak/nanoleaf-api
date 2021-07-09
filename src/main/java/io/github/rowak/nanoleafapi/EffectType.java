package io.github.rowak.nanoleafapi;

/**
 * There are three main types of effects: Plugin, Custom, and Static.
 */
public enum EffectType {
	
	/**
	 * Plugin effects are the most common type of effect. The plugin (also called motion)
	 * describes how the base effect is rendered, and plugin options allow some modifications
	 * to be made, such as speed and direction.
	 */
	PLUGIN,
	
	/**
	 * <p>Custom effects allow for highly customizable animation to be designed. A custom effect
	 * is made up of a sequence of frames defined for each panel, where each frame has a color
	 * and a set time that it takes to transition to that color.</p>
	 * 
	 * <p>The CustomEffect class provides methods for creating and interacting with
	 * custom effects.</p>
	 */
	CUSTOM,
	
	/**
	 * <p>Static effects are a subset of custom effects; they are effects with no motion.
	 * A static effect is made up of frame definitions for each panel, where each frame has
	 * a color and a set time that it takes to transition to that color. Each panel can only
	 * have one frame.</p>
	 * 
	 * <p>The StaticEffect class provides methods for creating and interacting with
	 * static effects.</p>
	 */
	STATIC
}
