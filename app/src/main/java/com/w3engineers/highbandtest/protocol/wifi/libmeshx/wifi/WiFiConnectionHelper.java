package com.w3engineers.highbandtest.protocol.wifi.libmeshx.wifi;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import java.util.List;


/**
 * All native WiFi adapter related helper functions
 */
public class WiFiConnectionHelper {

    private WifiManager mWifiManager;
    private Context mContext;

    public WiFiConnectionHelper(Context context) {
        mWifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        this.mContext = context;
    }

    public boolean disconnect() {
        if (mWifiManager != null) {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                // TODO: 1/22/2020 Will add removeSuggestions API upon upgrading API level over 29
            } else {
                return mWifiManager.disconnect();
            }
        }
        return false;
    }

    /**
     * Disable all configured wifi network. We want to stop any automatic connection.
     */
    public void disableAllConfiguredWiFiNetworks() {
        List<WifiConfiguration> configuredNetworks = mWifiManager.getConfiguredNetworks();

        if (configuredNetworks != null) {
            for (WifiConfiguration wifiConfiguration : configuredNetworks) {
                if (wifiConfiguration != null && wifiConfiguration.networkId != -1) {
                    boolean isDisabled = mWifiManager.disableNetwork(wifiConfiguration.networkId);
                }
            }
        }
    }

    public boolean disableConfiguredWiFiNetwork(int networkId) {
        if (networkId != -1) {
            return mWifiManager.disableNetwork(networkId);
        }
        return false;
    }

    public int getConnectedNetworkId() {
        return getConnectionInfo().getNetworkId();
    }

    /**
     * Disable all configured wifi network. We want to stop any automatic connection.
     */
    public int getConfiguredWiFiNetworkId(String SSID) {
        if (TextUtils.isEmpty(SSID)) {
            return -1;
        }
        List<WifiConfiguration> configuredNetworks = mWifiManager.getConfiguredNetworks();

        if (configuredNetworks != null) {

            for (WifiConfiguration wifiConfiguration : configuredNetworks) {
                if (wifiConfiguration != null && wifiConfiguration.networkId != -1) {
                    if (SSID.equals(wifiConfiguration.SSID)) {
                        return wifiConfiguration.networkId;
                    }
                }
            }
        }

        return -1;
    }

    public boolean connect(String ssid, String passPhrase) {

        /*if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {

            NetworkSpecifier specifier = new WifiNetworkSpecifier.Builder()
                    .setSsid(ssid)
                    .setWpa2Passphrase(passPhrase)
                    .build();

            NetworkRequest request =
                    new NetworkRequest.Builder()
                            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)

                            .setNetworkSpecifier(specifier)
                            .build();

            ConnectivityManager connectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(@NonNull Network network) {
                    super.onAvailable(network);
                    connectivityManager.bindProcessToNetwork(network);
                }

            };

            connectivityManager.requestNetwork(request, networkCallback);

            return true;
        } else {*/

        //forgetAllNetwork();

        WifiConfiguration wifiConfig = new WifiConfiguration();
        wifiConfig.status = WifiConfiguration.Status.ENABLED;
        wifiConfig.SSID = String.format("\"%s\"", ssid);
        wifiConfig.preSharedKey = String.format("\"%s\"", passPhrase);

        int networkId = getConfiguredWiFiNetworkId(ssid);
        Log.e("Highband-bt", "High band net id : " + networkId);
        if (networkId != -1) {
            wifiConfig.networkId = networkId;
            networkId = mWifiManager.updateNetwork(wifiConfig);
            if (networkId == -1) {
                networkId = this.mWifiManager.addNetwork(wifiConfig);
            }
            Log.e("Highband-bt", "High band net id2 : " + networkId);
        } else {
            networkId = this.mWifiManager.addNetwork(wifiConfig);
        }


        Log.e("Highband-bt", "High band net id3 : " + networkId);
        boolean isDisconnected = mWifiManager.disconnect();
        boolean isEnabled = mWifiManager.enableNetwork( networkId, true);
        boolean status = mWifiManager.reconnect();
        return status && isEnabled && isDisconnected;
        /* }*/
    }

    public WifiInfo getConnectionInfo() {

        return mWifiManager.getConnectionInfo();
    }

    public boolean isWiFiOn() {

        return mWifiManager != null && mWifiManager.isWifiEnabled();
    }

    public void removeNetwork(int netId) {
        if (netId > 0 && mWifiManager != null) {
            boolean status = mWifiManager.removeNetwork(netId);
            mWifiManager.saveConfiguration();
        }

    }

    public void setHighBand() {
    }

    public void releaseHighBand() {
    }

}