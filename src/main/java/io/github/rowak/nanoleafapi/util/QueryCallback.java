package io.github.rowak.nanoleafapi.util;

/**
 * This is a simple callback interface for the Hola Query wrapper.
 */
public interface QueryCallback {
	
	/**
	 * Signals that an instance has been received. May be called more than once.
	 * @param instance  the instance that was received
	 */
	public void onInstance(Instance instance);
	
	/**
	 * Signals that the search has timed out.
	 */
	public void onTimeout();
}
