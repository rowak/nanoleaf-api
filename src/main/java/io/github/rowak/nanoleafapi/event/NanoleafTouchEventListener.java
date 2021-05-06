package io.github.rowak.nanoleafapi.event;

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
	 * @param event   the received touch event
	 */
	public void onEvent(DetailedTouchEvent[] event);
}
