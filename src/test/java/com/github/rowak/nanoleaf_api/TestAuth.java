package com.github.rowak.nanoleaf_api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.junit.Test;

import com.github.rowak.nanoleafapi.util.HttpUtil;

import okhttp3.OkHttpClient;
import okhttp3.Response;

/**
 * This class stores the temporary authentication required for testing.
 * The authentication file is ".testauth" and it must be present with
 * the following contents format:
 *    IP:PORT
 *    ACCESS_TOKEN
 */
public class TestAuth {
//	public static final String ADDRESS = readAddress();
//	public static final String TOKEN = readToken();
//	
//	private static final String CONFIG = ".testauth";
//	
//	private static String readAddress() {
//		String addr = null;
//		BufferedReader reader = null;
//		try {
//			reader = new BufferedReader(new FileReader(CONFIG));
//			addr = reader.readLine();
//		}
//		catch (IOException e) {
//			e.printStackTrace();
//		}
//		finally {
//			try {
//				reader.close();
//			}
//			catch (IOException e) {
//				e.printStackTrace();
//			}
//		}
//		return addr;
//	}
//	
//	private static String readToken() {
//		String token = null;
//		BufferedReader reader = null;
//		try {
//			reader = new BufferedReader(new FileReader(CONFIG));
//			reader.readLine();
//			token = reader.readLine();
//		}
//		catch (IOException e) {
//			e.printStackTrace();
//		}
//		finally {
//			try {
//				reader.close();
//			}
//			catch (IOException e) {
//				e.printStackTrace();
//			}
//		}
//		return token;
//	}
//	
//	@Test
//	public void authShouldBeValid() throws IOException {
//		assertNotNull(ADDRESS);
//		assertNotNull(TOKEN);
//		OkHttpClient client = new OkHttpClient();
//		String url = String.format("http://%s/api/v1/%s", ADDRESS, TOKEN);
//		Response r = HttpUtil.getHttpSync(client, url);
//		assertEquals(HttpUtil.OK, r.code());
//	}
}
