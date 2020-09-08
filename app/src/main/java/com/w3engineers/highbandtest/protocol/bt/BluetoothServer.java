package com.w3engineers.highbandtest.protocol.bt;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;

import com.google.gson.Gson;
import com.w3engineers.highbandtest.ProtocolManager;
import com.w3engineers.highbandtest.protocol.model.BtHello;
import com.w3engineers.highbandtest.util.Constant;
import com.w3engineers.highbandtest.util.MeshLog;

import java.io.IOException;

public class BluetoothServer {

    private BluetoothAdapter bluetoothAdapter;
    private String nodeId;
    private ConnectionListenThread connectionListenThread;
    private MessageListener messageListener;
    private String publicKey;

    private final Object mLock = new Object();
//    private String myUserInfo;

    public BluetoothServer(String nodeId, MessageListener bleListener) {
        this.nodeId = nodeId;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.messageListener = bleListener;
    }

    public void starListenThread() {
        if (connectionListenThread == null || !connectionListenThread.isRunning()) {
            synchronized (mLock) {
                connectionListenThread = new ConnectionListenThread();
                MeshLog.i(" [BT SERVER] -> Opened");
                if (connectionListenThread != null) {
                    connectionListenThread.start();
                }
            }
        }
    }

    public void stopListenThread() {
        if (connectionListenThread != null) {
            synchronized (mLock) {
                if (connectionListenThread != null) {
                    connectionListenThread.stop();
                    connectionListenThread = null;
                }
            }
        }
    }


    private class ConnectionListenThread implements Runnable {

        private Thread thread;
        private boolean isRunning;
        private BluetoothServerSocket bluetoothServerSocket;
        private volatile boolean isBtConnected = false;

        public ConnectionListenThread() {
            isRunning = true;
        }

        @Override
        public void run() {
            //while (isRunning) {
            MeshLog.i(" BLE Server running ........");
            try {
                bluetoothServerSocket = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(Constant.NAME_INSECURE,
                        Constant.MY_UUID_INSECURE);

                if (isBtConnected) return;
                BluetoothSocket bluetoothSocket = bluetoothServerSocket.accept();
                bluetoothAdapter.cancelDiscovery();
                isBtConnected = true;

                if (BleLink.getBleLink() != null) return;

                BleLink link = BleLink.on(bluetoothSocket, messageListener, LinkMode.SERVER);
                link.start();
                String hello = new Gson().toJson(new BtHello(ProtocolManager.bluetoothName));
                link.writeFrame(hello.getBytes());

                messageListener.onBluetoothConnected(link);

                MeshLog.v("Ble server accept connection");
            } catch (IOException e) {
//                e.printStackTrace();
                isRunning = false;
                isBtConnected = false;
                MeshLog.i(" Ble Socket thread closed.....");
            } catch (IllegalThreadStateException e) {
                MeshLog.i(" Ble Socket thread IllegalThreadStateException");
            } finally {
                if (bluetoothServerSocket != null) {
                    try {
                        bluetoothServerSocket.close();
                        isBtConnected = false;
                        MeshLog.i(" BLE Server Closed");
                    } catch (IOException e) {
//                        e.printStackTrace();
                    }
                }
            }
//            connectionListenThread = null;
            isRunning = false;
            //}
        }

        synchronized void start() {
            thread = new Thread(this, "listen");
            thread.setPriority(Thread.MAX_PRIORITY);
            isRunning = true;
            thread.start();
        }

        synchronized void stop() {
            /*if (!isRunning) {
                return;
            }*/
            try {
                if (bluetoothServerSocket != null) {
                    bluetoothServerSocket.close();
                    MeshLog.i(" [BT SERVER] -> Closed");
                } else {
                    MeshLog.i(" Bluetooth Socket NULL");
                }
                isRunning = false;
                isBtConnected = false;
                if (thread != null) {
                    thread.interrupt();
                    MeshLog.i(" Bluetooth Socket Thread interrupt");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        boolean isRunning() {
            return isRunning;
        }
    }


}
