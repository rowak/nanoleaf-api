package io.github.rowak.nanoleafapi.event;

/**
 * The general classes of events that separate the different functions of
 * Nanoleaf devices.
 */
public enum EventType {
	
	/** Listens for changes to the state of the Nanoleaf device, such as
	 *  changes in brightness. */
	STATE,
	
	/** Listens for changes to the physical layout of the Nanoleaf panels. */
	LAYOUT,
	
	/** Listens for changes to either the effect currently being displayed,
	 *  or changes to the effects installed on the Nanoleaf device. */
	EFFECTS,
	
	/** Listens for physical interactions with the Nanoleaf panels, such as
	 *  single/double taps and swipes. */
	TOUCH
}
