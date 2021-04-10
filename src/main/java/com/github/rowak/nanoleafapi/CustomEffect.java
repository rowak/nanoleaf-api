package com.github.rowak.nanoleafapi;

import org.json.JSONObject;

public class CustomEffect extends Effect {
	protected static final String CUSTOM_ANIM_TYPE = "custom";
	
	private String animData;
	private boolean loop;
	
	public CustomEffect() {
		setEffectType(CUSTOM_ANIM_TYPE);
	}
	
	public String getAnimationData() {
		return animData;
	}
	
	public void setAnimationData(String animData) {
		this.animData = animData;
	}
	
	public boolean doLoop() {
		return loop;
	}
	
	public void setLoop(boolean loop) {
		this.loop = loop;
	}
	
	@Override
	public JSONObject toJSON() {
		JSONObject json = super.toJSON();
		json.put("animType", getEffectType());
		json.put("animData", animData);
		json.put("loop", loop);
		return json;
	}
	
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
			effect.setLoop(json.getBoolean("loop"));
		}
		return effect;
	}
}
