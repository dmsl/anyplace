/*
* AnyPlace: A free and open Indoor Navigation Service with superb accuracy!
*
* Anyplace is a first-of-a-kind indoor information service offering GPS-less
* localization, navigation and search inside buildings using ordinary smartphones.
*
* Author(s): 
* //http://blog-emildesign.rhcloud.com/?p=822
* Timotheos Constambeys
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

package cy.ac.ucy.cs.anyplace.lib.android.googleapi;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import cy.ac.ucy.cs.anyplace.lib.android.utils.GeoPoint;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.PolyUtil;

import android.util.Log;

public class GMapV2Direction {

	public final static String MODE_DRIVING = "driving";
	public final static String MODE_WALKING = "walking";

	public GMapV2Direction() {
	}

	public Document getDocument(double fromLatitude, double fromLongitude, GeoPoint toPosition, String mode) throws Exception {
		String url = "http://maps.googleapis.com/maps/api/directions/xml?" + "origin=" + fromLatitude + "," + fromLongitude + "&destination=" + toPosition.lat + "," + toPosition.lng
				+ "&sensor=false&units=metric&mode=" + mode;

		HttpParams httpParameters = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(httpParameters, 20000); // 20seconds
		HttpConnectionParams.setSoTimeout(httpParameters, 20000); // 20 seconds

		HttpClient httpClient = new DefaultHttpClient(httpParameters);
		HttpPost httpPost = new HttpPost(url);
		HttpContext localContext = new BasicHttpContext();

		HttpResponse response = httpClient.execute(httpPost, localContext);

		InputStream in = response.getEntity().getContent();
		DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document doc = builder.parse(in);
		return doc;

	}

	public String getDurationText(Document doc) {
		NodeList nl1 = doc.getElementsByTagName("duration");
		Node node1 = nl1.item(0);
		NodeList nl2 = node1.getChildNodes();
		Node node2 = nl2.item(getNodeIndex(nl2, "text"));
		Log.i("DurationText", node2.getTextContent());
		return node2.getTextContent();
	}

	public int getDurationValue(Document doc) {
		NodeList nl1 = doc.getElementsByTagName("duration");
		Node node1 = nl1.item(0);
		NodeList nl2 = node1.getChildNodes();
		Node node2 = nl2.item(getNodeIndex(nl2, "value"));
		Log.i("DurationValue", node2.getTextContent());
		return Integer.parseInt(node2.getTextContent());
	}

	public String getDistanceText(Document doc) {
		NodeList nl1 = doc.getElementsByTagName("distance");
		Node node1 = nl1.item(0);
		NodeList nl2 = node1.getChildNodes();
		Node node2 = nl2.item(getNodeIndex(nl2, "text"));
		Log.i("DistanceText", node2.getTextContent());
		return node2.getTextContent();
	}

	public int getDistanceValue(Document doc) {
		NodeList nl1 = doc.getElementsByTagName("distance");
		Node node1 = nl1.item(0);
		NodeList nl2 = node1.getChildNodes();
		Node node2 = nl2.item(getNodeIndex(nl2, "value"));
		Log.i("DistanceValue", node2.getTextContent());
		return Integer.parseInt(node2.getTextContent());
	}

	public String getStartAddress(Document doc) {
		NodeList nl1 = doc.getElementsByTagName("start_address");
		Node node1 = nl1.item(0);
		Log.i("StartAddress", node1.getTextContent());
		return node1.getTextContent();
	}

	public String getEndAddress(Document doc) {
		NodeList nl1 = doc.getElementsByTagName("end_address");
		Node node1 = nl1.item(0);
		Log.i("StartAddress", node1.getTextContent());
		return node1.getTextContent();
	}

	public String getCopyRights(Document doc) {
		NodeList nl1 = doc.getElementsByTagName("copyrights");
		Node node1 = nl1.item(0);
		Log.i("CopyRights", node1.getTextContent());
		return node1.getTextContent();
	}

	public ArrayList<LatLng> getDirection(Document doc) {
		NodeList nl1, nl2, nl3;
		ArrayList<LatLng> listGeopoints = new ArrayList<LatLng>();
		nl1 = doc.getElementsByTagName("step");
		if (nl1.getLength() > 0) {
			for (int i = 0; i < nl1.getLength(); i++) {
				Node node1 = nl1.item(i);
				nl2 = node1.getChildNodes();

				Node locationNode = nl2.item(getNodeIndex(nl2, "start_location"));
				nl3 = locationNode.getChildNodes();
				Node latNode = nl3.item(getNodeIndex(nl3, "lat"));
				double lat = Double.parseDouble(latNode.getTextContent());
				Node lngNode = nl3.item(getNodeIndex(nl3, "lng"));
				double lng = Double.parseDouble(lngNode.getTextContent());
				listGeopoints.add(new LatLng(lat, lng));

				locationNode = nl2.item(getNodeIndex(nl2, "polyline"));
				nl3 = locationNode.getChildNodes();
				latNode = nl3.item(getNodeIndex(nl3, "points"));
				//ArrayList<LatLng> arr = decodePoly(latNode.getTextContent());
				List<LatLng> arr = PolyUtil.decode(latNode.getTextContent());
				for (int j = 0; j < arr.size(); j++) {
					listGeopoints.add(new LatLng(arr.get(j).latitude, arr.get(j).longitude));
				}

				locationNode = nl2.item(getNodeIndex(nl2, "end_location"));
				nl3 = locationNode.getChildNodes();
				latNode = nl3.item(getNodeIndex(nl3, "lat"));
				lat = Double.parseDouble(latNode.getTextContent());
				lngNode = nl3.item(getNodeIndex(nl3, "lng"));
				lng = Double.parseDouble(lngNode.getTextContent());
				listGeopoints.add(new LatLng(lat, lng));
			}
		}

		return listGeopoints;
	}

	private int getNodeIndex(NodeList nl, String nodename) {
		for (int i = 0; i < nl.getLength(); i++) {
			if (nl.item(i).getNodeName().equals(nodename))
				return i;
		}
		return -1;
	}

	private ArrayList<LatLng> decodePoly(String encoded) {
		ArrayList<LatLng> poly = new ArrayList<LatLng>();
		int index = 0, len = encoded.length();
		int lat = 0, lng = 0;
		while (index < len) {
			int b, shift = 0, result = 0;
			do {
				b = encoded.charAt(index++) - 63;
				result |= (b & 0x1f) << shift;
				shift += 5;
			} while (b >= 0x20);
			int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
			lat += dlat;
			shift = 0;
			result = 0;
			do {
				b = encoded.charAt(index++) - 63;
				result |= (b & 0x1f) << shift;
				shift += 5;
			} while (b >= 0x20);
			int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
			lng += dlng;

			LatLng position = new LatLng((double) lat / 1E5, (double) lng / 1E5);
			poly.add(position);
		}
		return poly;
	}
}