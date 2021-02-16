/*
 * AnyPlace: A free and open Indoor Navigation Service with superb accuracy!
 *
 * Anyplace is a first-of-a-kind indoor information service offering GPS-less
 * localization, navigation and search inside buildings using ordinary smartphones.
 *
 * Author(s): Constandinos Demetriou, Christakis Achilleos, Marcos Antonios Charalambous
 *
 * Co-supervisor: Paschalis Mpeis
 *
 * Supervisor: Demetrios Zeinalipour-Yazti
 *
 * URL: http://anyplace.cs.ucy.ac.cy
 * Contact: anyplace@cs.ucy.ac.cy
 *
 * Copyright (c) 2019, Data Management Systems Lab (DMSL), University of Cyprus.
 * All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in the
 * Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the
 * following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 *
 */

package cy.ac.ucy.cs.anyplace;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;


import java.util.List;

class WifiReceiver extends BroadcastReceiver {
    private WifiManager wifiManager;

    private String fingerprints[]; //a list with all fingerprints
    private boolean ready = false;

    private static final String TAG = "DEBUG";

    public WifiReceiver(WifiManager wifiManager) {
        this.wifiManager = wifiManager;
    }

    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action)) {
            List<ScanResult> wifiList = wifiManager.getScanResults();
            fingerprints = new String[wifiList.size()];
            int i = 0;
            //for each fingerprint
            for (ScanResult scanResult : wifiList) {
                StringBuilder result = new StringBuilder();
                result.append("{\"bssid\":\"").append(scanResult.BSSID).append("\",\"rss\":").append(scanResult.level).append("}");
                fingerprints[i] = result.toString();
                i++;
            }
            this.ready = true;
        }
    }

    /**
     * @return a string array with all fingerprints
     */
    public String[] getFingerprints() {
        return fingerprints;
    }

    /**
     * @return true if fingerprints has collected else return false
     */
    public boolean isReady() {
        return ready;
    }
}