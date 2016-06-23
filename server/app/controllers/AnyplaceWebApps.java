/*
 * AnyPlace: A free and open Indoor Navigation Service with superb accuracy!
 *
 * Anyplace is a first-of-a-kind indoor information service offering GPS-less
 * localization, navigation and search inside buildings using ordinary smartphones.
 *
 * Author(s): Lambros Petrou, Kyriakos Georgiou
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

package controllers;

import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;

import java.io.File;

/**
 * This controller is responsible to serve the web applications
 */
public class AnyplaceWebApps extends Controller {

    public static Result AddTrailingSlash() {
        return movedPermanently(request().path() + "/");
    }

    // @Security.Authenticated(Secured.class)
    public static Result serveArchitect2(String file) {
        File archiDir = new File("public/anyplace_architect");
        return serveFile(archiDir, file);
    }

    public static Result servePortal(String file) {
        File viewerDir = new File("web_apps/anyplace_portal");
        return serveFile(viewerDir, file);
    }

    public static Result servePortalTos(String file) {
        File viewerDir = new File("web_apps/anyplace_portal/tos");
        return serveFile(viewerDir, file);
    }

    public static Result servePortalPrivacy(String file) {
        File viewerDir = new File("web_apps/anyplace_portal/privacy");
        return serveFile(viewerDir, file);
    }

    public static Result servePortalMail(String file) {
        File viewerDir = new File("web_apps/anyplace_portal/mail");
        return serveFile(viewerDir, file);
    }

    // the action for the Anyplace Viewer
    public static Result serveViewer(String file) {

        String agentInfo = request().getHeader("user-agent");

        String mode = request().getQueryString("mode");

        if (mode == null || !mode.equalsIgnoreCase("widget")) {

            String bid = request().getQueryString("buid");
            String pid = request().getQueryString("selected");
            String floor = request().getQueryString("floor");
            if (null == bid) {
                bid = "";
            }
            if (null == pid) {
                pid = "";
            }
            if (null == floor) {
                floor = "";
            }

        }

        File viewerDir = new File("public/anyplace_viewer");
        return serveFile(viewerDir, file);
    }

    /**
     * @author KG
     * <p>
     * Viewer 2 for testing
     */
    public static Result serveViewer2(String file) {

        String agentInfo = request().getHeader("user-agent");

        String mode = request().getQueryString("mode");

        if (mode == null || !mode.equalsIgnoreCase("widget")) {

            String bid = request().getQueryString("buid");
            String pid = request().getQueryString("selected");
            String floor = request().getQueryString("floor");
            if (null == bid) {
                bid = "";
            }
            if (null == pid) {
                pid = "";
            }
            if (null == floor) {
                floor = "";
            }

        }

        File viewerDir = new File("public/anyplace_viewer2");
        return serveFile(viewerDir, file);
    }

    // the action for the Anyplace Developers
    public static Result serveDevelopers(String file) {
        File devsDir = new File("web_apps/anyplace_developers");
        return serveFile(devsDir, file);
    }


    public static String parseCookieForUsername(Http.Request request) {
        String cookie = request.cookie("PLAY_SESSION").value();
        String data = cookie.substring(cookie.indexOf('-') + 1);
        String pairs[] = data.split("\u0000");
        //String pairs[] = data.split("&");// play 2.1.3+
        for (String pair : pairs) {
            String dat[] = pair.split("%3A");
            //String dat[] = pair.split("=");// play 2.1.3+
            String key = dat[0];
            String value = dat[1];
            if (key.equals("username")) {
                return value;
            }
        }
        return "";
    }

    /**
     * HELPER METHODS
     */
    public static Result serveFile(File appDir, String file) {
        if (file == null) {
            return notFound();
        }
        if (file.trim().isEmpty() || file.trim().equals("index.html")) {
            file = "index.html";
            response().setHeader("Content-Disposition", "inline");
        }

        File reqFile = new File(appDir, file);
        if (!reqFile.exists() || !reqFile.canRead()) {
            return notFound();
        }

        return ok(reqFile);
    }

}
