package com.w3engineers.highbandtest;

import android.content.Context;

import com.w3engineers.highbandtest.protocol.bt.BleLink;
import com.w3engineers.highbandtest.protocol.bt.BluetoothClient;
import com.w3engineers.highbandtest.protocol.bt.BluetoothServer;
import com.w3engineers.highbandtest.protocol.bt.MessageListener;
import com.w3engineers.highbandtest.util.MeshLog;

class ProtocolManager implements MessageListener {
    private BluetoothServer bluetoothServer;
    private BluetoothClient bluetoothClient;
    private static ProtocolManager protocolManager;
    private Context mContext;

    private ProtocolManager(Context context){
        this.mContext = context;
        this.bluetoothServer = new BluetoothServer("Node id", this::onBluetoothConnected);
        this.bluetoothClient = new BluetoothClient("", this::onBluetoothConnected, bluetoothServer);
    }

    public ProtocolManager on(Context context){
        if(protocolManager != null){
            protocolManager = new ProtocolManager(context);
        }
        return protocolManager;
    }

    @Override
    public void onBluetoothConnected(BleLink link) {

    }
}
