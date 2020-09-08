package com.w3engineers.highbandtest.protocol.bt;

interface ConnectionState {
    void onConnectionState(String deviceName, boolean isConnected);
}
