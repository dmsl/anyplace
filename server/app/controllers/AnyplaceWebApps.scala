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
package controllers

import play.mvc.{Http, Security}
import play.api.mvc.{Action, Result}
import play.Play

object AnyplaceWebApps extends play.api.mvc.Controller {

  def AddTrailingSlash() = Action { implicit request =>
    MovedPermanently(request.path + "/")
  }

  def serveArchitect2(file: String) = Action {
    val archiDir = "public/anyplace_architect"
    serveFile(archiDir, file)
  }

  def servePortal(file: String) = Action {
    val viewerDir = "web_apps/anyplace_portal"
    serveFile(viewerDir, file)
  }

  def servePortalTos(file: String) = Action {
    val viewerDir = "web_apps/anyplace_portal/tos"
    serveFile(viewerDir, file)
  }

  def servePortalPrivacy(file: String) = Action {
    val viewerDir = "web_apps/anyplace_portal/privacy"
    serveFile(viewerDir, file)
  }

  def servePortalMail(file: String) = Action {
    val viewerDir = "web_apps/anyplace_portal/mail"
    serveFile(viewerDir, file)
  }

  def serveViewer(file: String) = Action { implicit request =>
    val mode = request.getQueryString("mode").getOrElse("")
    var viewerDir = "public/anyplace_viewer"
    if (mode == null || !mode.equalsIgnoreCase("widget")) {
      var bid = request.getQueryString("buid").getOrElse("")
      var pid = request.getQueryString("selected").getOrElse("")
      var floor = request.getQueryString("floor").getOrElse("")
      var campus = request.getQueryString("cuid").orNull

      if (null == campus) {
        campus = ""
        viewerDir = "public/anyplace_viewer"
      } else {
        viewerDir = "public/anyplace_viewer_campus"
      }

    }
    serveFile(viewerDir, file)
  }

  def serveViewer2(file: String) = Action { request =>
    val mode = request.getQueryString("mode").getOrElse("")
    if (mode == null || !mode.equalsIgnoreCase("widget")) {
      var bid = request.getQueryString("buid").getOrElse("")
      var pid = request.getQueryString("selected").getOrElse("")
      var floor = request.getQueryString("floor").getOrElse("")
    }
    val viewerDir = "public/anyplace_viewer_campus"
    serveFile(viewerDir, file)
  }

  def serveDevelopers(file: String) = Action {
    val devsDir = "public/anyplace_developers"
    serveFile(devsDir, file)
  }

  def parseCookieForUsername(request: Http.Request): String = {
    val cookie = request.cookie("PLAY_SESSION").value()
    val data = cookie.substring(cookie.indexOf('-') + 1)
    val pairs = data.split(" ")
    for (pair <- pairs) {
      val dat = pair.split("%3A")
      val key = dat(0)
      val value = dat(1)
      if (key == "username") {
        return value
      }
    }
    ""
  }

  // The action that serves the Admin website
  @Security.Authenticated(classOf[Secured])
  def serveAdmin(file: String) = Action {
    val accountsDir = "web_apps/anyplace_accounts"
    serveFile(accountsDir, file)
  }

  def serveFile(appDir: String, file_in: String): Result = {
    var header = ("", "")
    var file_str = file_in

    if (file_str == null) {
      return NotFound("")
    }
    if (file_str.trim().isEmpty || file_str.trim() == "index.html") {
      file_str = "index.html"
      header = ("Content-Disposition", "inline")
    }
    val reqFile = appDir + "/" + file_str
    val file = Play.application().resourceAsStream(reqFile)

    if (file != null)
      Ok(scala.io.Source.fromInputStream(file, "UTF-8").mkString).as("text/html")
    else
      NotFound
  }
}