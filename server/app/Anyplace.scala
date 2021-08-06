/**
 * Anyplace: A free and open Indoor Navigation Service with superb accuracy!
 *
 * Anyplace is a first-of-a-kind indoor information service offering GPS-less
 * localization, navigation and search inside buildings using ordinary smartphones.
 *
 * Author(s): Constantinos Costa, Kyriakos Georgiou, Lambros Petrou, Paschalis Mpeis
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

import com.dmurph.tracking.JGoogleAnalyticsTracker.GoogleAnalyticsVersion
import com.dmurph.tracking.{AnalyticsConfigData, JGoogleAnalyticsTracker}
import datasources.MongodbDatasource

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.inject.ApplicationLifecycle
import utils.Utils.prettyDate
import utils.LOG

import scala.concurrent.Future

/**
 * Entrypoint method to the Anyplace Server
 *
 * @param conf
 * @param appLifecycle to add a stop hook
 */
@Singleton
class Anyplace @Inject() (conf: Configuration) (appLifecycle: ApplicationLifecycle) {
  OnAnyplaceStart()
  appLifecycle.addStopHook({ () => Future.successful { OnAnyplaceStop() } })

  /** Play Application started */
  def OnAnyplaceStart(): Unit = {
    LOG.I(prettyDate + " | Anyplace backend: started.")
    MongodbDatasource.initialize(conf)
    logAnalyticsInstallation()
  }

  /** Play Application stopped */
  def OnAnyplaceStop(): Unit = {
    LOG.I(prettyDate + " | Anyplace backend: stopped.")
    try {
      MongodbDatasource.instance.disconnect()
    } catch {
      case e: Exception => LOG.E("OnAnyplaceStop", e)
    }
  }

  /**
   * Log the entry point from server installation
   */
  def logAnalyticsInstallation(): Unit = {
    JGoogleAnalyticsTracker.setProxy(System.getenv("http_proxy"))
    val config = new AnalyticsConfigData("UA-61313158-2")
    val tracker = new JGoogleAnalyticsTracker(config, GoogleAnalyticsVersion.V_4_7_2)
    tracker.trackEvent("Anyplace Installation", "Anyplace Server start", "Anyplace logging")
  }
}


/* // CHECK: from Global.scala
override def onHandlerNotFound(request: RequestHeader) = {
  Future.successful(NotFound(Json.obj("error" -> "Not Found")))
}
 */