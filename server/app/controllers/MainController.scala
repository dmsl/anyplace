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

import datasources.SCHEMA
import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import utils.LOG

import scala.language.postfixOps

@Singleton
class MainController @Inject()(cc: ControllerComponents,
                               conf: Configuration,
                               assets: Assets)
  extends AbstractController(cc) {

  def index(): Action[AnyContent] = Action { Redirect("/viewer") }
  def indexAny(): Action[AnyContent]  = index()

  def indexRedirect(any: String): Action[AnyContent] = {
      //if(any.contains("/api")) {
      // TODO: if on API then return json
      //implicit request =>
      //  def inner(request: Request[AnyContent]): Result = {
      //    RESPONSE.BAD(RESPONSE.ERROR_API_USAGE)
      //  }
      //  return inner(request)
      //
      index() // redirect all others to index
  }

  def at(path: String, file: String): Action[AnyContent] = Action.async {
    implicit request =>
      val uri = request.headers.get("referer").getOrElse("")
      var viewerDir = "/anyplace_viewer/"
      val campus = !uri.contains(SCHEMA.fCampusCuid)
      if (campus) {
        viewerDir = "/anyplace_viewer/"
      } else {
        viewerDir = "/anyplace_viewer_campus/"
      }
      assets.at(path + viewerDir, file).apply(request)
  }

  def Version: Action[AnyContent] = Action {
    val version = conf.get[String]("application.version")
    val address = conf.get[String]("server.address")
    val port = conf.get[String]("server.port")
    LOG.D4("PORT: " + port)
    LOG.D4("address: " + address)

    var variant=""
    if (address.contains("ap-dev")) {
      variant = "alpha"
      if (port.equals("443") || port.equals("80")) {
        variant = "beta"
      }
    } else if (address.contains("localhost") || address == "127.0.0.1") {
      variant = "local"
    }

    val res: JsValue = Json.obj(
      "version" -> version,
      "address" -> address,
      "port"-> port,
      "variant" -> variant)
    Ok(res)
  }

  // CHECK:PM CHECK:NN ??
  //  def Architect = Action {
  //    Ok(views.html.architect())
  //  }
  //
  //  def Viewer= Action {
  //    Ok(views.html.viewer())
  //  }
  //
  //    def ViewerCampus(any: String) = Action {
  //    Ok(views.html.viewer_campus())
  //  }
  // CLR:PM
  ///**
  //  * AUTHORIZATION SESSION
  //  */
  //case class Login(username: String, password: String) {
  //  var admin_mode = ""
  //
  //  def validate: String = {
  //    if (!User.authenticate(username, password)) return "Invalid user or password"
  //    null
  //  }
  //}
  //
  //// displays the login form for the architect
  //def login = Action { // IMPORTANT - to allow cross domain requests
  //  LPLogger.error("ApplicationAnyplace.login() ?")
  //  //response().setHeader("Access-Control-Allow-Origin", CORSHelper.checkOriginFromRequest(request()));
  //  // allows session cookies to be transferred
  //  //response().setHeader("Access-Control-Allow-Credentials", "true");
  //  def frm = Form(mapping("Username" -> nonEmptyText,
  //    "Password" -> nonEmptyText)(Login.apply)(Login.unapply))
  //
  //  Ok(views.html.anyplace_login.render(frm))
  //}
  //
  //// validates the username and password
  //def authenticate = Action {
  //  LPLogger.error("ApplicationAnyplace.authenticate() ?")
  //  def loginForm = Form(mapping("Username" -> nonEmptyText, "Password" -> nonEmptyText)(Login.apply)(Login.unapply))
  //
  //  if (loginForm.hasErrors)
  //    BadRequest(views.html.anyplace_login.render(loginForm))
  //  else {
  //    if (loginForm.get.admin_mode.equalsIgnoreCase("architect"))
  //      Redirect(routes.AnyplaceWebApps.serveAdmin("index.html")).withSession(("username", loginForm.get.username))
  //    else if (loginForm.get.admin_mode.equalsIgnoreCase("android"))
  //      Redirect(routes.AnyplaceAndroid.getApks).withSession(("username", loginForm.get.username))
  //    else if (loginForm.get.admin_mode.equalsIgnoreCase("admin"))
  //      Redirect(routes.AnyplaceWebApps.serveAdmin("index.html")).withSession(("username", loginForm.get.username))
  //    else if (loginForm.get.admin_mode.equalsIgnoreCase("architect2"))
  //      Redirect(routes.AnyplaceWebApps.serveAdmin("index.html")).withSession(("username", loginForm.get.username))
  //  }
  //
  //  def frm = Form(mapping("Username" -> nonEmptyText,
  //    "Password" -> nonEmptyText)(Login.apply)(Login.unapply))
  //  BadRequest(views.html.anyplace_login.render(frm))
  //}
  //
  ///**
  //  * Logout and clean the session.
  //  */
  //def logout = Action {
  //  LPLogger.error("ApplicationAnyplace.logout() ?")
  //  Redirect(routes.ApplicationAnyplace.login).withNewSession.flashing(
  //    "success" -> "You've been logged out"
  //  )
  //}

}
