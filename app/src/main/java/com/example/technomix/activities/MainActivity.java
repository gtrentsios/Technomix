package com.example.technomix.activities;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.technomix.R;
import com.example.technomix.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private WifiManager wifiManager;
    private int size = 0;
    private ArrayList<String> arrayList = new ArrayList<>();
    private ArrayAdapter adapter;
    private static final int ACCESS_WIFI_STATE = 9000;
    private static final int CHANGE_WIFI_STATE = 9001;
    private static final int CHANGE_NETWORK_STATE = 9002;
    private static final int INTERNET = 9003;
    private static final int ACCESS_LOCATION = 9004;
    private int networkId;

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button buttonScan = findViewById(R.id.scanBtn);
        buttonScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                scanWifi();
            }
        });

        ListView listView = findViewById(R.id.wifiList);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.Q)
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String ssid = (String) parent.getItemAtPosition(position);
                Bundle bundle = new Bundle();
                bundle.putString("inpSSID", ssid);
                Intent i = new Intent(MainActivity.this, SendWifiCredentials.class);
                i.putExtras(bundle);
                startActivity(i);
                //  connectToWifi(ssid, "", 0);
            }
        });
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, arrayList);
        listView.setAdapter(adapter);
        checkPermissions();
    }
/* Permissions methods */
    void checkPermissions() {
        if (Utils.isWiFiStatePermissionsGranted(this)) {
            if (Utils.isWiFiChangeStatePermissionsGranted(this)) {
                if (Utils.isSystemWriteSettingsPermissionsGranted(this)) {
                    if (Utils.isInternetPermissionsGranted(this)) {
                        if (Utils.isLocationPermissionsGranted(this)) {
                            wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                            if (!wifiManager.isWifiEnabled()) {
                                Toast.makeText(this, "WiFi is disabled ... We need to enable it", Toast.LENGTH_LONG).show();
                                wifiManager.setWifiEnabled(true);
                            }
                            scanWifi();
                        } else {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                ActivityCompat.requestPermissions(
                                        this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        ACCESS_LOCATION);
                            } else {
                                ActivityCompat.requestPermissions(
                                        this,
                                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                                        ACCESS_LOCATION);
                            }
                        }
                    } else {
                        ActivityCompat.requestPermissions(
                                this,
                                new String[]{Manifest.permission.INTERNET},
                                INTERNET);
                    }

                } else {
                    ActivityCompat.requestPermissions(
                            this,
                            new String[]{Manifest.permission.CHANGE_NETWORK_STATE},
                            CHANGE_NETWORK_STATE);
                }

            } else {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.CHANGE_WIFI_STATE},
                        CHANGE_WIFI_STATE);
            }
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_WIFI_STATE},
                    ACCESS_WIFI_STATE);
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        switch (requestCode) {
            case ACCESS_WIFI_STATE:
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.CHANGE_WIFI_STATE}, CHANGE_WIFI_STATE);
                break;
            case CHANGE_WIFI_STATE:
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.WRITE_SETTINGS}, CHANGE_NETWORK_STATE);
                break;
            case CHANGE_NETWORK_STATE:
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.INTERNET},
                        INTERNET);
                break;
            case INTERNET:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ActivityCompat.requestPermissions(
                            this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, ACCESS_LOCATION);
                } else {
                    ActivityCompat.requestPermissions(
                            this,
                            new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, ACCESS_LOCATION);
                }
                break;
            case ACCESS_LOCATION:
                wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                if (!wifiManager.isWifiEnabled()) {
                    Toast.makeText(this, "WiFi is disabled ... We need to enable it", Toast.LENGTH_LONG).show();
                    wifiManager.setWifiEnabled(true);
                }
                scanWifi();
                break;
        }
    }
/* WIFI methods */
    @RequiresApi(api = Build.VERSION_CODES.N)
    private void scanWifi() {
        arrayList.clear();
        WifiInfo connectedInfo = wifiManager.getConnectionInfo();
        networkId = connectedInfo.getNetworkId();
        registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        wifiManager.startScan();
        //Toast.makeText(this, "Scanning WiFi ...", Toast.LENGTH_SHORT).show();
        // Register Callback - Call this in your app start!

    }

    BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            List<ScanResult> results = wifiManager.getScanResults();
            unregisterReceiver(this);

            for (ScanResult scanResult : results) {
                WifiInfo info = wifiManager.getConnectionInfo();
                String ssid = info.getSSID();
                if (scanResult.SSID.contains("Technomix")) {
                    arrayList.add(scanResult.SSID);
                    adapter.notifyDataSetChanged();
                }
            }

        }

        ;
    };


}