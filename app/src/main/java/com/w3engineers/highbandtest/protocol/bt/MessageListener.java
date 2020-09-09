package com.w3engineers.highbandtest.protocol.bt;

import com.w3engineers.highbandtest.protocol.model.BtMessage;
import com.w3engineers.highbandtest.protocol.model.Credential;
import com.w3engineers.highbandtest.protocol.model.HelloMessage;

public interface MessageListener {
    void onBluetoothConnected(BleLink link);
    void onBluetoothDisconnected();
    void onCredentialReceived(Credential credential);
    void onHelloMessageReceiver(HelloMessage helloMessage);
    void onMessage(String message);

    void onBtMessageReceived(BtMessage btMessage);
}
