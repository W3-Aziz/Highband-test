package com.w3engineers.highbandtest;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.IntentFilter;

import com.w3engineers.highbandtest.protocol.bt.BleLink;
import com.w3engineers.highbandtest.protocol.bt.BluetoothClient;
import com.w3engineers.highbandtest.protocol.bt.BluetoothDeviceReceiver;
import com.w3engineers.highbandtest.protocol.bt.BluetoothServer;
import com.w3engineers.highbandtest.protocol.bt.LinkMode;
import com.w3engineers.highbandtest.protocol.bt.MessageListener;
import com.w3engineers.highbandtest.protocol.model.Credential;
import com.w3engineers.highbandtest.util.MeshLog;

import java.util.List;
import java.util.UUID;

public class ProtocolManager implements MessageListener, BluetoothDeviceReceiver.BTDiscoveryListener {

    public static final String SERVICE_TYPE = "xyz.m";
    public static String mMyBTName = "abc";
    public static String mMySSIDName;

    private BluetoothServer bluetoothServer;
    private BluetoothClient bluetoothClient;
    private static ProtocolManager protocolManager;
    private BluetoothDeviceReceiver mBluetoothDiscoveryReceiver;
    private BluetoothAdapter bluetoothAdapter;
    private Context mContext;
    private BleLink mBleLink;
    public static final String BLUETOOTH_PREFIX = "prefix";

    private ProtocolManager(Context context) {
        this.mContext = context;
        this.bluetoothServer = new BluetoothServer("Node id", this);
        this.bluetoothClient = new BluetoothClient("Node id", this, bluetoothServer);
        this.bluetoothServer.starListenThread();
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBluetoothDiscoveryReceiver = new BluetoothDeviceReceiver(this);
    }

    public static ProtocolManager on(Context context) {
        if (protocolManager != null) {
            protocolManager = new ProtocolManager(context);
        }
        return protocolManager;
    }


    public void startProtocol() {
        MeshLog.v("**** Highband mesh started *****");
        String btName = BLUETOOTH_PREFIX + "-" + getRandomString();
        bluetoothAdapter.setName(btName);
        registerBTDiscoveryReceiver();
        startBtSearch();
    }

    private String getRandomString() {
        String uuid = UUID.randomUUID().toString();
        return uuid.substring(uuid.length() - 5);
    }

    private void registerBTDiscoveryReceiver() {
        MeshLog.i("Register:: ble device receiver");
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        mContext.registerReceiver(mBluetoothDiscoveryReceiver, intentFilter);
    }

    private void unregisterBluetoothReceiver() {
        try {
            mContext.unregisterReceiver(mBluetoothDiscoveryReceiver);
        } catch (IllegalArgumentException e) {
        }
    }

    private void startBtSearch() {
        bluetoothAdapter.startDiscovery();
    }

    private void stopBtSearch() {
        bluetoothAdapter.cancelDiscovery();
    }


    @Override
    public void onBluetoothConnected(BleLink link) {
        mBleLink = link;
        unregisterBluetoothReceiver();
        stopBtSearch();
        if (link.getLinkMode() == LinkMode.SERVER) {

        } else {

        }
    }

    @Override
    public void onBluetoothDisconnected() {
        /*bluetoothServer.starListenThread();
        registerBTDiscoveryReceiver();
        startBtSearch();*/
    }

    @Override
    public void onCredentialReceived(Credential credential) {

    }


    @Override
    public void onBluetoothFound(List<BluetoothDevice> bluetoothDevices) {

    }

    @Override
    public void onScanFinished() {

    }
}
