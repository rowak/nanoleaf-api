package com.github.rowak.nanoleafapi.util;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.json.JSONObject;

import com.github.rowak.nanoleafapi.NanoleafDevice;
import com.github.rowak.nanoleafapi.NanoleafDeviceMeta;
import com.github.rowak.nanoleafapi.NanoleafException;

import net.straylightlabs.hola.dns.Domain;
import net.straylightlabs.hola.sd.Instance;
import net.straylightlabs.hola.sd.Query;
import net.straylightlabs.hola.sd.Service;
import okhttp3.OkHttpClient;
import okhttp3.Response;

/**
 * A utility class with useful methods for managing Nanoleaf devices.
 */
public class Setup {
	
	public static int DEFAULT_PORT = 16021;
	public static String NANOLEAF_MDNS_SERVICE = "_nanoleafapi._tcp";
	
	/**
	 * Searches for Nanoleaf devices on the local network using mDNS.<br>
	 * <i>Note: This method has the potential to fail (and return an empty array).
	 * You may want to call the method more than once or handle this in some other way.</i>
	 * @return  a collection of type <code>NanoleafDeviceMeta</code>,
	 * 			with each element containing the metadata of a device
	 * @throws IOException  unknown IO exception
	 * @throws UnknownHostException  if the host's local address cannot be found
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
	 * Creates a unique authentication token that exists until it is destroyed
	 * using the <code>destroyAccessToken()</code> method.
	 * @param host  the hostname of the controller
	 * @param port  the port of the controller (default=16021)
	 * @return a unique authentication token
	 * @throws NanoleafException  if the response status code not 2xx
	 * @throws IOException
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
	 * Permanently destroys an authentication token.
	 * @param host  the hostname of the device
	 * @param port  the port of the device (default=16021)
	 * @param accessToken  a unique authentication token
	 * @throws NanoleafException  if the access token is invalid
	 * @throws IOException
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
}
