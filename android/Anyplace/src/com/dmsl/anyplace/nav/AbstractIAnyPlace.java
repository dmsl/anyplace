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

package com.dmsl.anyplace.nav;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class AbstractIAnyPlace implements IAnyPlace {

    /**
	 * 
	 */
	private static final long serialVersionUID = -5969867318967773899L;
	private String id;
    private String name;
	private Double lat;
	private Double lng;
	private String description;
	private Type type;
    
	// used by de-serializer
	public AbstractIAnyPlace() {}
	
	public AbstractIAnyPlace( JSONObject json ){
		id = json.optString("id", "");
		name = json.optString("name", "");
		description = json.optString("description", "");
		lat = json.optDouble("lat", 0.0f);
		lng = json.optDouble("lng", 0.0f);
		type = IAnyPlace.Type.fromKey(json.optString("type"));
	}
	
	@Override
	public String id() {
		return id;
	}

	@Override
	public double lat() {
		return lat;
	}

	@Override
	public double lng() {
		return lng;
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public String description() {
		return description;
	}

	@Override
	public Type type() {
		return type;
	}

	@Override
	public String toJSON() {
		try {
			JSONObject json = new JSONObject();
			json.put("lat", String.valueOf(lat()));
			json.put("lng", String.valueOf(lng()));
			json.put("name", name());
			json.put("description", description());
			json.put("id", id());
			json.put("type", type());
			return json.toString();
		} catch (JSONException e) {
			return "{}";
		}
	}
	
	/**
	 * Gson custom serializer
	 * @author Lambros Petrou
	 *
	 */
	public static class AbstractIAnyPlaceSerializer implements JsonSerializer<AbstractIAnyPlace> {
		@Override
		public JsonElement serialize(AbstractIAnyPlace src, java.lang.reflect.Type arg1,
				JsonSerializationContext context) {
			JsonObject json = new JsonObject();
			json.addProperty("lat", String.valueOf(src.lat()));
			json.addProperty("lng", String.valueOf(src.lng()));
			json.addProperty("name", src.name());
			json.addProperty("description", src.description());
			json.addProperty("id", src.id());
			json.add("type", context.serialize(src.type()));
			return json;
		}
	}

}
