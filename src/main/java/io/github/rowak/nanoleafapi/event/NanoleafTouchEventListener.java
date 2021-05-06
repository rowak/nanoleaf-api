package io.github.rowak.nanoleafapi.event;

import io.github.rowak.nanoleafapi.NanoleafDevice;

/**
 * A listener for listening to low-latency touch events from a Nanoleaf device.
 * A listener can be registered on a Nanoleaf device using the
 * {@link NanoleafDevice#registerTouchEventStreamingListener(NanoleafTouchEventListener)}
 * method.
 */
public interface NanoleafTouchEventListener {
	
	/**
	 * Called when the connection has been successfully opened.
	 */
	public void onOpen();
	
	/**
	 * Called when the connection has been successfully closed.
	 */
	public void onClosed();
	
	/**
	 * Called when a touch event has been received.
	 * 
	 * @param event   the received touch event
	 */
	public void onEvent(DetailedTouchEvent[] event);
}
