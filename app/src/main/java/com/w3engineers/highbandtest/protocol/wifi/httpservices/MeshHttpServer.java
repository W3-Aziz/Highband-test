package com.w3engineers.highbandtest.protocol.wifi.httpservices;

import android.content.Context;
import android.net.Network;
import android.util.Pair;

import com.w3engineers.highbandtest.App;
import com.w3engineers.highbandtest.protocol.bt.MessageListener;
import com.w3engineers.highbandtest.protocol.model.BaseMessage;
import com.w3engineers.highbandtest.protocol.model.HelloMessage;
import com.w3engineers.highbandtest.protocol.util.Constant;
import com.w3engineers.highbandtest.protocol.util.P2PUtil;
import com.w3engineers.highbandtest.protocol.util.WiFiUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * NANO http server initializer
 */
public class MeshHttpServer implements NanoHTTPServer.HttpDataListener {

    public static final int DEFAULT_CONNECTION_TIMEOUT = 60 * 1000;
    private static MeshHttpServer server;
    private NanoHTTPServer nanoHTTPServer;
    private ExecutorService callableExecutor;
    private MessageListener messageListener;
    private int APP_PORT;
    private Context context;
    private volatile boolean iSDiscoveryPause;
    /**
     * To temporarily store missing discovery data which was paused during server pausing
     */
    private ConcurrentLinkedQueue<Pair<String, String>> mBufferedDiscoveryData;
    private volatile boolean mIsDirectDiscoveryPause;
    private volatile boolean mIsAdhocDiscoveryPause;

    /**
     * To temporarily store missing discovery data which was paused during server pausing
     */

    private MeshHttpServer() {
        callableExecutor = Executors.newFixedThreadPool(1);
        context = App.getContext();
        mBufferedDiscoveryData = new ConcurrentLinkedQueue<>();
    }

    public static MeshHttpServer on() {
        if (server == null) {
            server = new MeshHttpServer();
        }

        return server;
    }

    public void start(MessageListener messageListener, int appPort) {
        stop();
        this.messageListener = messageListener;
        this.APP_PORT = appPort;

        try {
            nanoHTTPServer = new NanoHTTPServer(appPort);
            nanoHTTPServer.setHttpDataListener(this::receivedData);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void setListener(MessageListener messageListener) {

        this.messageListener = messageListener;

    }

    public void stop() {
        if (nanoHTTPServer != null) {
            nanoHTTPServer.stop();
        }

    }

    private Network getNetwork() {
        return WiFiUtil.getConnectedWiFiNetwork(context);
    }
    //.url("http://" + ip + ":8080/hellopacket?data=" + dataStr)

    public Integer sendMessage(String ip, byte[] data) throws ExecutionException, InterruptedException {
        return sendMessage(ip, data, DEFAULT_CONNECTION_TIMEOUT);
    }

    public Integer sendMessage(String ip, byte[] data, int connectionTimeOutInMillis)
            throws ExecutionException, InterruptedException {
        String dataStr = new String(data);//android.util.Base64.encodeToString(data, Base64.DEFAULT);
        RequestBody formBody = new FormBody.Builder()
                .add("data", dataStr)
                .build();
        Future<Integer> future = callableExecutor.submit(() -> {
            OkHttpClient client;
            OkHttpClient.Builder builder = new OkHttpClient.Builder();

            builder.connectTimeout(connectionTimeOutInMillis, TimeUnit.MILLISECONDS);
            builder.retryOnConnectionFailure(true);
            client = builder.build();

            int responseCode = 0;
            Request request = new Request.Builder()
                    .url("http://" + ip + ":" + APP_PORT)
                    .post(formBody)
                    .addHeader("cache-control", "no-cache")
                    .addHeader("Connection", "close")
                    .build();

            try {

                Response response = client.newCall(request).execute();
                if (response != null) {
                    responseCode = response.code() == 200 ? 1 : 0;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return responseCode;
        });

        return future.get();
    }

    public void parseReceivedData(JSONObject jo, String ipAddress) {}


    @Override
    public void receivedData(String ipAddress, String data) {
        processMessage(ipAddress, data);
    }

    private void processMessage(String ip, String data) {

        BaseMessage baseMessage = BaseMessage.toBaseMessage(data);

        if(baseMessage instanceof HelloMessage) {
            if(messageListener != null) {
                messageListener.onMessage(((HelloMessage) baseMessage).hello);
            }

            if(!Constant.MASTER_IP_ADDRESS.equals(ip) && P2PUtil.isMeGO()) {

                HelloMessage helloMessage = new HelloMessage(null);
                helloMessage.hello = "Hello from:"+ Constant.MASTER_IP_ADDRESS;

                try {
                    sendMessage(ip, helloMessage.toJson().getBytes());
                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Stop accepting any discovery messages
     */
    public void pauseDirectDiscovery() {
        mIsDirectDiscoveryPause = true;
    }

    public void resumeDirectDiscovery() {
        mIsDirectDiscoveryPause = false;
        processMissingDiscoveryMessage();
    }

    /**
     * Stop accepting any discovery messages
     */
    public void pauseAdhocDiscovery() {
        mIsAdhocDiscoveryPause = true;
    }

    public void resumeAdhocDiscovery() {
        mIsAdhocDiscoveryPause = false;
        processMissingDiscoveryMessage();
    }

    /**
     * During pause of Server, we might receive sme discovery data. We process it upon resume of
     * server
     */
    private synchronized void processMissingDiscoveryMessage() {
        if (mBufferedDiscoveryData != null && mBufferedDiscoveryData.size() > 0) {
            Pair<String, String> ipData;
            do {
                ipData = mBufferedDiscoveryData.remove();
                if (ipData != null) {
                    processMessage(ipData.first, ipData.second);
                }
            } while (!mBufferedDiscoveryData.isEmpty());
        }
    }
}
