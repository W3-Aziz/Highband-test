package com.w3engineers.highbandtest.protocol.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Enumeration;

/**
 * Wifi Utility class
 */
public class WiFiUtil {

    public static final String HOTSPOT_IP_ADDRESS = "192.168.43.1";

    public static Network getConnectedWiFiNetwork(Context context) {

        String connectedSSID = getConnectedSSID(context);

        if (TextUtils.isEmpty(connectedSSID))
            return null;


        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo;
        Network[] networks = connectivityManager.getAllNetworks();
        for (Network network : networks) {
            networkInfo = connectivityManager.getNetworkInfo(network);

            NetworkInfo.DetailedState detailedState = networkInfo.getDetailedState();
            String typeName = networkInfo.getTypeName();


            if (detailedState.equals(NetworkInfo.DetailedState.CONNECTED)
                    && typeName != null && typeName.toLowerCase().contains("wifi")) {
                return network;
            }
        }
        return null;
    }

    public static boolean isWifiConnected(Context context) {
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return mWifi.isConnected();
    }

    public static String getConnectedSSID(Context context) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo connectionInfo = wifiManager.getConnectionInfo();

        if (connectionInfo == null || TextUtils.isEmpty(connectionInfo.getSSID()))
            return null;

        return connectionInfo.getSSID();
    }

    public static boolean isConnectedWithAdhoc(Context context) {
        String connectedSSID = getConnectedSSID(context);
        return connectedSSID != null && !P2PUtil.isPotentialGO(connectedSSID);
    }

    public static boolean disconnect(Context context) {
        WifiManager mWifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (mWifiManager != null) {
            return mWifiManager.disconnect();
        }
        return false;
    }

    public static boolean isSameSSID(String ssid1, String ssid2) {

        if (ssid1 != null && ssid2 != null) {
            ssid1 = ssid1.replaceAll("\"", "");
            ssid2 = ssid2.replaceAll("\"", "");

            return ssid1.equals(ssid2);
        }

        return false;
    }

    public static InetAddress determineAddress(Context context) {
        WifiManager manager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = manager.getConnectionInfo();
        if (wifiInfo == null)
            return null;

        String hostname = Formatter.formatIpAddress(wifiInfo.getIpAddress());
        //byte[] byteaddr = new byte[] {
        // (byte) (intaddr & 0xff),
        // (byte) (intaddr >> 8 & 0xff),
        // (byte) (intaddr >> 16 & 0xff),
        // (byte) (intaddr >> 24 & 0xff) };

        final InetAddress address;

        try {
            address = InetAddress.getByName(hostname);
        } catch (UnknownHostException ex) {
            ex.printStackTrace();
            return null;
        }

        return address;
    }

    public static boolean isInternetAvailable(Context context) {
        try {
            HttpURLConnection urlc = (HttpURLConnection) (new URL("http://www.google.com").openConnection());
            urlc.setRequestProperty("User-Agent", "Test");
            urlc.setRequestProperty("Connection", "close");
            urlc.setConnectTimeout(1500);
            urlc.connect();
            return (urlc.getResponseCode() == 200);
        } catch (IOException e) {
            Log.e("InternetCheck", "Error checking internet connection " + e.getMessage());
        }
        return false;
    }

    public static boolean isWiFiOn(Context context) {

        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(
                Context.WIFI_SERVICE);
        return wifiManager != null && wifiManager.isWifiEnabled();
    }

    /**
     * Search for the IP of local AP
     * @return Return WiFi interface hotspot IP iff hotspot enabled
     */
    public static String getLocalAPIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                if (intf != null && intf.getName() != null && (intf.getName().startsWith("swlan0")
                        || intf.getName().startsWith("wlan0"))) {
                    //Few devices maintain swlan and few wlan

                    for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                        InetAddress inetAddress = enumIpAddr.nextElement();
                        if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                            String apIp = inetAddress.getHostAddress();
                             if(apIp.contains(HOTSPOT_IP_ADDRESS)) {
                                 return apIp;
                             }
                        }
                    }
                }
            }
        } catch (SocketException ex) {
            ex.printStackTrace();
        }
        return null;
    }
    public static String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                if (intf != null && intf.getName() != null && (intf.getName().startsWith("wlan0"))) {
                    //Few devices maintain swlan and few wlan

                    for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                        InetAddress inetAddress = enumIpAddr.nextElement();
                        if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                            String ip = inetAddress.getHostAddress();
                             if(AddressUtil.isValidIPAddress(ip)) {
                                 return ip;
                             }
                        }
                    }
                }
            }
        } catch (SocketException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static boolean isHotSpotEnabled() {
        return getLocalAPIpAddress() != null;
    }
}
