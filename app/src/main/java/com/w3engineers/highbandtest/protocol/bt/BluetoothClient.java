package com.w3engineers.highbandtest.protocol.bt;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Build;
import android.util.Log;

import com.google.gson.Gson;
import com.w3engineers.highbandtest.protocol.model.HelloMessage;
import com.w3engineers.highbandtest.ProtocolManager;
import com.w3engineers.highbandtest.util.Constant;
import com.w3engineers.highbandtest.util.MeshLog;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

public class BluetoothClient {
    private BluetoothSocket bluetoothSocket;
    private Executor executor;
    private String myId;
    private MessageListener messageListener;
    private BluetoothServer server;
    public BluetoothDevice mBluetoothDevice;
    private BluetoothAdapter bluetoothAdapter;

    private volatile boolean isConnecting = false;

    public BluetoothClient(String nodeId, MessageListener bleListener, BluetoothServer server) {

        this.executor = Executors.newSingleThreadExecutor();
        this.myId = nodeId;
        this.messageListener = bleListener;
        this.server = server;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }
    public boolean isBtConnecting(){
        return isConnecting;
    }

    public void createConnection(final BluetoothDevice bluetoothDevice, final ConnectionState listener) {
        try {
            executor.execute(() -> {
                try {
                    isConnecting = true;
                    bluetoothAdapter.cancelDiscovery();
                    bluetoothSocket = createBluetoothSocket(bluetoothDevice, Constant.MY_UUID_INSECURE);

                    bluetoothSocket.connect();
                    if (BleLink.getBleLink() != null){
                        MeshLog.e("BT link already exist...");
                        return;
                    }

                    BleLink link = BleLink.on(bluetoothSocket, messageListener, LinkMode.CLIENT);
                    link.start();
                    String hello = new Gson().toJson(new HelloMessage(ProtocolManager.bluetoothName));
                    link.writeFrame(hello.getBytes());
                    server.stopListenThread();
                    listener.onConnectionState(bluetoothDevice.getName(), true);
                    //messageListener.onBluetoothConnected(link);
                    MeshLog.v("Bluetooth connection success");
                    isConnecting = false;
                } catch (IOException | IllegalThreadStateException e) {
                    MeshLog.v("Bluetooth connection failed");
                    Log.e("Bluetooth-dis", "Bt connection failed :" + e.getMessage());
                    e.printStackTrace();
                    try {
                        if (bluetoothSocket != null) {
                            bluetoothSocket.close();
                            bluetoothSocket = null;
                        }
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                    isConnecting = false;
                }
            });
        } catch (RejectedExecutionException executionException) {
            isConnecting = false;
        }

    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device, UUID uuid) throws IOException {
        if (Build.VERSION.SDK_INT >= 10) {
            try {
                final Method m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", new Class[]{UUID.class});
                return (BluetoothSocket) m.invoke(device, uuid);
            } catch (Exception e) {
                //Log.e(TAG, "Could not create Insecure RFComm Connection",e);
            }
        }
        return device.createRfcommSocketToServiceRecord(uuid);
    }


    public void stop() {
        if (executor instanceof ExecutorService) {
            ((ExecutorService) executor).shutdown();
        }

        stopConnectionProcess();
    }


    protected void stopConnectionProcess() {

        //Close socket if it is not in a complete connected state
        if (bluetoothSocket != null && !bluetoothSocket.isConnected()) {
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
