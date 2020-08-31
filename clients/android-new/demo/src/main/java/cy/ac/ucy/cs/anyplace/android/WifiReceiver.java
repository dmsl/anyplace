package cy.ac.ucy.cs.anyplace.android;

//TODO Update to androidx

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