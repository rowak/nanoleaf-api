package io.github.rowak.nanoleafapi.util;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.json.JSONObject;

import io.github.rowak.nanoleafapi.NanoleafCallback;
import io.github.rowak.nanoleafapi.NanoleafDevice;
import io.github.rowak.nanoleafapi.NanoleafDeviceMeta;
import io.github.rowak.nanoleafapi.NanoleafException;
import net.straylightlabs.hola.dns.Domain;
import net.straylightlabs.hola.sd.Service;
import okhttp3.OkHttpClient;
import okhttp3.Response;

/**
 * A utility class with useful methods for managing Nanoleaf devices.
 */
public class NanoleafSetup {
	
	public static int DEFAULT_PORT = 16021;
	public static String NANOLEAF_MDNS_SERVICE = "_nanoleafapi._tcp";
	
	/**
	 * Searches for Nanoleaf devices on the local network.<br>
	 * 
	 * <p><b>Note:</b> If the timeout is set too low, not all of the devices may be found
	 * in time. A few seconds should usually be enough time. You may also want to consider
	 * using the asynchronous version of this method.</p>
	 * 
	 * @return                        a list of device metadata objects
	 * 			                      with each element containing the metadata of a device
	 * @throws UnknownHostException   If the host's local address cannot be found
	 * @throws IOException            Unknown IO exception
	 */
	public static List<NanoleafDeviceMeta> findNanoleafDevices(int timeout)
			throws UnknownHostException, IOException {
		List<NanoleafDeviceMeta> devices = new ArrayList<NanoleafDeviceMeta>();
		Service service = Service.fromName(NANOLEAF_MDNS_SERVICE);
        Query query = Query.createWithTimeout(service, Domain.LOCAL, timeout);
        Set<Instance> instances = query.runOnce();
        for (Iterator<Instance> it = instances.iterator(); it.hasNext();) {
        	devices.add(NanoleafDeviceMeta.fromMDNSInstance(it.next()));
        }
        return devices;
	}
	
	/**
	 * Searches for Nanoleaf devices on the local network.<br>
	 * 
	 * <p><b>Note:</b> If the timeout is set too low, not all of the devices may be found
	 * in time. A few seconds should usually be enough time.</p>
	 * 
	 * <p>The callback status will return {@link NanoleafCallback.SUCCESS} on success,
	 * or {@link NanoleafCallback.FAILURE} if an error occurs.</p>
	 * 
	 * @throws UnknownHostException  if the host's local address cannot be found
	 * @throws IOException           unknown IO exception
	 */
	public static void findNanoleafDevicesAsync(NanoleafCallback<NanoleafDeviceMeta> callback, int timeout)
			throws UnknownHostException, IOException {
		Service service = Service.fromName(NANOLEAF_MDNS_SERVICE);
        Query query = Query.createWithTimeout(service, Domain.LOCAL, timeout);
        query.runAsync((instance) -> {
        	callback.onCompleted(NanoleafCallback.SUCCESS, NanoleafDeviceMeta.fromMDNSInstance(instance), null);
        });
	}
	
	/**
	 * Creates a unique authentication token that exists until it is destroyed
	 * using the {@link NanoleafSetup#destroyAccessToken} method.
	 * 
	 * @param host                 the hostname of the controller
	 * @param port                 the port of the controller (default=16021)
	 * @return                     a unique authentication token
	 * @throws NanoleafException   If the response status code not 2xx
	 * @throws IOException         If an HTTP exception occurs
	 */
	public static String createAccessToken(String host, int port)
			throws NanoleafException, IOException {
		OkHttpClient client = new OkHttpClient();
		String url = String.format("http://%s:%d/api/%s/new",
				host, port, NanoleafDevice.API_LEVEL);
		Response resp = HttpUtil.postHttpSync(client, url, null);
		int status = resp.code();
		String body = resp.body().string();
		if (status == HttpUtil.OK) {
			JSONObject json = new JSONObject(body);
			return json.getString("auth_token");
		}
		else {
			NanoleafException.checkStatusCode(status);
			return null;
		}
	}
	
	/**
	 * Permanently destroys an access token.
	 * 
	 * @param host                 the hostname of the device
	 * @param port                 the port of the device (default=16021)
	 * @param accessToken          a unique authentication token
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public static void destroyAccessToken(String host, int port, String accessToken)
			throws NanoleafException, IOException {
		OkHttpClient client = new OkHttpClient();
		String url = String.format("http://%s:%d/api/%s/%s",
				host, port, NanoleafDevice.API_LEVEL, accessToken);
		Response resp = HttpUtil.deleteHttpSync(client, url);
		int status = resp.code();
		NanoleafException.checkStatusCode(status);
	}
	
	/**
	 * Permanently destroys the access token associated with a Nanoleaf device.
	 * This will also render the NanoleafDevice object unusable.
	 * 
	 * @param device               the device whose access token to destroy
	 * @throws NanoleafException   If the access token is invalid
	 * @throws IOException         If an HTTP exception occurs
	 */
	public static void destroyAccessToken(NanoleafDevice device)
			throws NanoleafException, IOException {
		destroyAccessToken(device.getHostname(), device.getPort(), device.getAccessToken());
	}
}
