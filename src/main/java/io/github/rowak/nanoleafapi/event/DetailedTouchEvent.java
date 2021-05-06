package io.github.rowak.nanoleafapi.event;

public class DetailedTouchEvent {
	
	public static final int UNKNOWN_EVENT = -1;
	public static final int HOVER_EVENT = 0;
	public static final int DOWN_EVENT = 1;
	public static final int HOLD_EVENT = 2;
	public static final int UP_EVENT = 3;
	public static final int SWIPE_EVENT = 4;
	
	public static final int NO_PANEL = -1;
	
	private int panelId;
	private int touchType;
	private int touchStrength;
	private int initialPanelId;
	
	public DetailedTouchEvent(int panelId, int touchType, int touchStrength, int initialPanelId) {
		this.panelId = panelId;
		this.touchType = touchType;
		this.touchStrength = touchStrength;
		this.initialPanelId = initialPanelId;
	}
	
	public int getPanelId() {
		return panelId;
	}
	
	public int getTouchType() {
		return touchType;
	}
	
	public int getStrength() {
		return touchStrength;
	}
	
	public int getInitialPanelId() {
		return initialPanelId;
	}
}
