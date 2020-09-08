package com.w3engineers.highbandtest.protocol.bt;

import com.w3engineers.highbandtest.protocol.model.Credential;

public interface MessageListener {
    void onBluetoothConnected(BleLink link);
    void onCredentialReceived(Credential credential);
}
