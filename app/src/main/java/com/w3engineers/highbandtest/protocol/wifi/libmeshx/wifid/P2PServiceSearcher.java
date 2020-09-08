package com.w3engineers.highbandtest.protocol.wifi.libmeshx.wifid;

import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.Handler;

import com.w3engineers.highbandtest.ProtocolManager;

import static android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION;
import static android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION;
import static android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION;
import static android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION;

/**
 * Discovers peers and provided service type. If found then call {@link #onDesiredServiceFound(String, String, WifiP2pDevice)}
 */
public abstract class P2PServiceSearcher implements WifiP2pManager.ChannelListener {

    // FIXME: 10/2/2019 Sync approach decrease the possibility of searching service for long run,
    //  will add dynamic time based logic with {@link P2PStateListener#onP2PPeersStateChange}
    private enum ServiceState {
        NONE,
        RequestingDiscoverPeer,
        DiscoverPeer,
        RequestingDiscoverService,
        DiscoverService
    }

    protected volatile boolean mIsAlive = true;
    private Context mContext;
    private WifiP2pManager mWifiP2pManager;
    private WifiP2pManager.Channel mChannel;
    private PeerReceiver mPeerReceiver;
    private WifiP2pManager.PeerListListener mPeerListListener;
    private WifiP2pManager.DnsSdServiceResponseListener mDnsSdServiceResponseListener;
    private volatile ServiceState mServiceState = ServiceState.NONE;
    protected volatile boolean mIsPauseConnectivity;
    private volatile boolean mIsServiceFound;
    /**
     * set type of service we are looking for
     */
    String mServiceType = ProtocolManager.SERVICE_TYPE;

    public P2PServiceSearcher(Context context) {
        mContext = context;

        mWifiP2pManager = (WifiP2pManager) mContext.getSystemService(Context.WIFI_P2P_SERVICE);

        mChannel = mWifiP2pManager.initialize(mContext, mContext.getMainLooper(), this);

        mPeerReceiver = new PeerReceiver(mP2PStateListener);
        IntentFilter filter = new IntentFilter();
        filter.addAction(WIFI_P2P_STATE_CHANGED_ACTION);
        filter.addAction(WIFI_P2P_CONNECTION_CHANGED_ACTION);
        filter.addAction(WIFI_P2P_DISCOVERY_CHANGED_ACTION);
        filter.addAction(WIFI_P2P_PEERS_CHANGED_ACTION);
        mContext.registerReceiver(mPeerReceiver, filter);

        mPeerListListener = new WifiP2pManager.PeerListListener() {
            @Override
            public void onPeersAvailable(WifiP2pDeviceList peers) {

                final WifiP2pDeviceList pers = peers;
                int numm = 0;
                for (WifiP2pDevice peer : pers.getDeviceList()) {
                    numm++;
                }

                if (numm > 0) {
                    startServiceDiscovery();
                } else {
                    startPeerDiscovery();
                }
            }
        };

        mDnsSdServiceResponseListener = new WifiP2pManager.DnsSdServiceResponseListener() {

            public void onDnsSdServiceAvailable(String instanceName, String serviceType, WifiP2pDevice device) {

                if (serviceType.startsWith(mServiceType)) {


                    String[] separated = instanceName.split(":");
                    if (separated.length > 1) {
                        final String networkSSID = separated[0];
                        final String networkPass = separated[1];
                        final String serviceAdvertisersBtName = separated.length > 2 ? separated[2] : null;

                        //networkname available in existing ndde's SSID
//                        boolean isNotToAcceptService = ConnectionLinkCache.getInstance().
//                                isSsidNameExistInConnectedSet(networkSSID);
                        boolean isNotToAcceptService = false;
                        if(!isNotToAcceptService) {//Check further if false
                            isNotToAcceptService = false;
//                            isNotToAcceptService = ConnectionLinkCache.getInstance().
//                                    isBtNameExistInConnectedSet(serviceAdvertisersBtName);
                        }
                        if (isNotToAcceptService) {
                        } else {

                            if (mIsPauseConnectivity) {
                            } else {
                                if (!mIsServiceFound){
                                    mIsServiceFound = true;
                                    onDesiredServiceFound(networkSSID, networkPass, device);

                                } else {
                                }
                            }
                        }
                    }

                } else {
                }

                startPeerDiscovery();
            }
        };
    }

    protected abstract void onP2pAlreadyConnected(String ssid);

    protected abstract void onDesiredServiceFound(String ssid, String passPhrase, WifiP2pDevice wifiP2pDevice);

    public boolean start() {
        if (mWifiP2pManager == null) {
            return false;
        }

        mIsServiceFound = false;

        mWifiP2pManager.setDnsSdResponseListeners(mChannel, mDnsSdServiceResponseListener, null);

        mWifiP2pManager.stopPeerDiscovery(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                updateServiceStateWithoutCondition(ServiceState.NONE);
                startPeerDiscovery();
            }

            @Override
            public void onFailure(int reason) {
            }
        });

        return true;
    }

    public void restart() {
        mWifiP2pManager.setDnsSdResponseListeners(mChannel, null, null);
        start();
    }

    private synchronized void startServiceDiscovery() {

        if(mServiceState == ServiceState.DiscoverPeer) {
            updateServiceState(ServiceState.RequestingDiscoverService);
            mWifiP2pManager.clearServiceRequests(mChannel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    WifiP2pDnsSdServiceRequest request = WifiP2pDnsSdServiceRequest.newInstance(
                            ProtocolManager.SERVICE_TYPE);
                    final Handler handler = new Handler();
                    mWifiP2pManager.addServiceRequest(mChannel, request, new WifiP2pManager.ActionListener() {

                        public void onSuccess() {
                            handler.postDelayed(new Runnable() {
                                //There are supposedly a possible race-condition bug with the service discovery
                                // thus to avoid it, we are delaying the service discovery start here
                                public void run() {
                                    mWifiP2pManager.discoverServices(mChannel, new WifiP2pManager.ActionListener() {
                                        public void onSuccess() {
                                            updateServiceState(ServiceState.DiscoverService);
                                            mServiceState = ServiceState.DiscoverService;
                                        }

                                        public void onFailure(int reason) {
                                            updateServiceStateWithoutCondition(ServiceState.DiscoverPeer);
                                        }
                                    });
                                }
                            }, 1000);
                        }

                        public void onFailure(int reason) {
                            updateServiceState(ServiceState.DiscoverPeer);
                            // No point starting service discovery
                        }
                    });
                }

                @Override
                public void onFailure(int reason) {
                    updateServiceState(ServiceState.DiscoverPeer);

                }
            });
        }
    }

    private synchronized void startPeerDiscovery() {
        if(mServiceState == ServiceState.NONE) {
            updateServiceState(ServiceState.RequestingDiscoverPeer);
            mWifiP2pManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
                public void onSuccess() {
                    updateServiceState(ServiceState.DiscoverPeer);
                }

                public void onFailure(int reason) {
                    updateServiceStateWithoutCondition(ServiceState.NONE);
                }
            });
        }
    }

    private void stopPeerDiscovery() {
//                public static final int ERROR               = 0;
//                public static final int P2P_UNSUPPORTED     = 1;
//                public static final int BUSY                = 2;
//                public static final int NO_SERVICE_REQUESTS = 3;
        // TODO: 8/21/2019
        //Check:
        // 1. whether it fails for blank peerDiscovery stop.
        // 2. Is there any way to detect peer disoovery running or not?
        mWifiP2pManager.stopPeerDiscovery(mChannel, new WifiP2pManager.ActionListener() {
            public void onSuccess() {
                updateServiceStateWithoutCondition(ServiceState.NONE);
            }

            public void onFailure(int reason) {
            }
        });
    }

    // TODO: 8/21/2019
    // 1. Does it fail if no service request present?
    // 2. Is there any way to check service discovery running or not?
    private void stopServiceDiscovery() {
        mWifiP2pManager.clearServiceRequests(mChannel, new WifiP2pManager.ActionListener() {
            public void onSuccess() {
                updateServiceStateWithoutCondition(ServiceState.DiscoverPeer);
            }

            public void onFailure(int reason) {
            }
        });
    }


    public void stop() {
        mIsAlive = false;
        mWifiP2pManager.setDnsSdResponseListeners(mChannel, null, null);
        stopServiceDiscovery();
        stopPeerDiscovery();
        try {
            mContext.unregisterReceiver(mPeerReceiver);
        } catch (IllegalArgumentException ex) {
//            ex.printStackTrace();
        }
    }


    private P2PStateListener mP2PStateListener = new P2PStateListener() {
        @Override
        public void onP2PStateChange(int state) {

        }

        @Override
        public void onP2PPeersStateChange() {
//            MeshLog.i("[WDC]onP2PPeersStateChange");
            //We do not want any new peer request while service discovery alive
            // TODO: 10/2/2019 except if it is too long that we have not discovered any GO
            if (mServiceState.ordinal() < ServiceState.RequestingDiscoverService.ordinal()) {
                mWifiP2pManager.requestPeers(mChannel, mPeerListListener);
            }
        }

        @Override
        public void onP2PConnectionChanged() {
            startPeerDiscovery();
        }

        @Override
        public void onP2PDisconnected() {
            startPeerDiscovery();
        }

        @Override
        public void onP2PPeersDiscoveryStarted() {

        }

        @Override
        public void onP2PPeersDiscoveryStopped() {
            startPeerDiscovery();
        }
    };

    @Override
    public void onChannelDisconnected() {

    }

    /**
     * Update {@link #mServiceState} with serviceState iff new state is greater than earlier state
     * @param serviceState
     */
    private void updateServiceState(ServiceState serviceState) {
        if(ServiceState.valueOf(serviceState.name()).ordinal() >
                ServiceState.valueOf(mServiceState.name()).ordinal()) {
            mServiceState = serviceState;
        }
    }

    private void updateServiceStateWithoutCondition(ServiceState serviceState) {
        mServiceState = serviceState;
    }

    protected void pauseConnectivity() {
        mIsPauseConnectivity = true;
    }

    protected void resumeConnectivity() {
        mIsPauseConnectivity = false;
    }
}