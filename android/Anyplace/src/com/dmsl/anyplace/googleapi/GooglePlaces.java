/*
* AnyPlace: A free and open Indoor Navigation Service with superb accuracy!
*
* Anyplace is a first-of-a-kind indoor information service offering GPS-less
* localization, navigation and search inside buildings using ordinary smartphones.
*
* Author(s): Lambros Petrou
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

package com.dmsl.anyplace.googleapi;

import java.io.IOException;

import org.apache.http.client.HttpResponseException;

import android.util.Log;

import com.dmsl.anyplace.utils.GeoPoint;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson.JacksonFactory;

public class GooglePlaces {

	/** Global instance of the HTTP transport. */
	private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

	// Google API Key
	private static final String API_KEY = "AIzaSyBhOx9L_6sIo8s771SGKyMvPeJo9e7Kq4k";

	// Google Places serach url's
	private static final String PLACES_AUTOCOMPLETE_URL = "https://maps.googleapis.com/maps/api/place/autocomplete/json?";
	private static final String PLACES_SEARCH_URL = "https://maps.googleapis.com/maps/api/place/search/json?";
	private static final String PLACES_DETAILS_URL = "https://maps.googleapis.com/maps/api/place/details/json?";

	/**
	 * Searching places
	 * 
	 * @param latitude
	 *            - latitude of place
	 * @params longitude - longitude of place
	 * @param radius
	 *            - radius of searchable area
	 * @param types
	 *            - type of place to search
	 * @return list of places
	 * @throws IOException
	 * */
	public static PlacesList autocomplete(double latitude, double longitude, double radius, String query_text) throws IOException {

		try {

			HttpRequestFactory httpRequestFactory = createRequestFactory(HTTP_TRANSPORT);
			HttpRequest request = httpRequestFactory.buildGetRequest(new GenericUrl(PLACES_AUTOCOMPLETE_URL));
			request.getUrl().put("key", API_KEY);
			request.getUrl().put("location", latitude + "," + longitude);
			request.getUrl().put("radius", radius); // in meters
			request.getUrl().put("sensor", "false");
			request.getUrl().put("input", query_text);
			PlacesList list = request.execute().parseAs(PlacesList.class);
			return list;

		} catch (HttpResponseException e) {
			Log.e("Error:", e.getMessage());
			return null;
		}

	}

	public static PlacesList queryStaticGoogle(String query, GeoPoint position) throws IOException {
		PlacesList placesList = GooglePlaces.search(position.dlat, position.dlon, -1, query);
		return placesList;
	}

	/**
	 * Searching places
	 * 
	 * @param latitude
	 *            - latitude of place
	 * @params longitude - longitude of place
	 * @param radius
	 *            - radius of searchable area
	 * @param types
	 *            - type of place to search
	 * @return list of places
	 * @throws IOException
	 * */
	public static PlacesList search(double latitude, double longitude, double radius, String query_text) throws IOException {

		try {

			HttpRequestFactory httpRequestFactory = createRequestFactory(HTTP_TRANSPORT);
			HttpRequest request = httpRequestFactory.buildGetRequest(new GenericUrl(PLACES_SEARCH_URL));
			request.getUrl().put("key", API_KEY);
			request.getUrl().put("location", latitude + "," + longitude);
			request.getUrl().put("rankby", "distance"); // in meters
			request.getUrl().put("sensor", "false");
			request.getUrl().put("keyword", query_text);

			Log.d("Places status", "url: " + request.getUrl().toString());

			PlacesList list = request.execute().parseAs(PlacesList.class);
			// Check log cat for places response status
			Log.d("Places Status", "" + list.status + " size: " + list.results.size());
			return list;

		} catch (HttpResponseException e) {
			Log.e("Error:", e.getMessage());
			return null;
		}

	}

	/**
	 * Searching single place full details
	 * 
	 * @param reference
	 *            - reference id of place - which you will get in search api
	 *            request
	 * */
	public static PlaceDetails getPlaceDetails(String reference) throws Exception {
		try {

			HttpRequestFactory httpRequestFactory = createRequestFactory(HTTP_TRANSPORT);
			HttpRequest request = httpRequestFactory.buildGetRequest(new GenericUrl(PLACES_DETAILS_URL));
			request.getUrl().put("key", API_KEY);
			request.getUrl().put("reference", reference);
			request.getUrl().put("sensor", "false");

			PlaceDetails place = request.execute().parseAs(PlaceDetails.class);

			return place;

		} catch (HttpResponseException e) {
			Log.e("Error in Perform Details", e.getMessage());
			throw e;
		}
	}

	/**
	 * Creating http request Factory
	 * */
	public static HttpRequestFactory createRequestFactory(final HttpTransport transport) {
		return transport.createRequestFactory(new HttpRequestInitializer() {
			public void initialize(HttpRequest request) {
				HttpHeaders headers = new HttpHeaders();
				request.setHeaders(headers);
				JsonObjectParser parser = new JsonObjectParser(new JacksonFactory());
				request.setParser(parser);
			}
		});
	}

}