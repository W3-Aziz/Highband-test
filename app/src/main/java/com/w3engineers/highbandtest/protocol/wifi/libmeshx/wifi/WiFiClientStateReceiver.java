package com.w3engineers.highbandtest.protocol.wifi.libmeshx.wifi;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;

/**
 * Receiver of Wifi client
 */
public class WiFiClientStateReceiver {

    private enum ConnectionState {
        CONNECTED,
        CONNECTING,
        DISCONNECTED
    }

    private boolean misDestroyed;
    private WiFiClientState mWiFiClientState;
    private ConnectionState mConnectionState = ConnectionState.DISCONNECTED;
    private ConnectivityManager mConnectivityManager;

    public WiFiClientStateReceiver(Context context, final WiFiClientState wiFiClientState) {

        mWiFiClientState = wiFiClientState;
        misDestroyed = false;

        mConnectivityManager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkRequest.Builder builder = new NetworkRequest.Builder();
        builder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
    }

    public void destroy() {

        misDestroyed = true;
    }
}
