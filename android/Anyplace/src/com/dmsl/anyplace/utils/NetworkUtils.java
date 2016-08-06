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
* this software and associated documentation files (the "Software"), to deal in the
* Software without restriction, including without limitation the rights to use, copy,
* modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
* and to permit persons to whom the Software is furnished to do so, subject to the
* following conditions:
*
* The above copyright notice and this permission notice shall be included in all
* copies or substantial portions of the Software.
*
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
* OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
* FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
* DEALINGS IN THE SOFTWARE.
*
*/

package com.dmsl.anyplace.utils;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HttpsURLConnection;

/**
 * Network helpers for usual actions.
 *
 * @author Lambros Petrou
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

    // <HTTP Get>
    public static InputStream downloadHttps(String urlS) throws IOException, URISyntaxException {
        InputStream is = null;

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
        } else {
            throw new RuntimeException("Server Error Code: " + conn.getResponseCode());
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
        } else {
            throw new RuntimeException("Server Error Code: " + conn.getResponseCode());
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

    public static String downloadUrlAsStringHttps(String urlS) throws IOException, URISyntaxException {
        InputStream is = null;
        try {
            is = downloadHttps(urlS);
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
    // </HTTP Get>

    /* HTTP Post Json (InputStream) */
    private static InputStream ISdownloadHttpClientJsonPostHelp(String url, String json, int timeout) throws URISyntaxException, IOException {

        InputStream is;

        URL obj = new URL(url);
        HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();
        con.setConnectTimeout(timeout);
        con.setRequestMethod("POST");
        con.setRequestProperty("Accept", "application/json");
        con.setRequestProperty("Accept-Encoding", "gzip");
        con.setRequestProperty("Content-type", "application/json");
        con.setDoOutput(true);
        con.setDoInput(true);
        con.connect();

        OutputStream os = con.getOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os));
        writer.write(json);
        writer.flush();
        writer.close();
        os.close();

        String encoding = con.getContentEncoding();

        int response = con.getResponseCode();
        if (response == HttpsURLConnection.HTTP_OK) {
            if (encoding != null && encoding.equals("gzip")) {
                is = new GZIPInputStream(con.getInputStream());
            } else {
                is = con.getInputStream();
            }
        } else {
            throw new RuntimeException("Service Error: " + con.getResponseMessage());
        }

        return is;
    }

    public static String downloadHttpClientJsonPost(String url, String json, int timeout) throws URISyntaxException, IOException {

        String content = readInputStream(ISdownloadHttpClientJsonPostHelp(url, json, timeout));
        return content;
    }

    public static String downloadHttpClientJsonPost(String url, String json) throws URISyntaxException, IOException {
        return downloadHttpClientJsonPost(url, json, 20000);
    }

    public static InputStream downloadHttpClientJsonPostStream(String url, String json) throws IllegalStateException, IOException, URISyntaxException {
        return ISdownloadHttpClientJsonPostHelp(url, json, 20000);
    }
    //</HTTP Post Json>

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
