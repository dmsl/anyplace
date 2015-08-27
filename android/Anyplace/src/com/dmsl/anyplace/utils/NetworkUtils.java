/*
* AnyPlace: A free and open Indoor Navigation Service with superb accuracy!
*
* Anyplace is a first-of-a-kind indoor information service offering GPS-less
* localization, navigation and search inside buildings using ordinary smartphones.
*
* Author(s): Lambros Petrou, Timotheos Constambeys
* 
* Supervisor: Demetrios Zeinalipour-Yazti
*
* URL: http://anyplace.cs.ucy.ac.cy
* Contact: anyplace@cs.ucy.ac.cy
*
* Copyright (c) 2015, Data Management Systems Lab (DMSL), University of Cyprus.
* All rights reserved.
*
* Permission is hereby granted, free of charge, to any person obtaining a copy of
* this software and associated documentation files (the “Software”), to deal in the
* Software without restriction, including without limitation the rights to use, copy,
* modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
* and to permit persons to whom the Software is furnished to do so, subject to the
* following conditions:
*
* The above copyright notice and this permission notice shall be included in all
* copies or substantial portions of the Software.
*
* THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS
* OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
* FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
* DEALINGS IN THE SOFTWARE.
*
*/

package com.dmsl.anyplace.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import javax.net.ssl.HttpsURLConnection;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.util.InetAddressUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.http.AndroidHttpClient;

/**
 * Network helpers for usual actions.
 * 
 * @author Lambros Petrou
 *
 */
public class NetworkUtils {

	public static String encodeURL(String urlStr) throws URISyntaxException, MalformedURLException {
		URL url = new URL(urlStr);
		URI uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(), url.getRef());
		url = uri.toURL();
		return url.toString();
	}

	private static String readInputStream(InputStream stream) throws IOException {
		int n = 0;
		char[] buffer = new char[1024 * 4];
		InputStreamReader reader = new InputStreamReader(stream, "UTF8");
		StringWriter writer = new StringWriter();
		while (-1 != (n = reader.read(buffer)))
			writer.write(buffer, 0, n);
		return writer.toString();
	}

	public static InputStream downloadHttps(String urlS) {
		InputStream is = null;

		try {
			URL url = new URL(encodeURL(urlS));

			HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
			conn.setReadTimeout(10000);
			conn.setConnectTimeout(15000);
			conn.setRequestMethod("GET");
			conn.setDoInput(true);

			conn.connect();

			int response = conn.getResponseCode();
			if (response == 200) {
				is = conn.getInputStream();
			}
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return is;
	}

	public static InputStream downloadHttp(String urlS) throws URISyntaxException, IOException {
		InputStream is = null;

		URL url = new URL(encodeURL(urlS));

		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setReadTimeout(10000);
		conn.setConnectTimeout(15000);
		conn.setRequestMethod("GET");
		conn.setDoInput(true);

		conn.connect();

		int response = conn.getResponseCode();
		if (response == 200) {
			is = conn.getInputStream();
		}

		return is;
	}

	public static InputStream downloadHttpClient(String url) {
		InputStream is = null;
		try {
			HttpClient client = new DefaultHttpClient();
			HttpGet request = new HttpGet(encodeURL(url));
			HttpResponse response = client.execute(request);
			HttpEntity entity = response.getEntity();
			is = entity.getContent();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return is;
	}

	public static String downloadUrlAsStringHttp(String urlS) throws URISyntaxException, IOException {
		InputStream is = null;
		try {
			is = downloadHttp(urlS);
			return readInputStream(is);
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
				}
			}
		}
	}

	public static String downloadUrlAsStringHttps(String urlS) {
		InputStream is = null;
		try {
			is = downloadHttps(urlS);
			return readInputStream(is);
		} catch (Exception e) {

		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					// TODO - error closing inputstream
				}
			}
		}
		return null;
	}

	private static HttpEntity downloadHttpClientJsonPostHelp(String url, String json, int timeout) throws URISyntaxException, ClientProtocolException, IOException {

		HttpParams httpParameters = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(httpParameters, timeout);
		HttpConnectionParams.setSoTimeout(httpParameters, timeout);

		HttpClient client = new DefaultHttpClient(httpParameters);
		HttpPost request = new HttpPost(encodeURL(url));
		StringEntity se = new StringEntity(json);
		request.setEntity(se);
		request.setHeader("Accept", "application/json");
		request.setHeader("Content-type", "application/json");

		HttpResponse response = client.execute(request);

		int status = response.getStatusLine().getStatusCode();
		if (status != 200 && status != 400) {
			throw new RuntimeException("Server Error: " + response.getStatusLine().getReasonPhrase());
		}

		HttpEntity entity = response.getEntity();
		if (entity == null)
			throw new RuntimeException("Server Error: " + response.getStatusLine().getReasonPhrase());

		return entity;
	}

	public static String downloadHttpClientJsonPost(String url, String json, int timeout) throws URISyntaxException, ClientProtocolException, IOException {

		String content = EntityUtils.toString(downloadHttpClientJsonPostHelp(url, json, timeout));
		return content;

		/*
		 * is = entity.getContent(); return readInputStream(is); Old Way Hangs
		 * Sometimes <0.8
		 */
	}

	public static String downloadHttpClientJsonPost(String url, String json) throws URISyntaxException, IOException {
		return downloadHttpClientJsonPost(url, json, 20000);
	}

	public static InputStream downloadHttpClientJsonPostStream(String url, String json) throws IllegalStateException, ClientProtocolException, IOException, URISyntaxException {
		return downloadHttpClientJsonPostHelp(url, json, 20000).getContent();
	}

	public static String downloadHttpClientJsonPostGzip(String url, String json) throws IllegalStateException, ClientProtocolException, IOException, URISyntaxException {
		InputStream stream = AndroidHttpClient.getUngzippedContent(downloadHttpClientJsonPostHelp(url, json, 20000));
		return readInputStream(stream);
	}

	public static String getLocalIP(boolean useIPv4) {
		String ip = "No Local IP assigned";
		try {

			List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
			for (NetworkInterface ni : interfaces) {
				List<InetAddress> addresses = Collections.list(ni.getInetAddresses());
				for (InetAddress ia : addresses) {
					if (ia != null && !ia.isLoopbackAddress()) {
						String sAddr = ia.getHostAddress().toUpperCase(Locale.ENGLISH);
						boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
						if (useIPv4) {
							if (isIPv4) {
								ip = sAddr;
							}
						} else {
							if (!isIPv4) {
								int delim = sAddr.indexOf('%');
								ip = delim < 0 ? sAddr : sAddr.substring(0, delim);
							}
						}
					}
				}
			}
		} catch (Exception ex) {
			ip = "Unknown Error, Report it!";
		}
		return ip;
	}

	public static String getExternalIP(Activity activity) {
		boolean hadError = false;
		String ip = null;
		try {

			if (!NetworkUtils.haveNetworkConnection(activity)) {
				return "No Internet Connection";
			}

			HttpClient client = new DefaultHttpClient();
			HttpGet request = new HttpGet("http://www.lpcode.net/api/externalip/");

			HttpResponse response = client.execute(request);
			HttpEntity entity = response.getEntity();
			InputStream content = entity.getContent();

			// Log.d("update ip", "ip: " + content);

			if (content == null)
				return "Connection Error";

			BufferedReader reader = new BufferedReader(new InputStreamReader(content, "utf-8"));
			StringBuilder sb = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null) {
				sb.append(line);
			}
			content.close();
			ip = sb.toString().replaceAll("\"", "");

		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
			// Toast.makeText(getBaseContext(), e.getMessage(),
			// Toast.LENGTH_SHORT).show();
			hadError = true;
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
			// Toast.makeText(getBaseContext(), e.getMessage(),
			// Toast.LENGTH_SHORT).show();
			hadError = true;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
			// Toast.makeText(getBaseContext(), e.getMessage(),
			// Toast.LENGTH_SHORT).show();
			hadError = true;
		}
		if (hadError) {
			return "Unknown Error, Report it!";
		}
		return ip;
	}

	public static boolean isOnline(Context context) {
		ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
		return (networkInfo != null && networkInfo.isConnected());
	}

	public static boolean isOnlineWiFiOrMobile(Context context) {
		ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		boolean isWifiConn = networkInfo.isConnected();
		networkInfo = connMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
		boolean isMobileConn = networkInfo.isConnected();
		return isMobileConn || isWifiConn;
	}

	public static boolean haveNetworkConnection(Activity activity) {

		boolean haveWifi = false;
		boolean haveMobile = false;

		ConnectivityManager cm = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo[] netInfo = cm.getAllNetworkInfo();
		for (NetworkInfo ni : netInfo) {
			if (ni != null && ni.getTypeName().equalsIgnoreCase("WIFI"))
				if (ni.isConnectedOrConnecting())
					haveWifi = true;
			if (ni != null && ni.getTypeName().equalsIgnoreCase("MOBILE"))
				if (ni.isConnectedOrConnecting())
					haveMobile = true;
		}
		return haveMobile || haveWifi;
	}

}
