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
  private String fingerprints2[]; //a list with all fingerprints
  private boolean ready = false;

  private static final String TAG = WifiReceiver.class.getSimpleName();

  public WifiReceiver(WifiManager wifiManager) {
    this.wifiManager = wifiManager;
    //Log.e(TAG, "constructor");
  }

  public void onReceive(Context context, Intent intent) {

    //----------------
    String action = intent.getAction();
    //Log.e(TAG, "Before if + " + action +"  +  "+WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
    if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action)) {
      //wifiManager.startScan();
      List<ScanResult> wifiList = wifiManager.getScanResults();
      fingerprints = new String[wifiList.size()];
      int i = 0;
      //for each fingerprint
      //Log.e(TAG, "Before for with : " + wifiList.size());
      for (ScanResult scanResult : wifiList) {
        StringBuilder result = new StringBuilder();
        result.append("{\"bssid\":\"").append(scanResult.BSSID).append("\",\"rss\":").append(scanResult.level).append("}");
        fingerprints[i] = result.toString();
        i++;
      }
      this.ready = true;
    }
    //-----------

    // boolean success = intent.getBooleanExtra(
    //         WifiManager.EXTRA_RESULTS_UPDATED, false);
    // if (success) {
    //   scanSuccess();
    // } else {
    //   // scan failure handling
    //   scanFailure();
    // }


  }
  private void scanSuccess() {
    List<ScanResult> wifiList = wifiManager.getScanResults();
    fingerprints2 = new String[wifiList.size()];
    int i = 0;
    //for each fingerprint
    Log.e(TAG, "scanSuccess : " + wifiList.size());
    for (ScanResult scanResult : wifiList) {
      StringBuilder result = new StringBuilder();
      result.append("{\"bssid\":\"").append(scanResult.BSSID).append("\",\"rss\":").append(scanResult.level).append("}");
      fingerprints2[i] = result.toString();
      i++;
    }
  }
  private void scanFailure() {
    // handle failure: new scan did NOT succeed
    // consider using old scan results: these are the OLD results!
    List<ScanResult> wifiList = wifiManager.getScanResults();
    fingerprints2 = new String[wifiList.size()];
    int i = 0;
    //for each fingerprint
    Log.e(TAG, "scanFailure : " + wifiList.size());
    for (ScanResult scanResult : wifiList) {
      StringBuilder result = new StringBuilder();
      result.append("{\"bssid\":\"").append(scanResult.BSSID).append("\",\"rss\":").append(scanResult.level).append("}");
      fingerprints2[i] = result.toString();
      i++;
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