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

package com.dmsl.anyplace.provider;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import com.dmsl.anyplace.cache.AnyplaceCache;
import com.dmsl.anyplace.googleapi.GooglePlaces;
import com.dmsl.anyplace.googleapi.Place;
import com.dmsl.anyplace.googleapi.PlacesList;
import com.dmsl.anyplace.nav.AbstractIAnyPlace;
import com.dmsl.anyplace.nav.IAnyPlace;
import com.dmsl.anyplace.nav.PoisModel;
import com.dmsl.anyplace.nav.IAnyPlace.Type;
import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

/**
 * This Provider provides the Points of Interest currently loaded inside a
 * Cursor object. TODO - Use an SQLite db instead of the AnyplaceCache
 * 
 * @author Lambros Petrou
 * 
 */
public class AnyplacePOIProvider extends ContentProvider {

	// public constants for client development
	public static final String AUTHORITY = AnyplacePOIProvider.class.getName();
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY
			+ "/");
	public static final Uri CONTENT_URI_ANYPLACE = Uri.parse(CONTENT_URI
			+ "anyplace/");
	public static final Uri CONTENT_URI_GOOGLE_PLACES = Uri.parse(CONTENT_URI
			+ "googleplaces/");

	// helper constants for use with the UriMatcher
	public static final int POI_GOOGLE_PLACES = 1;
	public static final int POI_ANYPLACE = 2;
	private static final UriMatcher URI_MATCHER;

	// prepare the UriMatcher
	static {
		URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		URI_MATCHER.addURI(AUTHORITY, "googleplaces/"
				+ SearchManager.SUGGEST_URI_PATH_QUERY + "/*",
				POI_GOOGLE_PLACES);
		URI_MATCHER.addURI(AUTHORITY, "anyplace/"
				+ SearchManager.SUGGEST_URI_PATH_QUERY + "/*", POI_ANYPLACE);
	}

	public static String req_columns[] = { BaseColumns._ID,
			SearchManager.SUGGEST_COLUMN_TEXT_1,
			SearchManager.SUGGEST_COLUMN_TEXT_2,
			SearchManager.SUGGEST_COLUMN_INTENT_DATA };

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	@Override
	public String getType(Uri uri) {
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean onCreate() {
		// initialize any objects needed for the Provider

		// TODO - the SQLite db in the future
		Log.d("poi provider", "provider created");

		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		String query = uri.getLastPathSegment().toLowerCase(Locale.ENGLISH);

		MatrixCursor mcursor = new MatrixCursor(req_columns);

		if (SearchManager.SUGGEST_URI_PATH_QUERY.equals(query)) {
			// empty query
			// AVOID MAKING A NETWORK REQUEST IF EMPTY
		} else {
			// we have a non-empty query

			// the INDEX OF OUR LIST
			int i = 0;

			switch (URI_MATCHER.match(uri)) {
			case POI_ANYPLACE: {
				// here we should get the POIS either from the db or from the
				// arrays
				Collection<PoisModel> pois = AnyplaceCache.getInstance(getContext()).getPois();
				// Log.d("poi provider", "all pois: " + pois.size());

				if (pois != null) {
					for (PoisModel p : pois) {
						// check if the query matches the POI
						if (PoisModel.matchQueryPoi(query, p.name)) {
							// add the matched POI into the suggestions list
							// mcursor.addRow( new
							// String[]{Integer.toString(i++), p.name, p.puid}
							// );
							mcursor.addRow(new String[] {
									Integer.toString(i++),
									p.name,
									p.description.trim().equalsIgnoreCase("-") ? ""
											: p.description, p.puid });
						}
					}
				}

				break;
			}
			case POI_GOOGLE_PLACES: {

				// get the places from the google API
				try {
					PlacesList placesList = GooglePlaces.search(35.144569,
							33.411107, -1, query);
					// Log.d("Provider Places", "results:" +
					// placesList.results);
					if (placesList.results != null) {
						for (Place p : placesList.results) {
							// Log.d("Provider Places", "results:" + p.name);
							// add the matched Google Place into the suggestions
							// list
							// mcursor.addRow( new
							// String[]{Integer.toString(i++), p.name,
							// p.reference} );
							mcursor.addRow(new String[] {
									Integer.toString(i++), p.name,
									p.formatted_address, p.reference });
						}
					}

				} catch (IOException e) {
					Log.d("AnyplacePOIProvider",
							"Error in GooglePlaces: " + e.getMessage());
				}
			}

				break;
			case UriMatcher.NO_MATCH:
				break;
			}
		}// end if non-empty query

		return mcursor;
	}

	/**
	 * Allows someone to get suggestions without using the provider Just passing
	 * in the query string and the Type of search wanted along with the position
	 * of the user. Type: AnyPlacePOI or GooglePlace = IAnyPlace.Type
	 * 
	 * @param query
	 * @param type
	 * @param latlng
	 * @return
	 * @throws IOException
	 */
	public Cursor queryStatic(String query, int type, LatLng latlng)
			throws IOException {

		MatrixCursor mcursor = new MatrixCursor(req_columns);

		if (query.isEmpty() || query.trim().isEmpty()) {
			// empty query
			// AVOID MAKING A NETWORK REQUEST IF EMPTY
		} else {
			// we have a non-empty query

			// the INDEX OF OUR LIST
			int i = 0;

			switch (type) {
			case POI_ANYPLACE: {
				// here we should get the POIS either from the db or from the
				// arrays
				Collection<PoisModel> pois = AnyplaceCache.getInstance(getContext()).getPois();
				// Log.d("poi provider", "all pois: " + pois.size());

				if (pois != null) {
					for (PoisModel p : pois) {
						// check if the query matches the POI
						if (PoisModel.matchQueryPoi(query, p.name)) {
							// add the matched POI into the suggestions list
							// mcursor.addRow( new
							// String[]{Integer.toString(i++), p.name, p.puid}
							// );
							mcursor.addRow(new String[] {
									Integer.toString(i++),
									p.name,
									p.description.trim().equalsIgnoreCase("-") ? "no description"
											: p.description, p.puid });
						}
					}
				}

				break;
			}
			case POI_GOOGLE_PLACES: {
				// get the places from the google API
				PlacesList placesList = GooglePlaces.search(latlng.latitude,
						latlng.longitude, -1, query);
				// Log.d("Provider Places", "results:" + placesList.results);
				if (placesList.results != null) {
					for (Place p : placesList.results) {
						// Log.d("Provider Places", "results:" + p.name);
						// add the matched Google Place into the suggestions
						// list
						// mcursor.addRow( new String[]{Integer.toString(i++),
						// p.name, p.reference} );
						mcursor.addRow(new String[] { Integer.toString(i++),
								p.name, p.formatted_address, p.reference });
					}
				}

				break;
			}
			case UriMatcher.NO_MATCH:
				break;
			}
		}// end if non-empty query

		return mcursor;
	}

	// *****************************************************************
	// 2-STEP SEARCHING BELOW
	// *****************************************************************

	public static Cursor prepareAnyPlacePOIsCursor(List<IAnyPlace> places) {
		MatrixCursor mcursor = new MatrixCursor(req_columns);
		int i = 0;
		if (places != null) {
			GsonBuilder gsonBuilder = new GsonBuilder();
	    	gsonBuilder.registerTypeAdapter(PoisModel.class, new PoisModel.PoisModelSerializer());
	    	Gson gson = gsonBuilder.create();
			for (IAnyPlace p : places) {
				mcursor.addRow(new String[] {
						Integer.toString(i++),
						p.name(),
						p.description().trim().equalsIgnoreCase("-") 
							? "no description"
							: p.description(), 
						gson.toJson(p) });
			}
		}
		return mcursor;
	}

	public static Cursor prepareGooglePlacesCursor(PlacesList places) {
		MatrixCursor mcursor = new MatrixCursor(req_columns);
		int i = 0;
		if (places.results != null) {
			GsonBuilder gsonBuilder = new GsonBuilder();
	    	gsonBuilder.registerTypeAdapter(Place.class, new Place.GooglePlaceSerializer());
	    	Gson gson = gsonBuilder.create();
			for (Place p : places.results) {
				// add the matched Google Place into the suggestions list
				mcursor.addRow(new String[] { 
						Integer.toString(i++), 
						p.name(),
						p.description(), 
						gson.toJson(p) });
			}
		}
		return mcursor;
	}

	  // *****************************************************************************************
    //  Handle ACTION_VIEW searches separately for speedup. You have the suggestion 
    //  selected so just construct the necessary model and return it
    // *****************************************************************************************
    public static IAnyPlace searchBySelectedSuggestion( String data ){
    	// HANDLE BOTH GooglePlace AND AnyplacePoi
    	GsonBuilder gsonBuilder = new GsonBuilder();
    	gsonBuilder.registerTypeAdapter(Type.class, new IAnyPlace.IAnyPlaceTypeDeserializer());
    	Gson gson = gsonBuilder.create();
    	return (IAnyPlace)gson.fromJson(data, AbstractIAnyPlace.class);
    }
}
