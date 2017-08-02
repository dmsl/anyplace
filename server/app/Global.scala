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

import java.io.{BufferedReader, InputStreamReader}

import datasources.CouchbaseDatasource
import datasources.DatasourceException
import play.{Application, GlobalSettings, Logger}

import com.dmurph.tracking.{AnalyticsConfigData, JGoogleAnalyticsTracker}
import com.dmurph.tracking.JGoogleAnalyticsTracker.GoogleAnalyticsVersion

class Global extends GlobalSettings {

  override def onStart(app: Application) {
    Logger.info("Global::onStart():: AnyPlace Application started")
    CouchbaseDatasource.getStaticInstance
    log()
  }

  override def onStop(app: Application) {
    Logger.info("Global::onStop():: AnyPlace Application stopped")
    try {
      CouchbaseDatasource.getStaticInstance.disconnect()
    } catch {
      case e: DatasourceException => Logger.error("Global::onStop():: Exception while disconnecting from the couchbase server: " +
        e.getMessage)
    }
  }

  def log(): Unit = {
    /**
      * Log the entry point from server installation
      */
    JGoogleAnalyticsTracker.setProxy(System.getenv("http_proxy"))
    val config = new AnalyticsConfigData("UA-61313158-2")
    val tracker = new JGoogleAnalyticsTracker(config, GoogleAnalyticsVersion.V_4_7_2)
    tracker.trackEvent("Anyplace Installation", "Anyplace Server start", "Anyplace logging")
    /**
      * End
      */

  }

}
