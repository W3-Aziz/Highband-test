package com.w3engineers.highbandtest.protocol.wifi.libmeshx.wifi;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.util.Log;

import com.w3engineers.highbandtest.protocol.util.AndroidUtil;
import com.w3engineers.highbandtest.protocol.util.WiFiUtil;
import com.w3engineers.mesh.libmeshx.discovery.MeshXLogListener;


/**
 * Maintain WiFi client related scenarios
 */
public class WiFiClient implements WiFiClientState {

    public static final long WIFI_CONNECTION_TIMEOUT = 20 * 1000L;

    // Don't change the value without team discussion
    public static final long WIFI_DISCONNECTION_TIMEOUT = 1 * 1000L;

    public interface ConneectionListener {
        void onConnected(WifiInfo wifiConnectionInfo, String passPhrase);

        void onTimeOut();

        void onDisconnected();
    }

    private Context mContext;
    private WiFiConnectionHelper mWiFiHelper;
    private WiFiClientStateReceiver mWiFiClientStateReceiver;
    private volatile boolean mIsConnected;
    private MeshXLogListener mMeshXLogListener;
    private ConneectionListener mConnectionListener;
    private String mConnectingPassPhrase;
    private Runnable mTimeOutTask = new Runnable() {
        @Override
        public void run() {
            if (!mIsConnected && mConnectionListener != null) {
                mConnectionListener.onTimeOut();
            }
        }
    };

    public WiFiClient(Context context) {

        mWiFiClientStateReceiver = new WiFiClientStateReceiver(context, this);

        mWiFiHelper = new WiFiConnectionHelper(context);

        mContext = context;
    }

    public void setMeshXLogListener(MeshXLogListener meshXLogListener) {
        mMeshXLogListener = meshXLogListener;
    }

    /**
     * Start connecting with provided ssid with given passphrase. This method works only
     * if it is not connected with any network.
     *
     * @param ssid
     * @param passPhrase
     * @param conneectionListener
     * @return
     */
    public boolean connect(String ssid, String passPhrase, ConneectionListener conneectionListener) {

        if (!mIsConnected && ssid != null && !ssid.equals(WiFiUtil.getConnectedSSID(mContext))) {

            mConnectingPassPhrase = passPhrase;

            if(conneectionListener != null) {
                mConnectionListener = conneectionListener;
            }
            Log.e("Highband-bt","High band credential attempt to connect");
            //mWiFiHelper.disconnect();

            boolean isConnecting = mWiFiHelper.connect(ssid, passPhrase);
            String logText = "[CONNECTING] SSID - " + ssid + "::Passphrase - "
                    + passPhrase+"-connecting..."+isConnecting;

            if (mMeshXLogListener != null) {
                mMeshXLogListener.onLog(logText);
            }
            if (isConnecting) {
                AndroidUtil.postBackground(mTimeOutTask, WIFI_CONNECTION_TIMEOUT);
                return true;
            }
        }

        return true;
    }

    /**
     * Disassociate from currently active network.
     * Works well upto API 28.
     * Also disable the network which is connected.
     * @return
     */
    public boolean disConnect() {

        if (mIsConnected) {

            if(mWiFiHelper.disconnect()) {
                mWiFiHelper.disableConfiguredWiFiNetwork(mWiFiHelper.getConnectedNetworkId());
                return true;
            }
        }

        return false;
    }

    public void setConnectionListener(ConneectionListener conneectionListener) {
        mConnectionListener = conneectionListener;
    }

    public void destroy() {
        if (mWiFiClientStateReceiver != null) {
            mWiFiClientStateReceiver.destroy();
        }
    }

    public boolean isConnected() {
        return mIsConnected;
    }


    public boolean isWiFiOn() {
        return mWiFiHelper.isWiFiOn();
    }

    @Override
    public void onConnected() {

        AndroidUtil.removeBackground(mTimeOutTask);

        if (!mIsConnected) {
            mIsConnected = true;
            if (mConnectionListener != null) {
                mConnectionListener.onConnected(mWiFiHelper.getConnectionInfo(), mConnectingPassPhrase);
            }

            if (mMeshXLogListener != null) {
                mMeshXLogListener.onLog("[Connected]");
            }
        }
    }

    @Override
    public void onDisconnected() {
        mIsConnected = false;

        if (mConnectionListener != null) {
            mConnectionListener.onDisconnected();
        }
    }

    public void setHighBand() {
        if(mWiFiHelper != null) {
            mWiFiHelper.setHighBand();
        }
    }

    public void releaseHighBand() {
        if(mWiFiHelper != null) {
            mWiFiHelper.releaseHighBand();
        }
    }
}
