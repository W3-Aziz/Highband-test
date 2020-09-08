package com.w3engineers.highbandtest.protocol.bt;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.w3engineers.highbandtest.util.MeshLog;

import java.util.List;

public class BluetoothDeviceReceiver extends BroadcastReceiver {
    public interface BTDiscoveryListener {
        void onBluetoothFound(List<BluetoothDevice> bluetoothDevices);

        void onScanFinished();
    }

    private BTDiscoveryListener btDiscoveryListener;

    public BluetoothDeviceReceiver(BTDiscoveryListener listener) {
        this.btDiscoveryListener = listener;
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null || action.isEmpty()) return;
        switch (action) {
            case BluetoothDevice.ACTION_FOUND:
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String name = device.getName();
                MeshLog.v("Bluetooth device discovered :" + name);
        }
    }
}
