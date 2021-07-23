/*
 * Anyplace: A free and open Indoor Navigation Service with superb accuracy!
 *
 * Anyplace is a first-of-a-kind indoor information service offering GPS-less
 * localization, navigation and search inside buildings using ordinary smartphones.
 *
 * Author(s): Paschalis Mpeis
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

import java.io.{PrintWriter, StringWriter}
import play.api.http.HttpErrorHandler

import scala.concurrent._
import javax.inject.Singleton
import play.api.mvc._
import play.api.mvc.Results._
import utils.{RESPONSE, LOG, Utils}

// TODO: will only emit json for all endpoints that contain /api
@Singleton
class ErrorHandler extends HttpErrorHandler {

    def infoGithub(eid: String) : String = {
        //"\n\n\nIf you think this is an error, open a new issue at:" +
        //"\nhttps://github.com/dmsl/anyplace/issues"  +
        "ErrorID:" + eid
    }

    def errorMsg(request: RequestHeader): String = {
        // can also use request.path instead of uri
        "Requested: " + request.method + " "  + request.uri +
            " SSL:" + request.secure + " HOST:" + request.host
    }

    def fullStacktrace(exception: Throwable): String= {
        val sw = new StringWriter
        exception.printStackTrace(new PrintWriter(sw))
       "StackTrace: " + sw.toString
    }

    def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = {
        // do NOT log any errors regarding this legacy code.
        // Remove this dependency if is not used
        if(request.path ==
        "/architect/bower_components/angularjs-dropdown-multiselect/pages/images/hr.png") {
            return Future.successful(Status(statusCode))
        }

        // API requests return an API answer.
        if(request.path.startsWith("/api")) {
           return Future.successful(RESPONSE.BAD("No such endpoint."))
        }

        val eid = Utils.genErrorUniqueID()
        val errInt = "404: ID: " + eid
        val errPub =  "Client Error: 404: Error ID: " + eid

        LOG.E(errInt + " " + errorMsg(request))

        Future.successful(Status(statusCode)(
            errPub + "\n\n\n" + errorMsg(request) + infoGithub(eid)))
    }

    def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = {
        val eid = Utils.genErrorUniqueID()
        val errInt = "500: ID: " + eid + ":"
        val errPub =  "500 Internal Server Error: Error ID: " + eid

        LOG.E(errInt + " " + errorMsg(request))

        LOG.E("Message: " + exception.getMessage)
        LOG.E("Cause: " + exception.getCause)
        if(request.path.startsWith("/api")) { // API requests return JSON
            return Future.successful(RESPONSE.BAD("No such endpoint."))
        }

        if (exception.isInstanceOf[MatchError]) {
            // CHECK if OK leave like this
            LOG.E("Skip full stacktrace?")
        } else {
            LOG.E("StackTrace: " + fullStacktrace(exception))
        }

      //  Handle:
      //  p.c.s.n.PlayDefaultU | 24/07/20 01:37:58 | ERROR | Exception caught in Netty
      //  java.lang.IllegalArgumentException: empty text
        val msg =  errPub + "\n\n\n" + errorMsg(request) +
          "\nCause: " + exception.getMessage + infoGithub(eid)

        if(request.path.startsWith("/api")) { // return JSON
            return Future.successful(RESPONSE.BAD(msg))
        }  else {
            return Future.successful(InternalServerError(msg))
        }
    }
}
