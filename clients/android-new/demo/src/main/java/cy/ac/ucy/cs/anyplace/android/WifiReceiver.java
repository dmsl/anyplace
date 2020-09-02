package cy.ac.ucy.cs.anyplace.android;

//TODO Update to androidx. WifiManager doScan() is deprecated since API 28.

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.util.Log;


import java.util.List;

class WifiReceiver extends BroadcastReceiver {
  private WifiManager wifiManager;

  private String fingerprints[]; //a list with all fingerprints
  private boolean ready = false;

  private static final String TAG = WifiReceiver.class.getSimpleName();

  public WifiReceiver(WifiManager wifiManager) {
    this.wifiManager = wifiManager;
    Log.e(TAG, "constructor");
  }

  public void onReceive(Context context, Intent intent) {
    String action = intent.getAction();
    Log.e(TAG, "Before if");
    if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action)) {
      List<ScanResult> wifiList = wifiManager.getScanResults();
      fingerprints = new String[wifiList.size()];
      int i = 0;
      //for each fingerprint
      Log.e(TAG, "Before for with : " + wifiList.size());
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