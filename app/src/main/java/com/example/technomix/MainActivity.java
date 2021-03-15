package com.example.technomix;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.NetworkSpecifier;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Build;
import android.os.Bundle;
import android.content.pm.PackageManager;
import android.text.format.Formatter;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.technomix.utils.Utils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private WifiManager wifiManager;
    private ListView listView;
    private Button buttonScan;
    private int size = 0;
    private List<ScanResult> results;
    private ArrayList<String> arrayList = new ArrayList<>();
    private ArrayAdapter adapter;
    private static final int ACCESS_WIFI_STATE = 9000;
    private static final int CHANGE_WIFI_STATE = 9001;
    private static final int CHANGE_NETWORK_STATE = 9002;
    private static final int INTERNET = 9003;
    private int networkId;
    private static final int ACCESS_LOCATION = 9004;

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        buttonScan = findViewById(R.id.scanBtn);
        buttonScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                scanWifi();
            }
        });

        listView = findViewById(R.id.wifiList);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.Q)
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String ssid = (String) parent.getItemAtPosition(position);
                connectToWifi(ssid, "", 0);
            }
        });
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, arrayList);
        listView.setAdapter(adapter);
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

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void scanWifi() {
        arrayList.clear();
        WifiInfo connectedInfo = wifiManager.getConnectionInfo();
        networkId = connectedInfo.getNetworkId();
        registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        wifiManager.startScan();
        Toast.makeText(this, "Scanning WiFi ...", Toast.LENGTH_SHORT).show();
        // Register Callback - Call this in your app start!

    }

    BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            results = wifiManager.getScanResults();
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

    /**
     * Connect to the specified wifi network.
     *
     * @param networkSSID     - The wifi network SSID
     * @param networkPassword - the wifi password
     */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void connectToWifi(final String networkSSID, final String networkPassword,
                               int networkId) {
        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }

/*        WifiConfiguration conf = new WifiConfiguration();
        conf.SSID = String.format("\"%s\"", networkSSID);
        conf.preSharedKey = String.format("\"%s\"", networkPassword);

        int netId = wifiManager.addNetwork(conf);
        wifiManager.disconnect();
        wifiManager.enableNetwork(netId, true);
        wifiManager.reconnect();*/
        WifiNetworkSpecifier.Builder builder = new WifiNetworkSpecifier.Builder();
        builder.setSsid(networkSSID);
        WifiNetworkSpecifier wifiNetworkSpecifier = builder.build();
        NetworkRequest.Builder networkRequestBuilder = new NetworkRequest.Builder();
        networkRequestBuilder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
        networkRequestBuilder.addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED);
        networkRequestBuilder.addCapability(NetworkCapabilities.NET_CAPABILITY_TRUSTED);
        networkRequestBuilder.setNetworkSpecifier(wifiNetworkSpecifier);
        NetworkRequest networkRequest = networkRequestBuilder.build();
        ConnectivityManager cm = (ConnectivityManager) this.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            cm.requestNetwork(networkRequest,
                    new ConnectivityManager.NetworkCallback() {
                        @Override
                        public void onAvailable(@NonNull Network network) {
                            try {
                                DhcpInfo dhcp = wifiManager.getDhcpInfo();
                                String address = Formatter.formatIpAddress(dhcp.gateway);
                                int i = 0;
                            } catch (Exception ex) {
                            }
                        }
                    });
        }
    }
}