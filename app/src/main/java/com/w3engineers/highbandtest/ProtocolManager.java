package com.w3engineers.highbandtest;

import android.content.Context;

import com.w3engineers.highbandtest.protocol.bt.BleLink;
import com.w3engineers.highbandtest.protocol.bt.BluetoothClient;
import com.w3engineers.highbandtest.protocol.bt.BluetoothServer;
import com.w3engineers.highbandtest.protocol.bt.LinkMode;
import com.w3engineers.highbandtest.protocol.bt.MessageListener;
import com.w3engineers.highbandtest.protocol.model.Credential;
import com.w3engineers.highbandtest.util.MeshLog;

public class ProtocolManager implements MessageListener {
    private BluetoothServer bluetoothServer;
    private BluetoothClient bluetoothClient;
    private static ProtocolManager protocolManager;
    private Context mContext;
    private BleLink mBleLink;

    private ProtocolManager(Context context){
        this.mContext = context;
        this.bluetoothServer = new BluetoothServer("Node id", this);
        this.bluetoothClient = new BluetoothClient("Node id", this, bluetoothServer);
        this.bluetoothServer.starListenThread();
    }

    public ProtocolManager on(Context context){
        if(protocolManager != null){
            protocolManager = new ProtocolManager(context);
        }
        return protocolManager;
    }

    @Override
    public void onBluetoothConnected(BleLink link) {
        mBleLink = link;

        if(link.getLinkMode() == LinkMode.SERVER){

        }else {

        }
    }

    @Override
    public void onCredentialReceived(Credential credential) {

    }
}
