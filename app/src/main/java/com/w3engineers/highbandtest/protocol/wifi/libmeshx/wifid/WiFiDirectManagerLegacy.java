package com.w3engineers.highbandtest.protocol.wifi.libmeshx.wifid;


import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.util.Log;

import com.w3engineers.highbandtest.protocol.model.Credential;
import com.w3engineers.highbandtest.protocol.util.AndroidUtil;
import com.w3engineers.highbandtest.protocol.util.P2PUtil;
import com.w3engineers.highbandtest.protocol.util.WiFiUtil;
import com.w3engineers.highbandtest.protocol.wifi.libmeshx.wifi.WiFiClient;
import com.w3engineers.highbandtest.protocol.wifi.libmeshx.wifi.WiFiConnectionHelper;
import com.w3engineers.highbandtest.util.MeshLog;
import com.w3engineers.mesh.libmeshx.discovery.MeshXAPListener;
import com.w3engineers.mesh.libmeshx.discovery.MeshXLCListener;
import com.w3engineers.mesh.libmeshx.discovery.MeshXLogListener;
import com.w3engineers.mesh.libmeshx.wifi.WiFiStateMonitor;

import java.util.Collection;

/**
 * For all WiFi direct or WiFiP2P related tasks. Support Legacy Client
 * connectivity
 */
public class WiFiDirectManagerLegacy {

//    private final long WIFI_OFF_DECIDER_DELAY = 1500;
    private final long WIFI_OFF_DECIDER_DELAY = 0;
    private final int START_TASK_ONLY_AP = 1;
    private final int START_TASK_ONLY_SEARCH = 2;
    private final int START_TASK_ALL = 3;
    private final long SOFT_DELAY_TO_START_P2P_SERVICES = 1000;
    public static final short MAX_SSID_CONNECTION_ATTEMPT = 3;

    private WiFiStateMonitor mWiFiStateMonitor;
    public transient WiFiMeshConfig mWiFiMeshConfig;
    private Context mContext;
    private WiFiConnectionHelper mWiFiConnectionHelper;
    private volatile static WiFiDirectManagerLegacy sWiFiDirectManagerLegacy;
    private SoftAccessPoint mSoftAccessPoint;
    private SoftAccessPointSearcher mSoftAccessPointSearcher;
    private WiFiClient mWiFiClient;
    private MeshXLogListener mMeshXLogListener;
    private MeshXAPListener mMeshXAPListener;
    private MeshXLCListener mMeshXLCListener;
    private String mNetworkName;
    private WiFiStateMonitor.WiFiAdapterStateListener mWiFiAdapterStateListener;
    public boolean mHasForcedOff;
    private short mSSIDConnectionAttempt;
    private APCredentials mAPCredentials;
    public volatile boolean mIsWaitingForConnection;

    /**
     * Maintain temporary state of connecting ssid. So that we can track whether system is in
     * connecting state or not. e.g: saved network connection. Take no decision at this moment
     * except a read only value
     */
    private volatile String mConnectingSSID;
    private volatile boolean mIsConnectivityPause;

    private SoftAccessPoint.SoftAPStateListener mSoftAPStateListener = new SoftAccessPoint.SoftAPStateListener() {
        @Override
        public void onSoftAPChanged(boolean isEnabled, String ssidName, String passPhrase) {
            if (ssidName != null && passPhrase != null) {
                mNetworkName = ssidName;
                mAPCredentials = new APCredentials(ssidName, passPhrase);
                MeshLog.v("[SoftAP]Credentials generated:"+mAPCredentials);
            }
            if (mMeshXAPListener != null) {
                mMeshXAPListener.onSoftAPStateChanged(isEnabled, ssidName, passPhrase);
            }
        }

        @Override
        public void onSoftApConnectedWithNodes(Collection<WifiP2pDevice> wifiP2pDevices) {
            if (mMeshXAPListener != null) {
                mMeshXAPListener.onGOConnectedWith(wifiP2pDevices);
            }

            if (mSoftAccessPointSearcher != null) {
                mSoftAccessPointSearcher.stop();
            }
        }

        @Override
        public void onSoftApDisConnectedWithNodes(String ip) {
            if (mMeshXAPListener != null) {

                mMeshXAPListener.onGODisconnectedWith(ip);

            }
        }
    };

    private SoftAccessPointSearcher.ServiceFound mServiceFound = new SoftAccessPointSearcher.ServiceFound() {
        @Override
        public void onServiceFoundSuccess(String ssid, String passPhrase, WifiP2pDevice p2pDevice) {
            if (mSoftAccessPointSearcher != null && !mSoftAccessPointSearcher.mIsPauseConnectivity
                    && P2PUtil.isPotentialGO(ssid)) {
                mConnectingSSID = ssid;

                if (mMeshXLogListener != null) {
                    mMeshXLogListener.onLog("[FOUND] SSID - " + ssid + "::Passphrase - " + passPhrase);
                }

                if (mSoftAccessPoint != null) {
                    mSoftAccessPoint.Stop();
                    mSoftAccessPoint = null;
                }

                mSoftAccessPointSearcher.stop();
                mSoftAccessPointSearcher = null;

                mWiFiClient.connect(ssid, passPhrase, mConnectionListener);

                if (mMeshXLogListener != null) {

                    StringBuilder devices = new StringBuilder();
                    //Building device details log
                    if (mSoftAccessPoint != null) {
                        Collection<WifiP2pDevice> wifiP2pDevices = mSoftAccessPoint.getConnectedPeers();
                        for (WifiP2pDevice wifiP2pDevice : wifiP2pDevices) {
                            devices.append(wifiP2pDevice.deviceName).append("-").
                                    append(wifiP2pDevice.deviceAddress).append(",");
                        }
                        //removing last comma
                        int index = devices.lastIndexOf(",");
                        if (index != -1) {
                            devices.replace(index, index + 1, "");
                        }
                    }

                    mMeshXLogListener.onLog("[NOT-CONNECTING] " + (mSoftAccessPoint == null ?
                            "null" : ("count::" + mSoftAccessPoint.getConnectedPeersCount() + "::" + devices)));
                }
            }
        }

        @Override
        public void onP2pAlreadyConnected(String ssid) {
            if (mMeshXLCListener != null ) {
                mMeshXLCListener.onConnectWithGO(ssid);
            }
        }
    };

    private WiFiClient.ConneectionListener mConnectionListener = new WiFiClient.ConneectionListener() {

        /**
         * We are connected. Check whether we are connected with any GO. If not then we disconnect
         * and in turns re attemp for connection
         * @param wifiConnectionInfo
         * @param passPhrase
         */
        @Override
        public void onConnected(final WifiInfo wifiConnectionInfo, String passPhrase) {

            String ssidName = wifiConnectionInfo.getSSID();
            boolean isPotentialGO = P2PUtil.isPotentialGO(ssidName);
            mAPCredentials = new APCredentials(ssidName, passPhrase);

            if (mWiFiMeshConfig.mIsGroupOwner && isPotentialGO && mSoftAccessPoint != null &&
                    mSoftAccessPoint.isGoAlive()) {
                //If me is a GO then we do not allow any connection with any other GO

                //We sent an interruption to app layer.
                mMeshXLCListener.onConnectWithGOBeingGO(mWiFiClient.disConnect());

            } else if (mWiFiMeshConfig.mIsClient) {//Works iff me contains a client mode


                if (mSoftAccessPointSearcher != null) {
                    mSoftAccessPointSearcher.stop();
                }


                mNetworkName = ssidName;

                if (isPotentialGO) {
                    if (WiFiUtil.isSameSSID(mConnectingSSID, ssidName)) {
                        if (mMeshXLCListener != null) {
                            mMeshXLCListener.onConnectWithGO(wifiConnectionInfo.getSSID());
                        } else {
                        }

                    } else {
                        //Unexpected P2P network
                        //Disconnect from the network and restart required services
                        if (mWiFiClient.isConnected()) {
                            mWiFiConnectionHelper.removeNetwork(wifiConnectionInfo.getNetworkId());
                            mWiFiClient.disConnect();
                        }
                    }
                } else {//Adhoc
              /*  MeshLog.w("[MeshX] Adhoc connectivity:" + ssidName);

                NSDHelper nsdHelper = NSDHelper.getInstance(mContext);
                nsdHelper.initializeNsd();
                nsdHelper.setNSDListener(new NSDListener() {
                    @Override
                    public void onNodeAvailable(String ip, int port, String name) {
                        MeshLog.i("[NSD] service found on:" + ip + ":" + port + ":" + name);
                        mMeshXLCListener.onConnectWithAdhocPeer(ip, port);
                    }

                    @Override
                    public void onNodeGone(String ip) {
                        MeshLog.i("[NSD] service gone:" + ip);
                        mMeshXLCListener.onDisconnectedWithAdhoc(ip);
                    }
                });

                //nsdHelper.stopDiscovery();
                //nsdHelper.tearDown();

                InetAddress inetAddress = WifiDetector.determineAddress(mContext);
                nsdHelper.registerService(mWiFiMeshConfig == null ? null :
                        mWiFiMeshConfig.mServiceName, TransportManagerX.APP_PORT, inetAddress);

                nsdHelper.discoverServices();*/
                }

                mConnectingSSID = null;
            }
        }

        @Override
        public void onTimeOut() {
            mConnectingSSID = null;
            if (mWiFiMeshConfig.mIsClient) {

                if (mWiFiClient != null && !mWiFiClient.isConnected()) {

                    if (mMeshXLogListener != null) {
                        mMeshXLogListener.onLog("[OnTimeOut]");
                    }

                    if (mWiFiMeshConfig != null && mWiFiMeshConfig.mIsClient) {
                        reAttemptServiceDiscovery();
                    }
                }
            } else if(mWiFiMeshConfig.mIsGroupOwner && !mSoftAccessPoint.isGoAlive()) {

                mSoftAccessPoint.start();

            }
        }

        /**
         * We are connected so enabling GO and Service searcher
         */
        @Override
        public void onDisconnected() {

            mConnectingSSID = null;
            if (mWiFiMeshConfig.mIsClient) {
                if (mMeshXLCListener != null) {
                    mMeshXLCListener.onDisconnectWithGO(mNetworkName);
                }

                if (mMeshXLogListener != null) {
                    mMeshXLogListener.onLog("[onDisconnected]");
                }

                if (mWiFiStateMonitor != null && mWiFiClient != null && mWiFiClient.isWiFiOn()) {
                    //Due to forceful reset of WiFi we do not want to have false alarm by state monitor
                    mWiFiStateMonitor.destroy();
                }
                reAttemptServiceDiscovery();

            } else if(mWiFiMeshConfig.mIsGroupOwner && !mSoftAccessPoint.isGoAlive()) {

                mSoftAccessPoint.start();

            }
        }
    };

    /**
     * It considers client connection state of GO part. Make sure we are not re initiating GO
     * while it has any valid client connected
     */
    public void reAttemptServiceDiscovery() {
        if (mMeshXLogListener != null) {
            mMeshXLogListener.onLog("[reAttemptServiceDiscovery]");
        }


        if (mWiFiStateMonitor != null) {

            mWiFiStateMonitor.destroy();
            mWiFiStateMonitor = null;
        }
        AndroidUtil.postDelay(this::start, SOFT_DELAY_TO_START_P2P_SERVICES);
    }

    public synchronized static WiFiDirectManagerLegacy getInstance(Context context,
                                                                   MeshXAPListener meshXAPListener,
                                                                   MeshXLCListener meshXLCListener,
                                                                   WiFiMeshConfig wiFiMeshConfig) {
        if (sWiFiDirectManagerLegacy == null) {
            synchronized (WiFiDirectManagerLegacy.class) {
                if (sWiFiDirectManagerLegacy == null) {
                    sWiFiDirectManagerLegacy = new WiFiDirectManagerLegacy(context, meshXAPListener,
                            meshXLCListener, wiFiMeshConfig);
                }
            }
        }
        return sWiFiDirectManagerLegacy;
    }

    /**
     * You must ensure to call {@link #getInstance(Context, MeshXAPListener, MeshXLCListener, WiFiMeshConfig)} before this method. Otherwise it will
     * return null
     *
     * @return
     */
    public static WiFiDirectManagerLegacy getInstance() {
        return sWiFiDirectManagerLegacy;
    }

    private WiFiDirectManagerLegacy(Context context, MeshXAPListener meshXAPListener,
                                   MeshXLCListener meshXLCListener, WiFiMeshConfig wiFiMeshConfig) {

        mWiFiMeshConfig = wiFiMeshConfig;
        mContext = context;
        mWiFiConnectionHelper = new WiFiConnectionHelper(mContext);
        mMeshXAPListener = meshXAPListener;
        this.mMeshXLCListener = meshXLCListener;
    }

    public void start() {

        start(null);

    }

    public void start(Context context) {
        if (mContext == null && context != null) {
            mContext = context;
        }

        mConnectingSSID = null;
        //make sure the instance is alive
        if (sWiFiDirectManagerLegacy != null && !mHasForcedOff) {

            // TODO: 11/20/2019 We should only clear service rather restarting WiFi client
            /*HardwareStateManager hardwareStateManager = new HardwareStateManager();
            hardwareStateManager.init(mContext);
            mIsRequestedForceOff = true;
            MeshLog.v("[WIFI]Resetting WiFi off");
            mIsRequestedForceOff = true;
            hardwareStateManager.resetWifi(isEnable -> {
                if (isEnable)*/
            {

                if (mWiFiStateMonitor == null) {
                    mWiFiStateMonitor = new WiFiStateMonitor(mContext, isEnabled -> {
                        if (!isEnabled) {//Somehow WiFi turned off
//                                mWiFiStateMonitor.destroy();//Would start receive upon reenable
//                                mWiFiStateMonitor = null;
//                                start(mContext);//reenabling WiFi

                            boolean isWiFiEnabled = WiFiUtil.isWiFiOn(mContext);
                            if(!isWiFiEnabled) {
                                mHasForcedOff = true;
                                //disabling wifi related portions
                                if (mWiFiAdapterStateListener != null) {
                                    mWiFiAdapterStateListener.onStateChanged(isEnabled);
                                }
                                stop();
                            }
                        }
                    });
                    mWiFiStateMonitor.init();
                }

//                    mWiFiConnectionHelper.disconnect();
                // FIXME: 11/19/2019 for testing purpose only
//                    mWiFiConnectionHelper.disableAllConfiguredWiFiNetworks();

                int task = -1;
                if (mWiFiMeshConfig.mIsClient && mWiFiMeshConfig.mIsGroupOwner) {
                    task = START_TASK_ALL;

                } else if (mWiFiMeshConfig.mIsGroupOwner) {
                    task = START_TASK_ONLY_AP;

                } else if (mWiFiMeshConfig.mIsClient) {
                    task = START_TASK_ONLY_SEARCH;
                }

                start(task);
            }
//            });
        }
    }

    public void start(int startTask) {

        //Irrespective of GO or LC we monitor WiFi connectivity status
        if (mWiFiClient == null) {
            mWiFiClient = new WiFiClient(mContext);
        }
        //Always set the listener
        mWiFiClient.setConnectionListener(mConnectionListener);
        mWiFiClient.setMeshXLogListener(mMeshXLogListener);

        if (startTask == START_TASK_ONLY_AP || startTask == START_TASK_ALL) {
            if (mWiFiMeshConfig.mIsGroupOwner) {
                if (mSoftAccessPoint == null) {
                    mSoftAccessPoint = new SoftAccessPoint(mContext, mSoftAPStateListener);
                    mSoftAccessPoint.setMeshXLogListener(mMeshXLogListener);

                    mSoftAccessPoint.start();
                } else {
                    mSoftAccessPoint.restart();
                }
            }

            if (startTask == START_TASK_ONLY_AP) {
                if (mSoftAccessPointSearcher == null) {
                    mSoftAccessPointSearcher = new SoftAccessPointSearcher(mContext);
                }

                mSoftAccessPointSearcher.stop();
            }
        }

        if (startTask == START_TASK_ONLY_SEARCH || startTask == START_TASK_ALL) {
            if (mWiFiMeshConfig.mIsClient) {

                if (mSoftAccessPointSearcher == null) {
                    mSoftAccessPointSearcher = new SoftAccessPointSearcher(mContext);
                    mSoftAccessPointSearcher.setServiceFound(mServiceFound);

                    mSoftAccessPointSearcher.start();
                } else {
                    mSoftAccessPointSearcher.restart();
                }
            }

            if (startTask == START_TASK_ONLY_SEARCH) {
                if (mSoftAccessPoint == null) {
                    mSoftAccessPoint = new SoftAccessPoint(mContext, mSoftAPStateListener);
                }

                mSoftAccessPoint.Stop();
            }
        }
    }

    public boolean isMeMasterAlive() {
        return (mSoftAccessPoint != null && mSoftAccessPoint.isGoAlive());
    }

    /**
     * Return true iff trying to connect with a SSID
     *
     * @return
     */
    public boolean isConnecting() {
        return mIsWaitingForConnection || mConnectingSSID != null;
    }

    public void destroy() {
        sWiFiDirectManagerLegacy = null;
        stop();

    }

    private void stop() {

        mConnectingSSID = null;


        //FIXME: context null issue
        // mContext = null;

        //todo
        if (mMeshXAPListener != null) {
            mMeshXAPListener.onSoftAPStateChanged(false, null, null);
        }

        if (mMeshXLCListener != null && sWiFiDirectManagerLegacy != null) {
            if (mWiFiClient != null && mWiFiClient.isConnected()) {
                mMeshXLCListener.onDisconnectWithGO(mNetworkName);
            }
        }

        if (mSoftAccessPoint != null) {
            mSoftAccessPoint.Stop();
        }

        if (mSoftAccessPointSearcher != null) {
            mSoftAccessPointSearcher.stop();
        }

        if (mWiFiClient != null) {
            mWiFiClient.destroy();
        }

        if (mWiFiStateMonitor != null) {
            mWiFiStateMonitor.destroy();
        }

        mWiFiAdapterStateListener = null;

        //stopNsd();
    }

    public void setMeshXLogListener(MeshXLogListener meshXLogListener) {
        mMeshXLogListener = meshXLogListener;
    }

    public void toggleGO(boolean newState) {

        if (newState != mWiFiMeshConfig.mIsGroupOwner) {
            mWiFiMeshConfig.mIsGroupOwner = newState;
            if (newState) {
                start(START_TASK_ONLY_AP);
            } else {
                mSoftAccessPoint.Stop();
            }
        }
    }

    public void toggleLC(boolean newState) {

        if (newState != mWiFiMeshConfig.mIsClient) {
            mWiFiMeshConfig.mIsClient = newState;
            if (newState) {
                start(START_TASK_ONLY_SEARCH);
            } else {
                if (mSoftAccessPointSearcher != null) {
                    mSoftAccessPointSearcher.stop();
                }
            }
        }
    }

    /**
     * Currently we deal with single interface either GO or LC. So this method
     *
     * @return only active network name or null whether GO or LC
     */
    public String getCurrentNetworkName() {

        return mNetworkName;
    }

    public boolean pauseConnectivity() {
        boolean isPaused = false;
        if (!mIsConnectivityPause) {
            mIsConnectivityPause = true;
            if (mSoftAccessPoint != null && mSoftAccessPoint.isGoAlive()) {
                //First pause from socket
                //MeshHttpServer.on().pauseDirectDiscovery();
                isPaused = true;
            }

            if (mSoftAccessPointSearcher != null && mSoftAccessPointSearcher.mIsAlive) {
                mSoftAccessPointSearcher.pauseConnectivity();
                mConnectingSSID = null;
                //MeshHttpServer.on().pauseDirectDiscovery();

                isPaused = true;
            }
        }

        return isPaused;
    }

    public boolean resumeConnectivity() {
        boolean isResumed = false;

        if (mIsConnectivityPause) {
            if (mSoftAccessPoint != null && mSoftAccessPoint.isGoAlive()) {
                //Resume socket
                //MeshHttpServer.on().resumeDirectDiscovery();
                isResumed = true;
            }

            if (mSoftAccessPointSearcher != null && mSoftAccessPointSearcher.mIsAlive) {
                mSoftAccessPointSearcher.resumeConnectivity();
                //MeshHttpServer.on().resumeDirectDiscovery();
                isResumed = true;
            }
        }

        return isResumed;
    }

    /**
     * Request GO to broadcast service in a scheduled fashion
     */
    public void requestGOScheduledBroadCast() {
        if (mSoftAccessPoint != null) {
            mSoftAccessPoint.requestScheduledServiceBroadcast();
        }
    }

    public void requestGOBroadcastOff() {
        if (mSoftAccessPoint != null) {
            mSoftAccessPoint.shutDownServiceBroadcasting();
        }
    }

    public void setWiFiAdapterStateListener(WiFiStateMonitor.WiFiAdapterStateListener
                                                    wiFiAdapterStateListener) {
        mWiFiAdapterStateListener = wiFiAdapterStateListener;
    }

    /**
     * Attempt to connect with an AP if not connected with any GO or Adhoc. It tries maximum
     * {@link #MAX_SSID_CONNECTION_ATTEMPT} times if fails.
     * @param credential
     * @return whether connection attempt possible or not
     */
    public boolean connectWithAP(Credential credential) {
        if(WiFiUtil.isWifiConnected(mContext)) {
            return false;
        }
        if(mWiFiClient == null) {
            mWiFiClient = new WiFiClient(mContext);
        }

        if(mWiFiMeshConfig.mIsGroupOwner && mSoftAccessPoint != null) {
            if(WiFiUtil.isSameSSID(credential.ssid, mSoftAccessPoint.mNetworkName)) {
                //Was attempting to connect with self SSID
                Log.e("Highband-bt","High band credential same ssid");
                return false;
            } else {
                //Turn off self GO and attempt to connect
                mSoftAccessPoint.Stop();
                AndroidUtil.sleep(2000);
            }
        } else if(mWiFiMeshConfig.mIsClient && mSoftAccessPointSearcher != null) {
            mSoftAccessPointSearcher.stop();
            AndroidUtil.sleep(2000);
        }

        MeshLog.v("[highband]Connecting attempt with AP:"+credential);
        mConnectingSSID = credential.ssid;
        return mWiFiClient.connect(credential.ssid, credential.password, new WiFiClient.ConneectionListener() {
            @Override
            public void onConnected(WifiInfo wifiConnectionInfo, String passPhrase) {
                mSSIDConnectionAttempt = 0;
                mWiFiClient.setConnectionListener(mConnectionListener);
                mAPCredentials = new APCredentials(wifiConnectionInfo.getSSID(), passPhrase);

                /*if(P2PUtil.isPotentialGO(credential.ssid)) {
                    mConnectionListener.onConnected(wifiConnectionInfo, passPhrase);
                }*/
            }

            @Override
            public void onTimeOut() {
                if(++mSSIDConnectionAttempt < MAX_SSID_CONNECTION_ATTEMPT) {
                    connectWithAP(credential);
                } else {
                    mSSIDConnectionAttempt = 0;
                    mWiFiClient.setConnectionListener(mConnectionListener);

                    if(mWiFiMeshConfig.mIsGroupOwner) {
                        mSoftAccessPoint.start();

                    } else if(mWiFiMeshConfig.mIsClient && mSoftAccessPointSearcher != null) {

                        mSoftAccessPointSearcher.start();
                    }
                    mConnectingSSID = null;
                }
            }

            @Override
            public void onDisconnected() {
                mSSIDConnectionAttempt = 0;
                mWiFiClient.setConnectionListener(mConnectionListener);

                if(mWiFiMeshConfig.mIsGroupOwner) {
                    mSoftAccessPoint.Stop();

                } else if(mWiFiMeshConfig.mIsClient && mSoftAccessPointSearcher != null) {

                    mSoftAccessPointSearcher.start();
                }

                mConnectingSSID = null;
            }
        });
    }

    public void setHighBandMode() {
        if(mWiFiClient != null) {
            mWiFiClient.setHighBand();
        }
    }

    public void releaseHighBandMode() {
        if(mWiFiClient != null) {
            mWiFiClient.releaseHighBand();
        }
    }

    public APCredentials getAPCredentials() {
        return mAPCredentials;
    }

    @Override
    public String toString() {
        return "WiFiDirectManagerLegacy{" +
                "mWiFiMeshConfig=" + mWiFiMeshConfig +
                '}';
    }
}
