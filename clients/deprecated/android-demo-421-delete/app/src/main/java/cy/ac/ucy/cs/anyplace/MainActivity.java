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

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.Button;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.content.Context;
import android.widget.Toast;
import android.Manifest;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import cy.ac.ucy.anyplace.Anyplace;

public class MainActivity extends AppCompatActivity {

    private final static String build = "username_1373876832005"; //building id
    private final static String floor = "-1"; //floor number
    private final static String algorithm = "1"; //algorithm KNN_WKNN
    private final static String host = "ap-dev.cs.ucy.ac.cy";
    private final static String port = "443";
    private static String access_token;
    private static String cache;

    private static final String TAG = "DEBUG"; //debug tag

    private ListView wifiList;
    private WifiManager wifiManager;
    private WifiReceiver receiverWifi;
    private final int MY_PERMISSIONS_ACCESS_COARSE_LOCATION = 1;

    private TextView tvOutput = null;
    private ProgressDialog progressDialog;

    /**
     * @param savedInstanceState
     */
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        access_token = getResources().getString(R.string.access_token);
        cache = String.valueOf(getApplicationContext().getFilesDir());
        Log.d(TAG, "cache path = " + cache + "/");

        //Collect fingerprints
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled()) {
            Toast.makeText(getApplicationContext(), "Turning WiFi ON...", Toast.LENGTH_LONG).show();
            wifiManager.setWifiEnabled(true);
        }

        //Button - API Call
        final Button button = findViewById(R.id.button);
        //Button - Online Localization
        final Button buttonOnlineLocalization = findViewById(R.id.buttonOnlineLocalization);
        //Button - Offline Localization
        final Button buttonOfflineLocalization = findViewById(R.id.buttonOfflineLocalization);
        //Results - Text view
        tvOutput = findViewById(R.id.tvOutput);
        tvOutput.setMovementMethod(new ScrollingMovementMethod());

        //Button - API Call - On click
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    AsyncTaskAPICall asyncTask = new AsyncTaskAPICall();
                    asyncTask.execute();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        //Button - Online Localization - On click
        buttonOnlineLocalization.setOnClickListener(new View.OnClickListener() {
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

        //Button - Offline Localization - On click
        buttonOfflineLocalization.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    AsyncTaskOfflineLocal asyncTask = new AsyncTaskOfflineLocal();
                    asyncTask.execute();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

            }
        });
    }

    //Wifi scanning

    protected void onPostResume() {
        super.onPostResume();
        receiverWifi = new WifiReceiver(wifiManager, wifiList);
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
    public class AsyncTaskAPICall extends AsyncTask<String, String, String> {
        @Override
        protected void onPreExecute() {
            tvOutput.setText(" ");
            super.onPreExecute();
            progressDialog = new ProgressDialog(MainActivity.this);
            progressDialog.setMessage("Please wait...");
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @Override
        protected String doInBackground(String... params) {
            //(MyApplication)getApplication()).anyplace =  new AnyplacePost("ap.cs.ucy.ac.cy", "443");
            Anyplace server = new Anyplace(host, port, cache + "/");
            //client.setCacheDir(getApplicationContext().getFilesDir()); // save where to store stuff..
            String response = server.allBuildingFloors(build);
            Log.d(TAG, response);
            return response;
        }

        @Override
        protected void onPostExecute(String response) {
            progressDialog.hide();
            tvOutput.setText(response);
        }
    }//end class ApiCallAsyncTasks

    /**
     * Online Localization - Async Task
     */
    public class AsyncTaskOnlineLocal extends AsyncTask<String, String, String> {
        @Override
        protected void onPreExecute() {
            tvOutput.setText(" ");
            super.onPreExecute();
            progressDialog = new ProgressDialog(MainActivity.this);
            progressDialog.setMessage("Please wait...");
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @Override
        protected String doInBackground(String... params) {
            //Collect fingerprints
            while (receiverWifi.isReady() == false) {
                //wait
            }

            //Hardcode fingerprints (only for testing)
            //String fingerprints[] = {"{\"bssid\":\"d4:d7:48:d8:28:b0\",\"rss\":-40}", "{\"bssid\":\"00:0e:38:7a:37:77\",\"rss\":-50}"};
            String fingerprints[] = receiverWifi.getFingerprints();
            for (int i = 0; i < fingerprints.length; i++) {
                Log.d(TAG, "Fingerprint[" + i + "] = " + fingerprints[i]);
            }

            String cache = String.valueOf(getApplicationContext().getFilesDir());
            Log.d(TAG, "cache path = " + cache + "/");
            Anyplace server = new Anyplace(host, port, cache + "/");

            String estimateResults = server.estimatePosition(build, floor, fingerprints, algorithm);
            if (estimateResults == null){
                return null;
            }

            String[] response = estimateResults.split("[,:]");
            double x = 0;
            double y = 0;
            for (int i = 0; i < response.length; i++) {
                if (response[i].equals("\"lat\"")) {
                    x = Double.parseDouble(response[i + 1].replace('"', ' '));
                    Log.d(TAG, "lan = " + x);
                }
                if (response[i].equals("\"long\"")) {
                    y = Double.parseDouble(response[i + 1].replace('"', ' '));
                    Log.d(TAG, "lon = " + y);
                }
            }
            MapsActivity.lat = x;
            MapsActivity.lon = y;

            //Open google maps
            Intent myIntent = new Intent(MainActivity.this, MapsActivity.class);
            MainActivity.this.startActivity(myIntent);

            return null;
        }

        @Override
        protected void onPostExecute(String response) {
            progressDialog.hide();
            Toast.makeText(getApplicationContext(), "(" + (float) MapsActivity.lat + ", " + (float) MapsActivity.lon + ")", Toast.LENGTH_LONG).show();
        }
    }//end class AsyncTaskOnlineLoc

    /**
     * Offline Localization - Async Task
     */
    public class AsyncTaskOfflineLocal extends AsyncTask<String, String, String> {
        @Override
        protected void onPreExecute() {
            tvOutput.setText(" ");
            super.onPreExecute();
            progressDialog = new ProgressDialog(MainActivity.this);
            progressDialog.setMessage("Please wait...");
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @Override
        protected String doInBackground(String... params) {
            //Wifi fingerprints
            while (receiverWifi.isReady() == false) {
                //wait
            }

            //Hardcode fingerprints (only for testing)
            //String fingerprints[] = {"{\"bssid\":\"d4:d7:48:d8:28:b0\",\"rss\":-40}", "{\"bssid\":\"00:0e:38:7a:37:77\",\"rss\":-50}"};
            String fingerprints[] = receiverWifi.getFingerprints();
            for (int i = 0; i < fingerprints.length; i++) {
                Log.d(TAG, "Fingerprint[" + i + "] = " + fingerprints[i]);
            }

            String cache = String.valueOf(getApplicationContext().getFilesDir());
            Log.d(TAG, "cache path = " + cache + "/");
            Anyplace client = new Anyplace(host, port, cache + "/");

            String radioResults = client.radioByBuildingFloor(access_token, build, floor);
            if (radioResults == null){
                return null;
            }

            String estimateResults = client.estimatePositionOffline(build, floor, fingerprints, algorithm);
            if (estimateResults == null){
                return null;
            }

            String response[] = estimateResults.split(" ");
            double x = Double.parseDouble(response[0]);
            double y = Double.parseDouble(response[1]);
            Log.d(TAG, "lan = " + x);
            Log.d(TAG, "lon = " + y);

            MapsActivity.lat = x;
            MapsActivity.lon = y;

            //Open google maps
            Intent myIntent = new Intent(MainActivity.this, MapsActivity.class);
            MainActivity.this.startActivity(myIntent);

            return null;
        }

        @Override
        protected void onPostExecute(String response) {
            progressDialog.hide();
            Toast.makeText(getApplicationContext(), "(" + (float) MapsActivity.lat + ", " + (float) MapsActivity.lon + ")", Toast.LENGTH_LONG).show();
        }
    }//end class AsyncTaskOfflineLocal

}

