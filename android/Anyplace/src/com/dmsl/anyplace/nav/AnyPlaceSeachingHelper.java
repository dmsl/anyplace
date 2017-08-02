/*
 * AnyPlace: A free and open Indoor Navigation Service with superb accuracy!
 *
 * Anyplace is a first-of-a-kind indoor information service offering GPS-less
 * localization, navigation and search inside buildings using ordinary smartphones.
 *
 * Author(s): Timotheos Constambeys, Lambros Petrou
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

package com.dmsl.anyplace.nav;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.SearchManager;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.provider.BaseColumns;
import android.support.v4.widget.CursorAdapter;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class AnyPlaceSeachingHelper {

	public enum SearchTypes {
		INDOOR_MODE, OUTDOOR_MODE
	}

	public static SearchTypes getSearchType(float zoomLevel) {
		if (zoomLevel >= 19) {
			return SearchTypes.INDOOR_MODE;
		} else {
			return SearchTypes.OUTDOOR_MODE;
		}
	}

	public static Cursor prepareSearchViewCursor(List<? extends IPoisClass> places) {
		String req_columns[] = { BaseColumns._ID, SearchManager.SUGGEST_COLUMN_TEXT_1, SearchManager.SUGGEST_COLUMN_TEXT_2, SearchManager.SUGGEST_COLUMN_INTENT_DATA };
		MatrixCursor mcursor = new MatrixCursor(req_columns);
		int i = 0;
		if (places != null) {
			GsonBuilder gsonBuilder = new GsonBuilder();
			gsonBuilder.registerTypeAdapter(IPoisClass.class, new IPoisClass.MyInterfaceAdapter());
			Gson gson = gsonBuilder.create();
			for (IPoisClass p : places) {
				mcursor.addRow(new String[] { Integer.toString(i++), p.name(), p.description().equals("") ? "no description" : p.description(), gson.toJson(p, IPoisClass.class) });
			}
		}
		return mcursor;
	}

	public static Cursor prepareSearchViewCursor(List<? extends IPoisClass> places, String query) {
		String req_columns[] = { BaseColumns._ID, SearchManager.SUGGEST_COLUMN_TEXT_1, SearchManager.SUGGEST_COLUMN_INTENT_DATA };
		MatrixCursor mcursor = new MatrixCursor(req_columns);
		int i = 0;
		if (places != null) {
			GsonBuilder gsonBuilder = new GsonBuilder();
			gsonBuilder.registerTypeAdapter(IPoisClass.class, new IPoisClass.MyInterfaceAdapter());
			Gson gson = gsonBuilder.create();

			// Better Use AndroidUtils.fillTextBox instead of regular expression
			// Regular expression
			// ?i ignore case
			Pattern patternTitle = Pattern.compile(String.format("((?i)%s)", query));
			// Matches X words before query and Y words after
			Pattern patternDescription = Pattern.compile(String.format("(([^\\s]+\\s+){0,%d}[^\\s]*)((?i)%s)([^\\s]*(\\s+[^\\s]+){0,%d})", 2, query, 2));
			for (IPoisClass p : places) {
				String name = "", description = "";
				Matcher m;
				m = patternTitle.matcher(p.name());
				// Makes matched query bold using HTML format
				// $1 returns the regular's expression outer parenthesis value
				name = m.replaceAll("<b>$1</b>");

				m = patternDescription.matcher(p.description());
				if (m.find()) {
					// Makes matched query bold using HTML format
					description = String.format(" %s<b>%s</b>%s", m.group(1), m.group(3), m.group(4));
				}

				mcursor.addRow(new String[] { Integer.toString(i++), name + description, gson.toJson(p, IPoisClass.class) });
			}
		}
		return mcursor;
	}

	public static IPoisClass getClassfromJson(String data) {
		// HANDLE BOTH GooglePlace AND AnyplacePoi
		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.registerTypeAdapter(IPoisClass.class, new IPoisClass.MyInterfaceAdapter());
		Gson gson = gsonBuilder.create();
		IPoisClass result = (IPoisClass) gson.fromJson(data, IPoisClass.class);
		// String name = result.getClass().toString();
		return result;
	}

	public static class HTMLCursorAdapter extends CursorAdapter {

		private LayoutInflater mInflater;
		private int layout;
		private String[] from;
		private int[] to;

		public HTMLCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to) {
			super(context, c);
			this.layout = layout;
			this.from = from;
			this.to = to;
			mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			for (int i = 0; i < to.length; i++) {
				TextView textView = (TextView) view.findViewById(to[i]);
				String text = cursor.getString(cursor.getColumnIndex(from[i]));
				textView.setText(Html.fromHtml(text));
			}
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			return mInflater.inflate(layout, parent, false);
		}

	}
}
