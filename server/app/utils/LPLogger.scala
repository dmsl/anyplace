/*
 * AnyPlace: A free and open Indoor Navigation Service with superb accuracy!
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
package utils

import play.Logger

// TODO rename to LOG
object LPLogger {
private val LEVEL:Int = 2

    def error(tag: String, message: String, e: Exception) {
        Logger.error(String.format("%s: %s [%s]", tag, message, e.getMessage), e)
    }

    def error(message: String) {
        Logger.error(String.format("%s", message))
    }

    def info(tag: String, message: String, e: Exception) {
        Logger.info(String.format("%s: %s [%s]", tag, message, e.getMessage), e)
    }

    def info(message: String) {
        Logger.info(String.format("%s", message))
    }

    def debug(tag: String, message: String, e: Exception) {
        Logger.debug(String.format("%s: %s [%s]", tag, message, e.getMessage), e)
    }

    def debug(message: String) {
        Logger.debug(String.format("%s", message))
    }

    def D1():Boolean = LEVEL >= 1
    def D2():Boolean = LEVEL >= 2
    def D3():Boolean = LEVEL >= 3
    def D4():Boolean = LEVEL >= 4
    def D5():Boolean = LEVEL >= 5

    def D1(message: String): Unit = if (D1()) Logger.debug(String.format("%s", message))
    def D2(message: String): Unit = if (D2()) Logger.debug(String.format("%s", message))
    def D3(message: String): Unit = if (D3()) Logger.debug(String.format("%s", message))
    def D4(message: String): Unit = if (D4()) Logger.debug(String.format("%s", message))
    def D5(message: String): Unit = if (D5()) Logger.debug(String.format("%s", message))


}
