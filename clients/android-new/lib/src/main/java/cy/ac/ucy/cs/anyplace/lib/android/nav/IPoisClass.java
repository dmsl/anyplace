/*
* AnyPlace: A free and open Indoor Navigation Service with superb accuracy!
*
* Anyplace is a first-of-a-kind indoor information service offering GPS-less
* localization, navigation and search inside buildings using ordinary smartphones.
*
* Author(s): 
* http://stackoverflow.com/questions/17049684/convert-from-json-to-multiple-unknown-java-object-types-using-gson
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

package cy.ac.ucy.cs.anyplace.lib.android.nav;

import java.io.Serializable;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public interface IPoisClass extends Serializable {

	enum Type {
		AnyPlacePOI, GooglePlace;
	}

	public final class MyInterfaceAdapter implements JsonDeserializer<IPoisClass>, JsonSerializer<IPoisClass> {
		private static final String PROP_NAME = "myClass";

		@Override
		public JsonElement serialize(IPoisClass src, java.lang.reflect.Type arg1, JsonSerializationContext context) {
			// note : won't work, you must delegate this
			JsonObject jo = context.serialize(src).getAsJsonObject();

			String classPath = src.getClass().getName();
			jo.add(PROP_NAME, new JsonPrimitive(classPath));

			return jo;
		}

		@Override
		public IPoisClass deserialize(JsonElement json, java.lang.reflect.Type arg1, JsonDeserializationContext context) throws JsonParseException {
			try {
				String classPath = json.getAsJsonObject().getAsJsonPrimitive(PROP_NAME).getAsString();
				Class<IPoisClass> cls = (Class<IPoisClass>) Class.forName(classPath);

				return (IPoisClass) context.deserialize(json, cls);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}

			return null;
		}

	}

	String id();

	double lat();

	double lng();

	String name();

	String description();

	Type type();
}
