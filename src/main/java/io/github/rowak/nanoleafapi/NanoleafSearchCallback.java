package io.github.rowak.nanoleafapi;

import io.github.rowak.nanoleafapi.util.NanoleafDeviceMeta;
import io.github.rowak.nanoleafapi.util.NanoleafSetup;

/**
 * A callback interface for the asynchronous Nanoleaf device search
 * method {@link NanoleafSetup#findNanoleafDevicesAsync(NanoleafSearchCallback, int)}.
 */
public interface NanoleafSearchCallback {
	
	/**
	 * Signals that an asynchronous request has completed.
	 * 
	 * @param meta   the metadata for the discovered device
	 */
	public void onDeviceFound(NanoleafDeviceMeta meta);
	
	/**
	 * Signals that the search has timed out.
	 */
	public void onTimeout();
}
