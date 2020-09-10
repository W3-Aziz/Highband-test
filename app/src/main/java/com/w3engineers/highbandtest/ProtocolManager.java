package com.w3engineers.highbandtest;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.WifiInfo;
import android.widget.Toast;

import com.google.gson.Gson;
import com.w3engineers.highbandtest.protocol.bt.BleLink;
import com.w3engineers.highbandtest.protocol.bt.BluetoothClient;
import com.w3engineers.highbandtest.protocol.bt.BluetoothDeviceReceiver;
import com.w3engineers.highbandtest.protocol.bt.BluetoothServer;
import com.w3engineers.highbandtest.protocol.bt.ConnectionState;
import com.w3engineers.highbandtest.protocol.bt.LinkMode;
import com.w3engineers.highbandtest.protocol.bt.MessageListener;
import com.w3engineers.highbandtest.protocol.data.AppMessageListener;
import com.w3engineers.highbandtest.protocol.model.BtMessage;
import com.w3engineers.highbandtest.protocol.model.Credential;
import com.w3engineers.highbandtest.protocol.model.HelloMessage;
import com.w3engineers.highbandtest.protocol.wifi.libmeshx.wifi.WiFiClient;
import com.w3engineers.highbandtest.protocol.wifi.libmeshx.wifid.APCredentials;
import com.w3engineers.highbandtest.protocol.wifi.libmeshx.wifid.WiFiDirectManagerLegacy;
import com.w3engineers.highbandtest.protocol.wifi.libmeshx.wifid.WiFiMeshConfig;
import com.w3engineers.highbandtest.util.HandlerUtil;
import com.w3engineers.highbandtest.util.MeshLog;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;

public class ProtocolManager implements MessageListener, BluetoothDeviceReceiver.BTDiscoveryListener {

    public static final String SERVICE_TYPE = "xyz.m";
    //public static String mMyBTName = "abc";
    public static String mMySSIDName;
    public static final int HTTP_PORT = 6565;
    private final long BT_SEARCH_DELAY = 15000;

    private BluetoothServer bluetoothServer;
    private BluetoothClient bluetoothClient;
    private static ProtocolManager protocolManager;
    private BluetoothDeviceReceiver mBluetoothDiscoveryReceiver;
    private BluetoothAdapter bluetoothAdapter;
    private Context mContext;
    private BleLink mBleLink;
    public static final String BLUETOOTH_PREFIX = "xaom";
    public static String bluetoothName;
    private WiFiDirectManagerLegacy mWiFiDirectManagerLegacy;
    public AppMessageListener mAppMessageListener;
    private List<String> connectedDeviceBtName;
    private volatile Queue<BluetoothDevice> mBluetoothDevices;


    private ProtocolManager(Context context) {
        this.mContext = context;
        connectedDeviceBtName = new ArrayList<>();
        mBluetoothDevices = new LinkedList<>();
        this.bluetoothServer = new BluetoothServer("Node id", this);
        this.bluetoothClient = new BluetoothClient("Node id", this, bluetoothServer);
        this.bluetoothServer.starListenThread();
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBluetoothDiscoveryReceiver = new BluetoothDeviceReceiver(this);
        mWiFiDirectManagerLegacy = WiFiDirectManagerLegacy.getInstance(mContext, null, null, null);
        mWiFiDirectManagerLegacy.mConnectionListener = new WiFiClient.ConneectionListener() {
            @Override
            public void onConnected(WifiInfo wifiConnectionInfo, String passPhrase) {
                MeshLog.v("[highband] Wifi connected .............");
                if (mBleLink != null) {
                    mBleLink.notifyDisconnect(getClass().getSimpleName());
                }
            }

            @Override
            public void onTimeOut() {

            }

            @Override
            public void onDisconnected() {

            }
        };
    }

    public static ProtocolManager on(Context context) {
        if (protocolManager == null) {
            protocolManager = new ProtocolManager(context);
        }
        return protocolManager;
    }


    public void startProtocol() {
        MeshLog.v("**** Highband mesh started *****");
        bluetoothName = BLUETOOTH_PREFIX + "-" + getRandomString();
        bluetoothAdapter.setName(bluetoothName);
        registerBTDiscoveryReceiver();
        startBtSearch();
        runPeriodicBtSearchThread();
    }

    private Runnable periodicBluetoothSearchRunnable = new Runnable() {
        @Override
        public void run() {
            if (mBleLink == null) {
                MeshLog.v("Periodic BT search triggered");
                startBtSearch();
            }
            HandlerUtil.postBackground(this, BT_SEARCH_DELAY);
        }
    };

    private void runPeriodicBtSearchThread() {
        HandlerUtil.postBackground(periodicBluetoothSearchRunnable, BT_SEARCH_DELAY);
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

    private void sendCredential() {
        APCredentials credential = mWiFiDirectManagerLegacy.getAPCredentials();
        if (credential != null && mBleLink != null) {
            MeshLog.v("BT credential send.. ssid: " + credential.mSSID + " pass :" + credential.mPassPhrase);
            Credential credentialMessage = new Credential(credential.mSSID, credential.mPassPhrase);
            String string = new Gson().toJson(credentialMessage);
            mBleLink.writeFrame(string.getBytes());
        } else {
            MeshLog.v("BT credential send.. null: ");
        }
    }


    @Override
    public void onBluetoothConnected(BleLink link) {
        mBleLink = link;
        bluetoothServer.stopListenThread();
        unregisterBluetoothReceiver();
        stopBtSearch();
        if (link.getLinkMode() == LinkMode.SERVER && (connectedDeviceBtName == null ||
                connectedDeviceBtName.isEmpty())) {
            MeshLog.v("[highBand]Link mode server");
            showToast("Bt connected as master");

            mWiFiDirectManagerLegacy.mWiFiMeshConfig = new WiFiMeshConfig();
            mWiFiDirectManagerLegacy.mWiFiMeshConfig.mIsGroupOwner = true;

            mWiFiDirectManagerLegacy.start();
        } else {
            MeshLog.v("[highBand]Link mode client");
            showToast("Bt connected as client");
        }

        HandlerUtil.postBackground(() -> sendCredential(), 1500);
    }

    @Override
    public void onBluetoothDisconnected() {
        mBleLink = null;
        bluetoothServer.starListenThread();
        registerBTDiscoveryReceiver();
        startBtSearch();
    }

    @Override
    public void onCredentialReceived(Credential credential) {
        if (mWiFiDirectManagerLegacy != null) {
            MeshLog.v("BT credential received ssid :" + credential.ssid + " password :" + credential.password);
            mWiFiDirectManagerLegacy.connectWithAP(credential);
        }
    }

    @Override
    public void onHelloMessageReceiver(HelloMessage helloMessage) {
        connectedDeviceBtName.add(helloMessage.bleName);
    }

    @Override
    public void onMessage(String message) {
        if (mAppMessageListener != null) {
            mAppMessageListener.onMessage(message);
        }
    }

    @Override
    public void onBtMessageReceived(BtMessage btMessage) {
        showToast(btMessage.message);
    }


    @Override
    public void onBluetoothFound(List<BluetoothDevice> bluetoothDevices) {
        for (BluetoothDevice item : bluetoothDevices) {
            if (!connectedDeviceBtName.contains(item.getName())) {
                mBluetoothDevices.add(item);
            }
        }

        if (!mBluetoothDevices.isEmpty()) {
            makeBtConnection();
        }
    }

    @Override
    public void onScanFinished() {
    }


    private void makeBtConnection() {
        if (!mBluetoothDevices.isEmpty()) {

            if (bluetoothClient.isBtConnecting()) {
                return;
            }

            BluetoothDevice device = mBluetoothDevices.poll();
            MeshLog.v("Attempt to connect bt connection :" + device.getName());
            bluetoothClient.createConnection(device, new ConnectionState() {
                @Override
                public void onConnectionState(String deviceName, boolean isConnected) {
                    if (!isConnected) {
                        makeBtConnection();
                    }
                }
            });
        }
    }

    private void showToast(String message) {
        HandlerUtil.postForeground(() -> Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show());
    }

    public void sendBtMessage() {
        HandlerUtil.postBackground(new Runnable() {
            @Override
            public void run() {
                if (mBleLink != null) {
                    BtMessage btMessage = new BtMessage("Bt message received");
                    mBleLink.writeFrame(btMessage.toJson().getBytes());
                } else {
                    showToast("BT link null");
                }
            }
        });
    }
}
