package com.w3engineers.highbandtest.protocol.wifi.libmeshx.wifi;

/**
 * Provides p2p Client connection or disconnection event
 */
public interface WiFiClientState {

    void onConnected();
    void onDisconnected();

}
