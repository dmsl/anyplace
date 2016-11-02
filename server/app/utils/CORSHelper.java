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

import play.mvc.Http;

/**
 * This helper class intends to help handling requests against
 * CORS: Cross Origin Resource Sharing
 *
 * Any allowed origin different than this server should be added to the
 * ALLOWED_CROSS_ORIGINS[] since every request is checked against this array
 * to find a match in order to allow the request to procceed.
 *
 * In order to allow Cross-Origin requests we should set the header:
 *
 *      Access-Control-Allow-Origin : "Allowed origin here"
 *      e.g. Access-Control-Allow-Origin: "http://127.0.0.1:9000"
 *
 * In order to allow Credentials (session, cookies) to be sent from
 * cross origin requests we should add the header:
 *
 *      Access-Control-Allow-Credentials : "true"
 *
 * notice:: The above headr does not allow Access-Control-Allow-Origin to have
 *          a value of wildcard ('*').
 *
 * Any AJAX request to endpoints with credentials should be sent with
 *
 *      withCredentials: true
 *
 */
public class CORSHelper {

    public static final String ALLOWED_CROSS_ORIGINS[] = {
            "http://127.0.0.1:3030",
            "http://localhost:9000",
            "http://127.0.0.1:9000",
            "http://127.0.0.1:8080",
            "http://localhost:8080",
            "http://localhost:8081",
            "http://localhost:3000",
            "http://localhost:8030",
            "http://localhost:63344"
    };

    /**
     * Returns whether the reOrigin is allowed to this server
     * @param reqOrigin The origin to check against our list
     * @return True if valid, otherwise False
     */
    public static boolean isOriginAllowed(String reqOrigin){
        if( reqOrigin == null || reqOrigin.trim().isEmpty() )
            return false;
        for( String allowed : ALLOWED_CROSS_ORIGINS ){
            if( allowed.equals(reqOrigin) ){
                return true;
            }
        }
        return false;
    }

    /**
     * Extracts the Origin from the request object and returns
     * it as String.
     * @param request The request object we want the origin from
     * @return The Origin as String
     */
    public static String getOriginFromRequest(Http.Request request){
        return request.getHeader("Origin");
    }

    /**
     * Checks the requests origin against our acceptable list
     * and returns the valid Origin that should be added to the
     * Response header
     *
     *  Access-Control-Allow-Origin: "origin here"
     *
     * @param request The request object we want to check the origin from
     * @return The valid origin ready for the header or empty string
     */
    public static String checkOriginFromRequest(Http.Request request){
        String origin = getOriginFromRequest(request);
        return isOriginAllowed(origin) ? origin : "";
    }


    /**
     * Sets the required headers for Cross-Origin requests that need to have session
     * and get/send credentials cookies.
     * @param response
     * @param request
     */
    public static void setCORSHeadersForAccounts(Http.Response response, Http.Request request){
        // allow credentials for cross origin
        response.setHeader("Access-Control-Allow-Credentials", "true");
        // set the Origins that are allowed to request ( '*' is not valid )
        response.setHeader("Access-Control-Allow-Origin", CORSHelper.checkOriginFromRequest(request));
        // Only allow POST
        response.setHeader("Access-Control-Allow-Methods", "POST");
        // Ensure this header is also allowed!
        response.setHeader("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept");
    }

    /**
     * Sets the required headers for Cross-Origin requests that need to have session
     * and get/send credentials cookies.
     * @param response
     * @param request
     */
    public static void setCORSHeadersForAPI(Http.Response response, Http.Request request){
        // Cache response for 5 minutes
        //response.setHeader("Access-Control-Max-Age", "300");
        response.setHeader("Access-Control-Allow-Origin", "*");
        // set the Origins that are allowed to request ( '*' is not valid )
        //response.setHeader("Access-Control-Allow-Origin", CORSHelper.checkOriginFromRequest(request));
        // Only allow POST
        response.setHeader("Access-Control-Allow-Methods", "POST");
        // Ensure this header is also allowed!
        response.setHeader("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept, " +
                "X-XSRF-TOKEN");
    }

}
