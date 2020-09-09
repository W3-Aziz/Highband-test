package com.w3engineers.highbandtest.protocol.bt;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.w3engineers.highbandtest.ProtocolManager;
import com.w3engineers.highbandtest.util.HandlerUtil;
import com.w3engineers.highbandtest.util.MeshLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BluetoothDeviceReceiver extends BroadcastReceiver {
    public interface BTDiscoveryListener {
        void onBluetoothFound(List<BluetoothDevice> bluetoothDevices);

        void onScanFinished();
    }

    private BTDiscoveryListener btDiscoveryListener;
    private Map<String, BluetoothDevice> mBluetoothDeviceMap;
    private final long DISCOVERY_NODE_CACHING_TIME = 3000;
    public BluetoothDeviceReceiver(BTDiscoveryListener listener) {
        this.btDiscoveryListener = listener;
        mBluetoothDeviceMap = new HashMap<>();
    }

    private Runnable postDeviceList = () -> {
        if (mBluetoothDeviceMap.size() > 0) {
            btDiscoveryListener.onBluetoothFound(new ArrayList<>(mBluetoothDeviceMap.values()));
            mBluetoothDeviceMap.clear();
        }
    };

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null || action.isEmpty()) return;
        switch (action) {
            case BluetoothDevice.ACTION_FOUND:
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String name = device.getName();
                if (name != null && name.contains(ProtocolManager.BLUETOOTH_PREFIX)) {
                    mBluetoothDeviceMap.put(device.getAddress(), device);
                    MeshLog.v("Bluetooth device discovered :" + name);
                }

                if (mBluetoothDeviceMap.size() > 0) {
                    btDiscoveryListener.onBluetoothFound(new ArrayList<>(mBluetoothDeviceMap.values()));
                    mBluetoothDeviceMap.clear();
                }
                break;

                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    btDiscoveryListener.onScanFinished();
                    break;
        }
    }
}
