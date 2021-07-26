/*
 * AnyPlace: A free and open Indoor Navigation Service with superb accuracy!
 *
 * Anyplace is a first-of-a-kind indoor information service offering GPS-less
 * localization, navigation and search inside buildings using ordinary smartphones.
 *
 * Author(s): Constantinos Costa, Kyriakos Georgiou, Lambros Petrou
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
package utils

import java.util

import play.api.libs.json.Json

import play.api.libs.json.{JsNumber, JsObject, JsString, JsValue}
import play.api.mvc.Results.Ok
import play.api.mvc._
import utils.RESPONSE.Response.Response

import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.language.implicitConversions

object RESPONSE {
    val ERROR_JSON_PARSE = "Cannot parse request body as Json object."
    val ERROR_API_USAGE = "Wrong API usage."

    object Response extends Enumeration {
        val BAD_REQUEST = new Response()
        val OK = new Response()
        val FORBIDDEN = new Response()
        val UNAUTHORIZED_ACCESS = new Response()
        val INTERNAL_SERVER_ERROR = new Response()
        val NOT_FOUND = new Response()

        class Response extends Val

        implicit def convertValue(v: Value): Response = v.asInstanceOf[Response]
    }

    private def CreateResultResponse(r: Response, json_in: JsValue, message: String): Result = {
        var obj: JsValue = json_in
        if (obj == null)
            obj = Json.obj()

        r match {
            case Response.BAD_REQUEST =>
                val res: JsObject = obj.as[JsObject] +
                  ("status" -> JsString("error")) +
                  ("message" -> JsString(message)) +
                  ("status_code" -> JsNumber(400))
                Results.BadRequest(res.toString)

            case Response.OK =>
                val res: JsObject = obj.as[JsObject] +
                  ("status" -> JsString("success")) +
                  ("message" -> JsString(message)) +
                  ("status_code" -> JsNumber(200))
                Results.Ok(res.toString)

            case Response.FORBIDDEN =>
                val res: JsObject = obj.as[JsObject] +
                  ("status" -> JsString("error")) +
                  ("message" -> JsString(message)) +
                  ("status_code" -> JsNumber(401))
                Results.Forbidden(res.toString)

            case Response.UNAUTHORIZED_ACCESS =>
                val res: JsObject = obj.as[JsObject] +
                  ("status" -> JsString("error")) +
                  ("message" -> JsString(message)) +
                  ("status_code" -> JsNumber(403))
                Results.Unauthorized(res.toString)

            case Response.INTERNAL_SERVER_ERROR =>
                val res: JsObject = obj.as[JsObject] +
                  ("status" -> JsString("error")) +
                  ("message" -> JsString(message)) +
                  ("status_code" -> JsNumber(500))
                Results.InternalServerError(res.toString)

            case Response.NOT_FOUND =>
                val res: JsObject = obj.as[JsObject] +
                  ("status" -> JsString("error")) +
                  ("message" -> JsString(message)) +
                  ("status_code" -> JsNumber(404))
                Results.NotFound(res.toString)

            case _ =>
                val res: JsObject = obj.as[JsObject] +
                  ("status" -> JsString("error")) +
                  ("message" -> JsString("Unknown Action")) +
                  ("status_code" -> JsNumber(403))
                Results.BadRequest(res.toString)
        }
    }
    def OK(json: JsValue, msg: String): Result = CreateResultResponse(Response.OK, json, msg)
    def BAD(json: JsValue, msg: String): Result = CreateResultResponse(Response.BAD_REQUEST, json, msg)

    def OK(msg: String): Result = CreateResultResponse(Response.OK, null, msg)
    def DEPRECATED(msg: String): Result = CreateResultResponse(Response.BAD_REQUEST, null, "Deprecated API endpoint: " + msg)
    def BAD(msg: String): Result = CreateResultResponse(Response.BAD_REQUEST, null, msg)
    def FORBIDDEN(msg: String): Result = CreateResultResponse(Response.FORBIDDEN, null, msg)
    def UNAUTHORIZED(msg: String): Result = CreateResultResponse(Response.UNAUTHORIZED_ACCESS, null, msg)

    private def prettyException(e: Exception): String = s"500: ${e.getClass}: ${e.getMessage}"

    def ERROR(e: Exception): Result = {
        CreateResultResponse(Response.INTERNAL_SERVER_ERROR, null, prettyException(e))
    }

    def ERROR(tag: String, e: Exception): Result = {
        val msg = tag+": "+prettyException(e)
        CreateResultResponse(Response.INTERNAL_SERVER_ERROR, null, msg)
    }

    def internal_server_error(msg: String): Result =
        CreateResultResponse(Response.INTERNAL_SERVER_ERROR, null, msg)

    def NOT_FOUND(msg: String): Result = CreateResultResponse(Response.NOT_FOUND, null, msg)

    def MISSING_FIELDS(missing: util.List[String]): Result = {
        if (missing == null) { throw new IllegalArgumentException("No null List of missing keys allowed.") }
        val error_messages = new java.util.ArrayList[String]
        for (s <- missing.asScala) {
            error_messages.add(String.format("Missing or Invalid parameter: %s", s))
        }
        val json = Json.obj("error_messages" -> error_messages.asScala)
        CreateResultResponse(Response.BAD_REQUEST, json, String.format("Missing or Invalid parameter: %s",
            missing.get(0)))
    }

    def gzipJsonOk(json: JsValue, message: String): Result = {
        val tempJson = json.as[JsObject] + ("message" -> JsString(message))
        gzipJsonOk(tempJson.toString())
    }

    def gzipJsonOk(body: String): Result = {
        val gzipv = Utils.gzip(body)
        Ok(gzipv.toByteArray).withHeaders(("Content-Encoding", "gzip"),
            ("Content-Length", gzipv.size.toString),
            ("Content-Type", "application/json"))
    }

    def gzipOk(body: String): Result = {
        val gzipv = Utils.gzip(body)
        Ok(gzipv.toByteArray).withHeaders(("Content-Encoding", "gzip"),
            ("Content-Length", gzipv.size.toString))
    }
}
