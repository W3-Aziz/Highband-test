package com.w3engineers.highbandtest.protocol.bt;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.List;

class BluetoothDeviceReceiver extends BroadcastReceiver {
    public interface BTDiscoveryListener {
        void onBluetoothFound(List<BluetoothDevice> bluetoothDevices);
        void onScanFinished();
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null || action.isEmpty()) return;
        switch (action) {
            case BluetoothDevice.ACTION_FOUND:
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String name = device.getName();
        }
    }
}
