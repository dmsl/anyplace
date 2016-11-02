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

package oauth.provider.v2.models;

import com.fasterxml.jackson.databind.JsonNode;
import play.mvc.Http;
import utils.CORSHelper;

import java.util.Map;

/**
 * This class will provide easy tools to handle HTTP Requests
 * in order to easily integrate OAuth2 implementation.
 *
 */
public class OAuth2Request {

    Http.Request mRequest;
    Http.Response mResponse;
    Http.RequestBody mBody;

    JsonNode mJsonBody;
    Map<String, String[]> mFormBody;

    /**
     * Creates an OAuthRequest from the request and the response objects.
     * It tries to convert the body into a JsonNode.
     * If not a Json conversion is possible then a FormUrlEncoded transformation.
     * If not that either the body remains as is.
     * CORS is enabled by default with this constructor.
     *
     * @param request The request object that invoked this call
     * @param response The response object that will be returned
     */
    public OAuth2Request(Http.Request request, Http.Response response){
        this( request, response, true );
    }

    /**
     * Creates an OAuthRequest from the request and the response objects.
     * It tries to convert the body into a JsonNode.
     * If not a Json conversion is possible then a FormUrlEncoded transformation.
     * If not that either the body remains as is.
     *
     * @param request The request object that invoked this call
     * @param response The response object that will be returned
     * @param enableCORS If True enables CORS, sets the Access-Control-Allow-Origin=*
     */
    public OAuth2Request(Http.Request request, Http.Response response, boolean enableCORS){
        this.mRequest = request;
        this.mResponse = response;
        //this.mResponse.setContentType("application/json; charset=utf-8");
        if( enableCORS ){
            // IMPORTANT - to allow cross domain requests
            this.mResponse.setHeader("Access-Control-Allow-Origin", CORSHelper.checkOriginFromRequest(this.mRequest));
            // allows session cookies to be transferred
            this.mResponse.setHeader("Access-Control-Allow-Credentials", "true");
        }
        this.mBody = this.mRequest.body();
        if( !assertJsonBody() ){
            assertFormUrlEncodedBody();
        }
    }

    /**
     * Make sure that the body can be parsed as a valid Json object.
     * If not already converted it tries to convert the body into Json first.
     * The transformed Json body can be later retrieved with @getJsonBody()
     *
     * @return True if the body parsed as Json, False otherwise
     */
    public boolean assertJsonBody(){
        if( this.mJsonBody == null )
            this.mJsonBody = this.mBody.asJson();
        return this.mJsonBody != null;
    }

    /**
     * Checks if the body has been parsed as Json.
     * @return True if there is a parsed Json body, False otherwise
     */
    public boolean hasJsonBody(){
        return this.mJsonBody != null;
    }

    /**
     * If the body has been parsed as Json using @assertJsonBody() that Json object
     * is returned, otherwise a call to that method is invoked and then the object
     * is returned.
     * @return The JsonNode of the body parsed as Json, or null if not possible.
     */
    public JsonNode getJsonBody(){
        if( this.mJsonBody == null )
            assertJsonBody();
        return this.mJsonBody;
    }


    /**
     * Make sure that the body can be parsed as a valid Form Url encoded map.
     * The transformed Json body can be later retrieved with @getFormEncodedBody()
     *
     * @return True if the body parsed as FormUrlEncoded, False otherwise
     */
    public boolean assertFormUrlEncodedBody(){
        this.mFormBody = this.mBody.asFormUrlEncoded();
        if( this.mFormBody == null ){
            return false;
        }
        return true;
    }

    /**
     * Checks if the body has been parsed as Json.
     * @return True if there is a parsed Json body, False otherwise
     */
    public boolean hasFormEncodedBody(){
        return this.mJsonBody != null;
    }

    /**
     * If the body has been parsed as Form using @assertFormUrlEncodedBody() that Map object
     * is returned, otherwise a call to that method is invoked and then the object
     * is returned.
     * @return The body parsed as Map<String, String[]>, or null if not possible.
     */
    public Map<String, String[]> getFormEncodedBody(){
        return this.mFormBody;
    }

    /**
     * If the body can be parsed as MultipartFormData then we return
     * the multipart data representation of the body.
     *
     * @return MultipartFormData object or null
     */
    public Http.MultipartFormData getMultipartFormData(){
        return this.mRequest.body().asMultipartFormData();
    }


    public String getHeader(String header){
        if( header == null || header.trim().isEmpty() ){
            throw new IllegalArgumentException("No null/empty headers allowed!");
        }
        return this.mRequest.getHeader(header);
    }

    public String setHeader(String header, String value){
        if( header == null || value == null
                || header.trim().isEmpty() || value.trim().isEmpty()){
            throw new IllegalArgumentException("No null/empty headers allowed!");
        }
        String old = this.mResponse.getHeaders().get(header);
        this.mResponse.setHeader(header, value);
        return old;
    }

    public String getParameter(String property){
        if( hasJsonBody() ){
            return this.mJsonBody.path(property).textValue();
        }else if( hasFormEncodedBody() ){
            return this.mFormBody.get(property)[0];
        }
        return null;
    }

}
