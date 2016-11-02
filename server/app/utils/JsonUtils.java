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
 * URL: https://anyplace.cs.ucy.ac.cy
 * Contact: anyplace@cs.ucy.ac.cy
 *
 * Copyright (c) 2016, Data Management Systems Lab (DMSL), University of Cyprus.
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

package utils;

import com.fasterxml.jackson.databind.node.*;
import com.fasterxml.jackson.databind.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * A utility class that provides useful functions regarding JSON
 *
 */
public class JsonUtils {

    private static final ObjectMapper sObjectMapper;
    private static final ObjectReader sObjectReader;

    static {
        sObjectMapper = new ObjectMapper();
        sObjectReader = sObjectMapper.reader();
    }

    /**
     * Returns an empty ObjectNode
     * @return A new empty ObjectNode
     */
    public static ObjectNode createObjectNode(){
        return JsonNodeFactory.instance.objectNode();
    }

    /**
     * Reads the passed in String and constructs a JsonNode from it.
     * @param s The String representation of a Json object
     * @return The JsonNode object constructed from the string s
     * @throws IOException
     */
    public static JsonNode getJsonTree(String s) throws IOException {
        return sObjectReader.readTree(s);
    }

    /**
     * Returns a JsonNode from the passed in List
     * @param l
     * @return
     */
    public static JsonNode getJsonFromList(List l){
        return sObjectMapper.valueToTree(l);
        //return sObjectMapper.convertValue(l, JsonNode.class);
    }

    /**
     * Converts the passed in Json string into a HashMap<String,String>.
     * If not successful an empty HashMap is returned
     *
     * @param jsonString The Json object string
     * @return The HashMap denoted by the jsonString or an empty HashMap
     */
    public static HashMap<String,String> getHashMapStrStr(String jsonString){
        try {
            return sObjectMapper.readValue(jsonString, HashMap.class);
        } catch (IOException e) {
            return new HashMap<>();
        }
    }



    /**
     * For each of the keys we get the value from the JsonNode object and put it
     * inside the second argument Map object.
     *
     * @param json The Json object from which we will check the values
     * @param map The destination map where we will put the extracted values
     * @param keys The keys/properties we want to extract from the JsonNode
     * @return A list with the missing keys/properties not found in the JsonNode
     */
    public static List<String> fillMapFromJson(JsonNode json, HashMap<String, String> map, String... keys){
        if( json == null || map == null ){
            throw new IllegalArgumentException("No source Json object or destination Map object can be null!");
        }
        if( keys == null ){
            return Collections.emptyList();
        }
        List<String> notFound = new ArrayList<String>();
        for( String k : keys ){
            String value = json.path(k).textValue();
            if( value == null )
                notFound.add(k);
            else
                map.put(k, value);
        }
        return notFound;
    }

    /**
     * For each of the keys we check  that the value exists in the JsonNode object.
     *
     * @param json The Json object from which we will check the values
     * @param keys The keys/properties we want to extract from the JsonNode
     * @return A list with the missing keys/properties not found in the JsonNode
     */
    public static List<String> requirePropertiesInJson(JsonNode json, String... keys){
        if( json == null ){
            throw new IllegalArgumentException("No source Json object or destination Map object can be null!");
        }
        if( keys == null ){
            return Collections.emptyList();
        }
        List<String> notFound = new ArrayList<String>();
        for( String k : keys ){

            if(json.path(k).isArray()) {
                continue;
            }

            String value = json.path(k).textValue();
            if( value == null || 0 == value.trim().length() )
                notFound.add(k);
        }
        return notFound;
    }

}
