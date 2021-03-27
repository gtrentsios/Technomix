package com.example.technomix.activities;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.example.technomix.R;
import com.example.technomix.utils.Background_send_credentials;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.Proxy;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.net.UnknownHostException;

public class SendWifiCredentials extends AppCompatActivity {
    private WifiManager wifiManager;
    EditText SSID, Pwd;
    String  sSSID,  sPwd;
    String  sDeviceSSID;
    String  connectedSIID;
    String  sURL;
    HttpURLConnection conn;
    String deviceIP;
    DhcpInfo dhcp;
    private Button buttonSend;
    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_wifi_credentials);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        //Get the bundle
        SSID      =   (EditText)findViewById(R.id.SSIDInput);
        Pwd      =   (EditText)findViewById(R.id.PasswordInput);
        Bundle bundle = getIntent().getExtras();
//Extract the data…
        sDeviceSSID = bundle.getString("inpSSID");
        connectedSIID = bundle.getString("connectedSIID");
        //connectToWifi(sSSID);
        buttonSend = findViewById(R.id.SendCredentialToDevice);
        buttonSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    Connect01(sURL);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

            }
        });
        connectToWifi(sDeviceSSID);
        int SDK_INT = android.os.Build.VERSION.SDK_INT;
        if (SDK_INT > 8)
        {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                    .permitAll().build();
            StrictMode.setThreadPolicy(policy);

        }
    }

    /**
     * Connect to the specified wifi network.
     *
     * @param networkSSID     - The wifi network SSID
     */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void connectToWifi(final String networkSSID) {
        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }

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
                                dhcp = wifiManager.getDhcpInfo();
                                String hostIP = InetAddress.getByAddress(intToByteArray(dhcp.gateway)).getHostAddress();
                                String[] ipParts = hostIP.split("\\.");
                                 deviceIP = ipParts[3] + "." + ipParts[2] + "." + ipParts[1] + "." + "71";
                                //sURL = "http://" + deviceIP  + "/WIFI";
                                sURL = "http://" + deviceIP +"/";
                               // sURL = "http://google.gr";
                                //GetText(sURL);
                            } catch (Exception ex) {
                            }
                        }
                    });
        }
    }

    public static byte[] intToByteArray(int a) {
        byte[] ret = new byte[4];
        ret[3] = (byte) (a & 0xFF);
        ret[2] = (byte) ((a >> 8) & 0xFF);
        ret[1] = (byte) ((a >> 16) & 0xFF);
        ret[0] = (byte) ((a >> 24) & 0xFF);
        return ret;
    }
    public static byte[] intToByteArray2(int a) {
        byte[] ret = new byte[4];
        ret[0] = (byte) (a & 0xFF);
        ret[1] = (byte) ((a >> 8) & 0xFF);
        ret[2] = (byte) ((a >> 16) & 0xFF);
        ret[3] = (byte) ((a >> 24) & 0xFF);
        return ret;
    }
    public void Connect01(String sURL) throws UnsupportedEncodingException {


        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
        sSSID = SSID.getText().toString();
        sPwd = Pwd.getText().toString();
        // Create data variable for sent values to server
        String data = URLEncoder.encode("SSID", "UTF-8") + "=" + URLEncoder.encode(sSSID, "UTF-8");
        data += "&" + URLEncoder.encode("password", "UTF-8") + "=" + URLEncoder.encode(sPwd, "UTF-8");
        String text = "";
        BufferedReader reader = null;
        // Send data
        try {
            /*
           StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);

             */
            URL url = new URL(sURL);
            Proxy proxy = new Proxy(Proxy.Type.DIRECT,
                    new InetSocketAddress(
                            InetAddress.getByAddress(intToByteArray2(dhcp.ipAddress)), Integer.parseInt("80")));
            URLConnection conn = url.openConnection(proxy);

            // Defined URL  where to send data

            // Send POST data request
            //conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(25000);
            conn.setConnectTimeout(25000);
            conn.setDoInput(true);
            conn.setDoOutput(true);
           // conn.setChunkedStreamingMode(0);
           // conn.setRequestMethod("POST");
            conn.setRequestProperty("User-Agent", "TechnomixAndroidAgent");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setUseCaches (false);
            //OutputStream outStream = conn.getOutputStream();
            OutputStream outStream = new BufferedOutputStream(conn.getOutputStream());
           // OutputStream outStream = new URL("http://stackoverflow.com").openStream();
           //OutputStream out = new BufferedOutputStream(outStream);

            outStream.write(data.getBytes("UTF-8"));
            outStream.flush();
            outStream.close();
            // Get the server response
            InputStream in = new BufferedInputStream(conn.getInputStream());
            // Read Server Response
            int line;
            StringWriter sb= new StringWriter();
            while ((line = in.read()) != -1) {
                // Append server response in string
                sb.append(line + "\n");
            }
            text = sb.toString();
        } catch (Exception ex) {
            Log.e("URL Connection", "STACKTRACE");
            Log.e("URL Connection", Log.getStackTraceString(ex));
        } finally {
            try {
                conn.disconnect();
            } catch (Exception ex) {
                int i = 0;
            }
        }
    }
    private void connect2(String sURL){
        Socket echoSocket = null;
        DataOutputStream os = null;
        DataInputStream is = null;
        DataInputStream stdIn = new DataInputStream(System.in);
        try {
            echoSocket = new Socket(sURL, 80);
            os = new DataOutputStream(echoSocket.getOutputStream());
            is = new DataInputStream(echoSocket.getInputStream());

        } catch (UnknownHostException e) {
            System.err.println("Don't know about host: Jatin");
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to: Jatin");
        }
    }

}
