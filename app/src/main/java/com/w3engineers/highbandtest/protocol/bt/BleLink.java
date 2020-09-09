package com.w3engineers.highbandtest.protocol.bt;

import android.bluetooth.BluetoothSocket;

import com.google.gson.Gson;
import com.w3engineers.highbandtest.protocol.model.BaseMessage;
import com.w3engineers.highbandtest.protocol.model.HelloMessage;
import com.w3engineers.highbandtest.protocol.model.Credential;
import com.w3engineers.highbandtest.util.Constant;
import com.w3engineers.highbandtest.util.HandlerUtil;
import com.w3engineers.highbandtest.util.MeshLog;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class BleLink extends Thread {
    public enum State {
        CONNECTING,
        CONNECTED,
        DISCONNECTED
    }

    private int RANDOM_DELAY_MAX = 16;
    private int RANDOM_DELAY_MIN = 12;

    private volatile State state = State.CONNECTING;
    private BluetoothSocket bSocket;
    private DataInputStream in;
    private DataOutputStream out;
    private MessageListener messageListener;
    private Queue<byte[]> outputQueue = new LinkedList<>();

    private volatile boolean shouldCloseWhenOutputIsEmpty = false;
    private String nodeId;
    private ScheduledThreadPoolExecutor pool;
    private ExecutorService outputExecutor;
    private LinkMode linkMode;
    public int mUserMode;
    public String publicKey;
    public static final long BT_TIMEOUT = 50 * 1000;

    private static BleLink mBleLink;
    private static Object object = new Object();


    private BleLink(BluetoothSocket bluetoothSocket, MessageListener connectionListener, LinkMode state) {
        try {
            this.bSocket = bluetoothSocket;
            this.in = new DataInputStream(bluetoothSocket.getInputStream());
            this.out = new DataOutputStream(bluetoothSocket.getOutputStream());
            this.messageListener = connectionListener;
            this.linkMode = state;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static BleLink on(BluetoothSocket bluetoothSocket, MessageListener connectionListener, LinkMode state) {
        BleLink bleLink = mBleLink;
        if (bleLink == null) {
            synchronized (object) {
                bleLink = mBleLink;
                if (bleLink == null) {
                    bleLink = mBleLink = new BleLink(bluetoothSocket, connectionListener, state);
                }
            }
        }
        return bleLink;
    }

    public static BleLink getBleLink() {
        return mBleLink;
    }

    public LinkMode getLinkMode() {
        return linkMode;
    }

    public void writeFrame(byte[] frame) {
        // Output thread.
        byte[] buffer = frame;

        ByteBuffer header = ByteBuffer.allocate(4);
        header.order(ByteOrder.BIG_ENDIAN);
        header.putInt(buffer.length);
        try {
            out.write(header.array());
            out.write(buffer);
            out.flush();
        } catch (IOException ex) {
            MeshLog.e(" Message Write fails in BlueTooth Link" + ex.getMessage());
            try {
                out.close();
                MeshLog.e("BT socket closed ");
                bSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    } // writeFrame


    @Override
    public void run() {

        int bufferSize = 4096;
        ByteBuf inputData = Unpooled.buffer(bufferSize);
        inputData.order(ByteOrder.BIG_ENDIAN);


        try {
            int len;
            while (true) {
                inputData.ensureWritable(bufferSize, true);

                len = in.read(
                        inputData.array(),
                        inputData.writerIndex(),
                        bufferSize);
                if (len < 0) {
                    continue;
                }

                inputData.writerIndex(inputData.writerIndex() + len);

                if (!formFrames(inputData)) {
                    break;

                }
                inputData.discardReadBytes();
                inputData.capacity(inputData.writerIndex() + bufferSize);
            } // while
        } catch (InterruptedIOException ex) {
            MeshLog.i("BT InterruptedIOException");
            try {
                in.close();
            } catch (IOException ioex) {

            }

            notifyDisconnect("InterruptedIOException -> " + ex.getMessage());
            return;
        } catch (IOException e) {
            MeshLog.i("BT IOException");
            try {
                in.close();
            } catch (IOException ioex) {

            }
            notifyDisconnect("IOException -> " + e.getMessage());
            return;
        }

    }


    private boolean formFrames(ByteBuf inputData) {
        final int headerSize = 4;

        while (true) {
            if (inputData.readableBytes() < headerSize)
                break;

            inputData.markReaderIndex();
            int frameSize = inputData.readInt();

            if (frameSize > Constant.frameSizeMax) {
                return false;
            }

            if (inputData.readableBytes() < frameSize) {
                inputData.resetReaderIndex();
                break;
            }

            //final Frames.Frame frame;

            final byte[] frameBody = new byte[frameSize];
            inputData.readBytes(frameBody, 0, frameSize);

            try {
                String msg = new String(frameBody);
                new Thread(() -> processMessage(msg)).start();
            } catch (Exception ex) {
                ex.printStackTrace();
                continue;
            }

        }

        return true;
    }

    private void processMessage(String msg) {
        BaseMessage message = new Gson().fromJson(msg, BaseMessage.class);

        if (message instanceof HelloMessage) {
            messageListener.onHelloMessageReceiver((HelloMessage) message);
        } else if (message instanceof Credential) {
            messageListener.onCredentialReceived((Credential) message);
        }
    }


    private void notifyDisconnect(String from) {
        this.state = State.DISCONNECTED;
        MeshLog.e(" Ble link disconnected due to =" + from);
        try {
            out.close();
            in.close();
            if (bSocket != null)
                MeshLog.e("BT socket closed ");
            bSocket.close();
        } catch (IOException e) {
            //CrashReporter.logException(e);
        }
        pool.shutdown();
        outputExecutor.shutdown();

        HandlerUtil.postBackground(() -> messageListener.onBluetoothDisconnected());

        mBleLink = null;

    }
}
