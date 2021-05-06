package io.github.rowak.nanoleafapi;

/**
 * An interface for creating a shared function between all devices in
 * a Nanoleaf group. Used for the {@link NanoleafGroup#forEach(GroupAction)}
 * method.
 */
public interface GroupAction {
	
	/**
	 * Called when the shared action is run for a device in the group. This
	 * method will be called for each device in the group.
	 * 
	 * @param device   the device executing the action
	 */
	public void run(NanoleafDevice device);
}
