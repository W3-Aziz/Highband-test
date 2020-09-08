package com.w3engineers.highbandtest.protocol.wifi.libmeshx.wifid;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;

import com.w3engineers.highbandtest.ProtocolManager;

/**
 * Soft Access point searcher
 */
public class SoftAccessPointSearcher extends P2PServiceSearcher {

    public interface ServiceFound {
        void onServiceFoundSuccess(String ssid, String passPhrase, WifiP2pDevice wifiP2pDevice);
        void onP2pAlreadyConnected(String ssid);
    }

    private ServiceFound mServiceFound;

    public void setServiceFound(ServiceFound serviceFound) {
        mServiceFound = serviceFound;
    }

    public SoftAccessPointSearcher(Context context) {
        super(context);
        mServiceType = ProtocolManager.SERVICE_TYPE;
    }

    @Override
    protected void onDesiredServiceFound(String ssid, String passPhrase, WifiP2pDevice wifiP2pDevice) {
        if(mServiceFound != null && mIsAlive) {
            mServiceFound.onServiceFoundSuccess(ssid, passPhrase, wifiP2pDevice);
        }
    }

    @Override
    protected void onP2pAlreadyConnected(String ssid){
        if(mServiceFound != null ) {
            mServiceFound.onP2pAlreadyConnected(ssid);
        }
    }

    @Override
    public boolean start() {
        return super.start();
    }

    @Override
    public void stop() {
        super.stop();
    }
}
