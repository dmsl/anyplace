package cy.ac.ucy.cs.anyplace.android;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import cy.ac.ucy.cs.anyplace.lib.Anyplace;

import cy.ac.ucy.cs.anyplace.lib.android.SettingsAnyplace;

public class MainActivity extends AppCompatActivity {

  private static final String TAG = MainActivity.class.getSimpleName();
  private final static String build = "username_1373876832005";
  private final static String floor = "-1";
  private final static String algorithm = "1"; //algorithm KNN_WKNN
  //private final static String host = "ap-dev.cs.ucy.ac.cy";
  //private final static String port = "443";

  private WifiManager wifiManager;
  private WifiReceiver receiverWifi;
  private final int MY_PERMISSIONS_ACCESS_COARSE_LOCATION = 1;

  private TextView textbox = null;
  private TextView RSSI;
  private String t = "";
  private String temp;
  private String prints;
  private ProgressDialog progressDialog;

  private String cache;
  private String host;
  private String port;
  private String apikey;
  //TODO: Read from preferences


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    //

    SettingsAnyplace prefs = new SettingsAnyplace(getApplicationContext(), "");
//getString(R.xml.root_preferences)


    port = "443";
    host=prefs.getHost();  // Read preferences
    Log.d(TAG, "host: " + host);

    apikey="";
    cache = String.valueOf(getApplicationContext().getFilesDir());



    //Collect fingerprints
    wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    if (!wifiManager.isWifiEnabled()) {
      Toast.makeText(getApplicationContext(), "Turning WiFi ON...", Toast.LENGTH_LONG).show();
      wifiManager.setWifiEnabled(true);
    }

    //Button
    final Button mainB = (Button) findViewById(R.id.button);

    //Results - Text view
    textbox = (TextView) findViewById(R.id.textbox);
    //textbox.setMovementMethod(new ScrollingMovementMethod());

    RSSI = (TextView) findViewById(R.id.RSSI);

    //Button - Online Localization - On click
    mainB.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        try {
          AsyncTaskOnlineLocal asyncTask = new AsyncTaskOnlineLocal();
          asyncTask.execute();
        } catch (Exception ex) {
          ex.printStackTrace();
        }
      }
    });

    //Launch Anyplace Viewer
    Button anyplaceViewer = (Button) findViewById(R.id.anyplaceViewerBtn);
    anyplaceViewer.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        String anyplaceViewerLink = "https://www.ap.cs.ucy.ac.cy/viewer";
        Uri webaddress = Uri.parse(anyplaceViewerLink);


        Intent gotoAnyplace = new Intent(Intent.ACTION_VIEW, webaddress);
        if (gotoAnyplace.resolveActivity(getPackageManager()) != null){
          startActivity(gotoAnyplace);
        }
      }
    });

  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.settingsbar, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()){

      case R.id.item2:
          Toast.makeText(this, "Preferences Selected", Toast.LENGTH_SHORT).show();
          Intent startIntent = new Intent(getApplicationContext(), SettingsActivity.class);
          startActivity(startIntent);
          return true;
      case R.id.item3:
        Toast.makeText(this, "About Selected", Toast.LENGTH_SHORT).show();
        return true;
      default:
        return super.onOptionsItemSelected(item);

    }

  }

  //Wifi scanning

  protected void onPostResume() {
    super.onPostResume();
    receiverWifi = new WifiReceiver(wifiManager);
    IntentFilter intentFilter = new IntentFilter();
    intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
    registerReceiver(receiverWifi, intentFilter);
    getWifi();
  }

  private void getWifi() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        Toast.makeText(MainActivity.this, "location turned off", Toast.LENGTH_SHORT).show();
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, MY_PERMISSIONS_ACCESS_COARSE_LOCATION);
      } else {
        wifiManager.startScan();
      }
    } else {
      wifiManager.startScan();
    }
  }

  @Override
  protected void onPause() {
    super.onPause();
    unregisterReceiver(receiverWifi);
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    switch (requestCode) {
      case MY_PERMISSIONS_ACCESS_COARSE_LOCATION:
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          wifiManager.startScan();
        } else {
          Toast.makeText(MainActivity.this, "permission not granted", Toast.LENGTH_SHORT).show();
          return;
        }
        break;
    }
  }

  /**
   * API Call - Async Task
   */
  public class AsyncTaskOnlineLocal extends AsyncTask<String, String, String> {
    @Override
    protected void onPreExecute() {
      textbox.setText(" ");
      Log.d(TAG, "onPreExecute.." );
      super.onPreExecute();
      progressDialog = new ProgressDialog(MainActivity.this);
      progressDialog.setMessage("Please wait...");
      progressDialog.setCancelable(false);
      progressDialog.show();
    }

    @Override
    protected String doInBackground(String... params) {
      //Collect fingerprints
      boolean debugging = false;
      String fingerprints[];

      if (debugging){
        fingerprints = new String[]{"{\"bssid\":\"d4:d7:48:d8:28:b0\",\"rss\":-40}", "{\"bssid\":\"00:0e:38:7a:37:77\",\"rss\":-50}"};
      }
      else  {
        while (receiverWifi.isReady() == false) {
          //wait
          Log.d(TAG, "In while for wifi");
        }
         fingerprints = receiverWifi.getFingerprints();
      }

      //Hardcode fingerprints (only for testing)




      prints = "";
      for (int i = 0; i < fingerprints.length; i++) {
        Log.d(TAG, "Fingerprint[" + i + "] = " + fingerprints[i]);
        prints += fingerprints[i] + " ";
      }



      //String cache = String.valueOf(getApplicationContext().getFilesDir());
      Log.d(TAG, "cache path = " + cache + "/");
      Anyplace server = new Anyplace(host, port, cache + "/");

      String estimateResults = server.estimatePosition(build, floor, fingerprints, algorithm);
      //String estimateResults = server.buildingAll();

      if (estimateResults == null){
        return null;
      }

      temp = estimateResults;
      String[] response = estimateResults.split("[,:]");
      double x = 0;
      double y = 0;

      for (int i = 0; i < response.length; i++) {
        if (response[i].equals("\"lat\"")) {
          x = Double.parseDouble(response[i + 1].replace('"', ' '));
          Log.d(TAG, "lan = " + x);
          t = t + "lan = " + x;
        }
        if (response[i].equals("\"long\"")) {
          y = Double.parseDouble(response[i + 1].replace('"', ' '));
          Log.d(TAG, "lon = " + y);
          t = t + ", lon = " + y;
        }
      }

      return null;
    }

    @Override
    protected void onPostExecute(String response) {
      progressDialog.hide();
      textbox.setText(t);
      RSSI.setText(prints);

    }
  }



}
