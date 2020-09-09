package com.w3engineers.highbandtest.protocol.wifi.libmeshx.wifi;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;

import com.w3engineers.highbandtest.protocol.util.NetworkCallBackReceiver;

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
    private NetworkCallBackReceiver mNetworkCallback;

    public WiFiClientStateReceiver(Context context, final WiFiClientState wiFiClientState) {

        mWiFiClientState = wiFiClientState;
        misDestroyed = false;

        mConnectivityManager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkRequest.Builder builder = new NetworkRequest.Builder();
        builder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);

        mNetworkCallback = new NetworkCallBackReceiver() {
            /**
             * @param network
             */
            @Override
            public void onAvailable(Network network) {
                if(!misDestroyed) {


                    NetworkInfo networkInfo = mConnectivityManager.getNetworkInfo(network);
                    if (mConnectionState != ConnectionState.CONNECTED &&
                            networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {

                        mConnectionState = ConnectionState.CONNECTED;
                        mWiFiClientState.onConnected();
                    }
                }
            }

            /**
             * @param network
             */
            @Override
            public void onLost(Network network) {
                if (mConnectionState != ConnectionState.DISCONNECTED) {
                    mConnectionState = ConnectionState.DISCONNECTED;
                    mWiFiClientState.onDisconnected();
                }
            }
        };

        mConnectivityManager.registerNetworkCallback(
                builder.build(),
                mNetworkCallback
        );
    }

    public void destroy() {

        misDestroyed = true;
        try {
            mConnectivityManager.unregisterNetworkCallback(mNetworkCallback);

        } catch (IllegalArgumentException illegalArgumentException) {
//            illegalArgumentException.printStackTrace();
        }
    }
}
