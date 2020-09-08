package com.w3engineers.highbandtest.protocol.bt;

import com.w3engineers.highbandtest.protocol.model.Credential;

public interface MessageListener {
    void onBluetoothConnected(BleLink link);
    void onBluetoothDisconnected();
    void onCredentialReceived(Credential credential);

    void onMessage(String message);
}
