package io.github.rowak.nanoleafapi;

import java.net.InetAddress;
import java.util.Iterator;

import io.github.rowak.nanoleafapi.util.Instance;

/**
 * An object for storing information related to a Nanoleaf device. This information
 * is typically received from mDNS broadcast packets, however, this class can also
 * be used separately as a simple storage for Nanoleaf devices.
 */
public class NanoleafDeviceMeta {
	
	private String hostName;
	private int port;
	private String deviceId;
	private String deviceName;
	
	/**
	 * Creates a new metadata object using existing Nanoleaf device information.
	 * 
	 * @param hostName     the hostname/ IP address of the device
	 * @param port         the port that the device is using
	 * @param deviceId     the unique device id for the device
	 * @param deviceName   the unique name for the device (a combination of the
	 *                     device type and the MAC address)
	 */
	public NanoleafDeviceMeta(String hostName, int port,
			String deviceId, String deviceName) {
		this.hostName = hostName;
		this.port = port;
		this.deviceId = deviceId;
		this.deviceName = deviceName;
	}
	
	/**
	 * <p>Creates a new metadata object using packet data from an mDNS broadcast.</p>
	 * 
	 * <p><b>Note:</b> This is used internally by the API.</p>
	 * 
	 * @param instance   the packet data containing the Nanoleaf device information
	 * @return           a new metadata object
	 */
	public static NanoleafDeviceMeta fromMDNSInstance(Instance instance) {
		NanoleafDeviceMeta metadata = new NanoleafDeviceMeta(null, 0, null, null);
		Iterator<InetAddress> addresses = instance.getAddresses().iterator();
		InetAddress address = addresses.next();
		metadata.setHostName(address.getHostName());
		metadata.setPort(instance.getPort());
		metadata.setDeviceId(address.getHostAddress());
		metadata.setDeviceName(instance.getName());
		return metadata;
	}
	
	/**
	 * Gets the name of the associated Nanoleaf device.
	 * 
	 * @return  the name of the device
	 */
	public String getHostName() {
		return hostName;
	}
	
	/**
	 * Sets the metadata host name of the associated Nanoleaf device.
	 * 
	 * @param hostName  the host name of the device
	 */
	public void setHostName(String hostName) {
		this.hostName = hostName;
	}
	
	/**
	 * Gets the port of the associated Nanoleaf device.
	 * 
	 * @return  the port of the Nanoleaf device
	 */
	public int getPort() {
		return port;
	}
	
	/**
	 * Sets the port of the associated Nanoleaf device.
	 * 
	 * @param port  the port of the Aurora
	 */
	public void setPort(int port) {
		this.port = port;
	}
	
	/**
	 * Gets the device id of the associated Nanoleaf device.
	 * 
	 * @return  the device id of the device
	 */
	public String getDeviceId() {
		return deviceId;
	}
	
	/**
	 * Sets the device id of the associated Nanoleaf device.
	 * 
	 * @param deviceId  the device id of the device
	 */
	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}
	
	/**
	 * Gets the device name of the associated Nanoleaf device. The device name is
	 * either a combination of the device type and the MAC address, or a combination
	 * of the device type and the model number. For example, "Nanoleaf Light Panels 11:ab:2c".
	 * 
	 * @return   the device name of the Nanoleaf device
	 */
	public String getDeviceName() {
		return deviceName;
	}
	
	/**
	 * <p>Sets the device name of the associated Nanoleaf device. The device name is either
	 * a combination of the device type and the MAC address, or a combination of the
	 * device type and model number. For example, "Nanoleaf Light Panels 11:ab:2c".</p>
	 * 
	 * <p><b>Note:</b> This does not affect the device name of the actual Nanoleaf device.</p>
	 * 
	 * @param deviceName   the device name of the device
	 */
	public void setDeviceName(String deviceName) {
		this.deviceName = deviceName;
	}
	
	@Override
	public String toString() {
		return String.format("%s:%d (%s)", hostName, port, deviceName);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null || obj.getClass() != this.getClass()) {
			return false;
		}
		NanoleafDeviceMeta other = (NanoleafDeviceMeta)obj;
		return this.hostName.equals(other.hostName) && this.port == other.port &&
				this.deviceName.equals(other.deviceName);
	}
}
