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

import play.Logger;

/**
 * Custom logger class to make logging easier.
 * I will try to use this everywhere so I can concentrate changing
 * this class to allow logging in files
 *
 */
public class LPLogger {

    public static void error(String tag, String message, Exception e){
        Logger.error(String.format("LPLogger::Error[%s]: %s [%s]", tag, message, e.getMessage()), e);
    }

    public static void error(String message){
        Logger.error(String.format("LPLogger::Error:: %s", message));
    }

    public static void info(String tag, String message, Exception e){
        Logger.info(String.format("LPLogger::Info[%s]: %s [%s]", tag, message, e.getMessage()), e);
    }

    public static void info(String message){
        Logger.info(String.format("LPLogger::Info:: %s", message));
    }

    public static void debug(String tag, String message, Exception e){
        Logger.debug(String.format("LPLogger::Debug[%s]: %s [%s]", tag, message, e.getMessage()), e);
    }

    public static void debug(String message){
        Logger.debug(String.format("LPLogger::Debug:: %s", message));
    }

}
