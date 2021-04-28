package io.github.rowak.nanoleafapi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

/**
 * <p>Custom effects allow for highly customizable animation to be designed. A custom effect
 * is made up of a sequence of frames defined for each panel, where each frame has a color
 * and a set time that it takes to transition to that color.</p>
 * 
 * <p>The CustomEffect class provides methods for creating and interacting with
 * custom effects.</p>
 */
public class CustomEffect extends Effect {
	
	protected static final String CUSTOM_ANIM_TYPE = "custom";
	
	private String animData;
	private boolean loop;
	
	/**
	 * Initializes an empty custom effect.
	 */
	public CustomEffect() {
		setEffectType(CUSTOM_ANIM_TYPE);
		setVersion(DEFAULT_VERSION);
	}
	
	/**
	 * Creates a new custom effect. Animation data must be created without the API.
	 * Refer to the <a href = "https://forum.nanoleaf.me/docs#_sh5xwlxaz1pa">
	 * OpenAPI documentation (section 3.2.6.1)</a> for more information.
	 * 
	 * @param name       the desired name of the new effect
	 * @param animData   the animation data for the effect
	 * @param loop       whether the effect should loop or not
	 * @return           a new custom effect
	 */
	public static CustomEffect createCustomEffect(String name,
			String animData, boolean loop) {
		CustomEffect ef = new CustomEffect();
		ef.setName(name);
		ef.setEffectType(CUSTOM_ANIM_TYPE);
		ef.setAnimationData(animData);
		ef.setLoopEnabled(loop);
		ef.setPalette(new Palette()); // unused, but required??
		return ef;
	}
	
	/**
	 * Returns the raw animation data string.
	 * 
	 * @return   the animation data
	 */
	public String getAnimationData() {
		return animData;
	}
	
	/**
	 * Sets the raw animation data string.
	 * 
	 * @param animData   the animation data
	 */
	public void setAnimationData(String animData) {
		this.animData = animData;
	}
	
	/**
	 * Checks whether or not the effect will loop when displayed
	 * on a device.
	 * 
	 * @return   true, if loop is enabled, or false if loop is disabled
	 */
	public boolean isLoopEnabled() {
		return loop;
	}
	
	/**
	 * Enables or disables looping for the effect.
	 * 
	 * @param loop   whether looping should be enabled or not
	 */
	public void setLoopEnabled(boolean loop) {
		this.loop = loop;
	}
	
	@Override
	public JSONObject toJSON() {
		JSONObject json = super.toJSON();
		json.put("animType", getEffectType());
		json.put("animData", animData);
		json.put("loop", loop);
		json.put("palette", getPalette().toJSON());
		return json;
	}
	
	/**
	 * Creates a new custom effect from JSON.
	 * 
	 * @param json   the JSON object containing a custom effect
	 * @return       a new custom effect
	 */
	public static CustomEffect fromJSON(JSONObject json) {
		Effect baseEffect = Effect.fromJSON(json);
		CustomEffect effect = new CustomEffect();
		effect.setName(baseEffect.getName());
		effect.setEffectType(CUSTOM_ANIM_TYPE);
		effect.setVersion(baseEffect.getVersion());
		effect.setColorType(baseEffect.getColorType());
		effect.setPalette(baseEffect.getPalette());
		if (json.has("animData")) {
			effect.setAnimationData(json.getString("animData"));
		}
		if (json.has("loop")) {
			effect.setLoopEnabled(json.getBoolean("loop"));
		}
		return effect;
	}
	
	/**
	 * The custom effect builder makes it easy to programmatically design custom effects.
	 * Frames can be added to panels individually in no particular order.
	 */
	public static class Builder {
		
		private List<Panel> panels;
		private Map<Integer, List<Frame>> frames;
		
		/**
		 * Creates a new custom effect builder.
		 * 
		 * @param device               the target device that the effect will be displayed on
		 * @throws NanoleafException   If the access token is invalid
		 */
		public Builder(NanoleafDevice device)
				throws NanoleafException, IOException {
			panels = device.getPanels();
			frames = new HashMap<Integer, List<Frame>>();
			for (Panel panel : panels)
				frames.put(panel.getId(), new ArrayList<Frame>());
		}
		
		/**
		 * Creates a new custom effect builder without requesting panel data
		 * from the device.
		 * 
		 * @param panels  the panel data
		 */
		public Builder(List<Panel> panels) {
			this.panels = panels;
			frames = new HashMap<Integer, List<Frame>>();
			for (Panel panel : panels)
				frames.put(panel.getId(), new ArrayList<Frame>());
		}
		
		/**
		 * <p>Asynchronously creates a new custom effect builder.</p>
		 * 
		 * <p>The callback status will return {@link NanoleafCallback#SUCCESS} on success,
		 * or {@link NanoleafCallback#UNAUTHORIZED} if the access token is invalid. If an
		 * internal API error occurs, it will instead return
		 * {@link NanoleafCallback#FAILURE}.</p>
		 * 
		 * @param device     the target device that the effect will be displayed on
		 * @param callback   the callback to receive the builder
		 */
		public static void createBuilderAsync(NanoleafDevice device, NanoleafCallback<Builder> callback) {
			device.getPanelsAsync((status, data, deviceCaller) -> {
				if (status != NanoleafCallback.SUCCESS) {
					callback.onCompleted(NanoleafCallback.FAILURE, null, deviceCaller);
				}
				else {
					callback.onCompleted(status, new Builder(data), deviceCaller);
				}
			});
		}
		
		/**
		 * Gets a map of the frames in this effect. The key represents the panel
		 * and the value represents a list of the frames for the corresponding panel.
		 * 
		 * @return  a map of the frames for this effect
		 */
		public Map<Integer, List<Frame>> getFrames() {
			return frames;
		}
		
		/**
		 * Creates a new custom effect using the animation data from the effect builder.
		 * 
		 * @param effectName           the desired effect name
		 * @param loop                 whether or not the effect will loop
		 * @return                     a new custom effect
		 * @throws NanoleafException   If the access token is invalid
		 */
		public CustomEffect build(String effectName, boolean loop)
				throws NanoleafException, IOException {
			int numPanels = 0;
			for (Panel p : panels) {
				if (frames.get(p.getId()).size() > 0) {
					numPanels++;
				}
			}
			StringBuilder data = new StringBuilder();
			data.append(numPanels);
			for (int i = 0; i < panels.size(); i++) {
				Panel panel = panels.get(i);
				int numFrames = frames.get(panel.getId()).size();
				if (numFrames > 0) {
					data.append(" " + panel.getId() + " " + numFrames);
					
					for (int j = 0; j < numFrames; j++) {
						Frame frame = frames.get(panel.getId()).get(j);
						data.append(" " +
									frame.getRed() + " " +
									frame.getGreen() + " " +
									frame.getBlue() + " " +
									0 + " " +
									frame.getTransitionTime());
					}
				}
			}
			return createCustomEffect(effectName, data.toString(), loop);
		}
		
		/**
		 * Adds a frame to all panels in the effect.
		 * 
		 * @param frame   the RGB color and transition time
		 * @return        the current builder
		 */
		public Builder addFrameToAllPanels(Frame frame) {
			for (Panel p : panels) {
				frames.get(p.getId()).add(frame);
			}
			return this;
		}
		
		/**
		 * Adds a new frame (RGB color and transition time) to the effect.
		 * @param panel  the panel to add the frame to
		 * @param frame  the RGB color and transition time
		 * @return  the current builder
		 */
		public Builder addFrame(Panel panel, Frame frame) {
			return addFrame(panel.getId(), frame);
		}
		
		/**
		 * Adds a new frame (RGB color and transition time) to the effect.
		 * 
		 * @param panelId   the panelId of the panel to add the frame to
		 * @param frame     the RGB color and transition time
		 * @return          the current builder
		 */
		public Builder addFrame(int panelId, Frame frame) {
			if (panelIdIsValid(panelId)) {
				frames.get(panelId).add(frame);
			}
			else {
				throw new IllegalArgumentException("Panel with id " +
						panelId + " does not exist.");
			}
			return this;
		}
		
		/**
		 * Removes a frame (RGB color and transition time) from the effect.
		 * 
		 * @param panel   the panel to add to add the frame to
		 * @param frame   the RGB color and transition time
		 * @return        the current builder
		 */
		public Builder removeFrame(Panel panel, Frame frame) {
			return removeFrame(panel.getId(), frame);
		}
		
		/**
		 * Removes a frame (RGB color and transition time) from the effect.
		 * 
		 * @param panelId   the panelId of the panel to add to add the frame to
		 * @param frame     the RGB color and transition time
		 * @return          the current builder
		 */
		public Builder removeFrame(int panelId, Frame frame) {
			if (panelIdIsValid(panelId)) {
				frames.get(panelId).remove(frame);
			}
			else {
				throw new IllegalArgumentException("Panel with id " +
						panelId + " does not exist.");
			}
			return this;
		}
		
		private boolean panelIdIsValid(int panelId) {
			for (Panel p : panels) {
				if (p.getId() == panelId) {
					return true;
				}
			}
			return false;
		}
	}
}
