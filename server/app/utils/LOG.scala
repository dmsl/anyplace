/*
 * Anyplace: A free and open Indoor Navigation Service with superb accuracy!
 *
 * Anyplace is a first-of-a-kind indoor information service offering GPS-less
 * localization, navigation and search inside buildings using ordinary smartphones.
 *
 * Author(s): Paschalis Mpeis, Constantinos Costa, Kyriakos Georgiou, Lambros Petrou
 *
 * Supervisor: Demetrios Zeinalipour-Yazti
 *
 * URL: https://anyplace.cs.ucy.ac.cy
 * Contact: anyplace@cs.ucy.ac.cy
 *
 * Copyright (c) 2021, Data Management Systems Lab (DMSL), University of Cyprus.
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

import com.typesafe.config.ConfigFactory
import play.api.Logger

object LOG {
  val logger: Logger = Logger(this.getClass)
  val LEVEL: Int = ConfigFactory.load().getInt("application.debug.level")

  def D(message: String): Unit = logger.debug(String.format("%s", message))
  def I(message: String): Unit = logger.info(String.format("%s", message))
  def W(message: String): Unit = logger.warn(String.format("%s", message))
  def E(message: String): Unit = logger.error(String.format("%s", message))

  def D(tag: String, message: String): Unit = D(String.format("%s: %s", tag, message))
  def I(tag: String, message: String): Unit = I(String.format("%s: %s", tag, message))
  def W(tag: String, message: String): Unit = W(String.format("%s: %s", tag, message))
  def E(tag: String, message: String): Unit = E(String.format("%s: %s", tag, message))

  def D(msg: String, e: Exception): Unit = D(msg + ": "+ prettyException(e))
  def I(msg: String, e: Exception): Unit = I(msg + ": "+ prettyException(e))
  def E(msg: String, e: Exception): Unit = E(msg + ": "+ prettyException(e))

  def E(tag:String, msg: String, e: Exception): Unit = E(tag+": "+msg, e)
  def I(tag:String, msg: String, e: Exception): Unit = I(tag+": "+msg, e)
  def D(tag:String, msg: String, e: Exception): Unit = D(tag+": "+msg, e)


  // Helper methods
  private def prettyException(e: Exception): String= {
    String.format("%s: %s\n%s", e.getClass, e.getMessage, e.getStackTrace)
  }

  def D1: Boolean = LEVEL >= 1
  def D2: Boolean = LEVEL >= 2
  def D3: Boolean = LEVEL >= 3
  def D4: Boolean = LEVEL >= 4
  def D5: Boolean = LEVEL >= 5

  def D1(message: String): Unit = if (D1) logger.warn(String.format("%s", message))
  def D2(message: String): Unit = if (D2) logger.debug(String.format("%s", message))
  def D3(message: String): Unit = if (D3) logger.debug(String.format("%s", message))
  def D4(message: String): Unit = if (D4) logger.debug(String.format("%s", message))
  def D5(message: String): Unit = if (D5) logger.debug(String.format("%s", message))
}
