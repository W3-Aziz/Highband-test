package com.w3engineers.highbandtest.protocol.bt;

public interface ConnectionState {
    void onConnectionState(String deviceName, boolean isConnected);
}
