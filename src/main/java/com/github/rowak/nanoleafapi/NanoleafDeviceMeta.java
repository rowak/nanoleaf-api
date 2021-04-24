package com.github.rowak.nanoleafapi;

import java.net.InetAddress;
import java.util.Iterator;

import com.github.rowak.nanoleafapi.util.Instance;

/**
 * An object for storing information related to an
 * Aurora device. This informtion is typically received
 * from mDNS/SSDP broadcast packets.
 */
public class NanoleafDeviceMeta {
	
	private String hostName;
	private int port;
	private String deviceId;
	private String deviceName;
	
	/**
	 * Creates a new <code>AuroraMetadata</code> object
	 * using existing Aurora device information.
	 * @param hostName  the hostname/ IP address of the Aurora
	 * @param port  the port that the Aurora is using
	 * @param deviceId  the unique device id for the Aurora
	 * @param deviceName  the unique name for the Aurora
	 * 					  (a combination of the device type
	 * 					  and the MAC address)
	 */
	public NanoleafDeviceMeta(String hostName, int port,
			String deviceId, String deviceName) {
		this.hostName = hostName;
		this.port = port;
		this.deviceId = deviceId;
		this.deviceName = deviceName;
	}
	
	/**
	 * Creates a new <code>AuroraMetadata</code> object using
	 * packet data from an mDNS broadcast.<br>
	 * <b>Note: This is used internally by the api.</b>
	 * @param instance  the packet data containing the
	 * 			        Aurora device information
	 * @return  a new <code>AuroraMetadata</code> object
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
	 * Gets the name of the associated Aurora device.
	 * @return  the name of the Aurora device
	 */
	public String getHostName() {
		return hostName;
	}
	
	/**
	 * Sets the metadata host name of the associated Aurora device.<br>
	 * <i>This does not affect the host name of the actual Aurora device.</i>
	 * @param hostName  the host name of the Aurora
	 */
	public void setHostName(String hostName) {
		this.hostName = hostName;
	}
	
	/**
	 * Gets the port of the associated Aurora device.
	 * @return  the port of the Aurora device
	 */
	public int getPort() {
		return port;
	}
	
	/**
	 * Sets the port of the associated Aurora device.<br>
	 * <i>This does not affect the port of the actual Aurora device.</i>
	 * @param port  the port of the Aurora
	 */
	public void setPort(int port) {
		this.port = port;
	}
	
	/**
	 * Gets the device id of the associated Aurora device.
	 * @return  the device id of the Aurora device
	 */
	public String getDeviceId() {
		return deviceId;
	}
	
	/**
	 * Sets the device id of the associated Aurora device.<br>
	 * <i>This does not affect the device id of the actual Aurora device.</i>
	 * @param deviceId  the device id of the Aurora
	 */
	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}
	
	/**
	 * Gets the device name of the associated Aurora device.
	 * The device name is a combination of the device type
	 * and the MAC address. For example, "Nanoleaf Light Panels 11:ab:2c".
	 * @return  the device name of the Aurora device
	 */
	public String getDeviceName() {
		return deviceName;
	}
	
	/**
	 * Sets the device name of the associated Aurora device.
	 * The device name is a combination of the device type
	 * and the MAC address. For example, "Nanoleaf Light Panels 11:ab:2c".<br>
	 * <i>This does not affect the device name of the actual Aurora device.</i>
	 * @param deviceName  the device name of the Aurora
	 */
	public void setDeviceName(String deviceName) {
		this.deviceName = deviceName;
	}
	
	@Override
	public String toString() {
		return String.format("%s:%d (%s)", hostName, port, deviceName);
	}
}
