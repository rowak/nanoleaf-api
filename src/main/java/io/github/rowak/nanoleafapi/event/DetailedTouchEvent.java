package io.github.rowak.nanoleafapi.event;

/**
 * Detailed touch events are received from low-latency touch event streaming.
 * Detailed touch events NOT related to the {@link Event} class.
 */
public class DetailedTouchEvent {
	
	/** Event attribute that is unrecognizable by this library. You should never
	 *  receive this attribute. Please report on GitHub. */
	public static final int UNKNOWN_EVENT = -1;
	
	/** Triggered while a hand is held just above a panel. */
	public static final int HOVER_EVENT = 0;
	
	/** Triggered when a hand is pressed on a panel. */
	public static final int DOWN_EVENT = 1;
	
	/** Triggered while a hand is held down on a panel. */
	public static final int HOLD_EVENT = 2;
	
	/** Triggered when a hand is lifted off of a panel. */
	public static final int UP_EVENT = 3;
	
	/** Triggered when a hand is swiped from one panel to another.
	 *  The panel ID will be set to the panel that the hand has swiped to,
	 *  and the initial panel ID will be set to the panel that the hand
	 *  swiped from. This can be used to determine the direction of the swipe. */
	public static final int SWIPE_EVENT = 4;
	
	/** Indicates that no panel is associated with the gesture. */
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
	
	/**
	 * Gets the ID of the panel associated with the event. This
	 * will always return a valid panel ID.
	 * 
	 * @return   the panel ID
	 */
	public int getPanelId() {
		return panelId;
	}
	
	/**
	 * <p>Gets the type of panel interaction for the event.</p>
	 * 
	 * <p>This will always return one of the following:
	 * <ul>
	 * <li>{@link DetailedTouchEvent#HOVER_EVENT}</li>
	 * <li>{@link DetailedTouchEvent#DOWN_EVENT}</li>
	 * <li>{@link DetailedTouchEvent#HOLD_EVENT}</li>
	 * <li>{@link DetailedTouchEvent#UP_EVENT}</li>
	 * <li>{@link DetailedTouchEvent#SWIPE_EVENT}</li>
	 * <ul></p>
	 * 
	 * @return   the touch type
	 */
	public int getTouchType() {
		return touchType;
	}
	
	/**
	 * Gets the strength of the hand being pressed down.
	 * 
	 * @return   the touch strength
	 */
	public int getStrength() {
		return touchStrength;
	}
	
	/**
	 * Gets the ID of the panel from which a swipe originated from. The
	 * initial panel will only be set for events with a touch type of
	 * {@link DetailedTouchEvent#SWIPE_EVENT}. This can be used to identify
	 * the direction of a swipe.
	 * 
	 * @return   the initial panel ID
	 */
	public int getInitialPanelId() {
		return initialPanelId;
	}
}
