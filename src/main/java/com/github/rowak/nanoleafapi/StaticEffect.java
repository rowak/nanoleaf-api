package com.github.rowak.nanoleafapi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

/**
 * <p>Static effects are a subset of custom effects; they are effects with no motion.
 * A static effect is made up of frame definitions for each panel, where each frame has
 * a color and a set time that it takes to transition to that color. Each panel can only
 * have one frame.</p>
 * 
 * <p>The CustomEffect class provides methods for creating and interacting with
 * static effects.</p>
 */
public class StaticEffect extends CustomEffect {
	
	private static final String STATIC_ANIM_TYPE = "static";
	
	/**
	 * Initializes an empty static effect.
	 */
	public StaticEffect() {
		setEffectType(STATIC_ANIM_TYPE);
		setVersion(DEFAULT_VERSION);
		setLoopEnabled(false);
	}
	
	/**
	 * Creates a new static effect using previously created
	 * animation data.
	 * 
	 * @param name       the desired name of the new effect
	 * @param animData   the desired animation data
	 * @return           a new static effect
	 */
	public static StaticEffect createStaticEffect(String name, String animData) {
		StaticEffect ef = new StaticEffect();
		ef.setName(name);
		ef.setEffectType(STATIC_ANIM_TYPE);
		ef.setAnimationData(animData);
		ef.setLoopEnabled(false);
		ef.setPalette(new Palette());
		return ef;
	}
	
	/**
	 * Creates a new static effect from JSON.
	 * 
	 * @param json   the JSON object containing a static effect
	 * @return       a new static effect
	 */
	public static StaticEffect fromJSON(JSONObject json) {
		Effect baseEffect = Effect.fromJSON(json);
		StaticEffect effect = new StaticEffect();
		effect.setLoopEnabled(false);
		effect.setName(baseEffect.getName());
		effect.setEffectType(STATIC_ANIM_TYPE);
		effect.setVersion(baseEffect.getVersion());
		effect.setColorType(baseEffect.getColorType());
		effect.setPalette(baseEffect.getPalette());
		if (json.has("animData")) {
			effect.setAnimationData(json.getString("animData"));
		}
		return effect;
	}
	
	/**
	 * The custom effect builder makes it easy to programmatically design static effects.
	 * Only one frame can be added per panel. Panels that are not set in the effect will
	 * "freeze" on the color they were before the effect is displayed.
	 */
	public static class Builder {
		
		private Panel[] panels;
		private Map<Integer, Frame> frames;
		
		/**
		 * Creates a new static effect builder.
		 * 
		 * @param device               the target device that the effect will be displayed on
		 * @throws NanoleafException   If the device access token is invalid
		 */
		public Builder(NanoleafDevice device)
				throws NanoleafException, IOException {
			panels = device.getPanels();
			frames = new HashMap<Integer, Frame>();
		}
		
		/**
		 * Creates a new static effect builder without requesting panel data
		 * from the device.
		 * 
		 * @param panels  the panel data
		 */
		public Builder(Panel[] panels) {
			this.panels = panels;
			frames = new HashMap<Integer, Frame>();
		}
		
		/**
		 * <p>Asynchronously creates a new static effect builder.</p>
		 * 
		 * <p>The callback status will return {@link NanoleafCallback.SUCCESS} on success,
		 * or {@link NanoleafCallback.UNAUTHORIZED} if the access token is invalid. If an
		 * internal API error occurs, it will instead return
		 * {@link NanoleafCallback.FAILURE}.</p>
		 * 
		 * @param device     the target device that the effect will be displayed on
		 * @param callback   the callback to receive the builder
		 */
		public void createBuilderAsync(NanoleafDevice device, NanoleafCallback<Builder> callback) {
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
		 * Gets a map of the frames for this effect. The key represents the
		 * panel and the value represents the frame for the corresponding panel.
		 * 
		 * @return   a map of the frames for this effect
		 */
		public Map<Integer, Frame> getFrames() {
			return frames;
		}
		
		/**
		 * Creates a new static effect using the animation data from the builder.
		 * 
		 * @param effectName           the desired effect name
		 * @return                     a new static effect
		 * @throws NanoleafException   If the access token is invalid
		 */
		public StaticEffect build(String effectName)
				throws NanoleafException, IOException {
			StringBuilder data = new StringBuilder();
			data.append(frames.size());
			List<Integer> ids = new ArrayList<Integer>(frames.keySet());
			for (int i = 0; i < panels.length; i++) {
				if (ids.contains(panels[i].getId())) {
					Panel panel = panels[i];
					Frame frame = frames.get(panel.getId());
					data.append(" " + panel.getId() + " 1");
					data.append(" " +
								frame.getRed() + " " +
								frame.getGreen() + " " +
								frame.getBlue() + " " +
								0 + " " +
								frame.getTransitionTime());
				}
			}
			return createStaticEffect(effectName, data.toString());
		}
		
		/**
		 * Adds a new frame (RGB color and transition time) to the effect.
		 * 
		 * @param panel   the panel to add the frame to
		 * @param frame   the RGB color and transition time
		 * @return        the current builder
		 */
		public Builder setPanel(Panel panel, Frame frame) {
			return setPanel(panel.getId(), frame);
		}
		
		/**
		 * Adds a new frame (RGB color and transition time) to the effect.
		 * 
		 * @param panelId   the panelId of the panel to add the frame to
		 * @param frame     the RGB color and transition time
		 * @return          the current builder
		 */
		public Builder setPanel(int panelId, Frame frame) {
			if (panelIdIsValid(panelId)) {
				this.frames.put(panelId, frame);
			}
			else {
				throw new IllegalArgumentException("Panel with id " +
						panelId + " does not exist.");
			}
			return this;
		}
		
		/**
		 * Adds a frame to all panels in the effect.
		 * 
		 * @param frame   the RGB color and transition time
		 * @return        the current builder
		 */
		public Builder setAllPanels(Frame frame) {
			for (Panel panel : this.panels) {
				this.frames.put(panel.getId(), frame);
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
