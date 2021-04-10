package com.github.rowak.nanoleafapi;

import org.json.JSONObject;

public class StaticEffect extends CustomEffect {
	private static final String STATIC_ANIM_TYPE = "static";
	
	public StaticEffect() {
		setEffectType(STATIC_ANIM_TYPE);
	}
	
	public static CustomEffect fromJSON(JSONObject json) {
		Effect baseEffect = Effect.fromJSON(json);
		CustomEffect effect = new CustomEffect();
		effect.setName(baseEffect.getName());
		effect.setEffectType(STATIC_ANIM_TYPE);
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
