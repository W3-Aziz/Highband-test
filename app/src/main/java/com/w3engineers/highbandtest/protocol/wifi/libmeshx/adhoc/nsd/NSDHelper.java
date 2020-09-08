package com.w3engineers.highbandtest.protocol.wifi.libmeshx.adhoc.nsd;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.text.TextUtils;
import android.util.Log;

import com.w3engineers.highbandtest.protocol.util.AddressUtil;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper for NSD
 */
public class NSDHelper {

    private static Object lock = new Object();
    private static NSDHelper sNSDHelper;
    private NSDListener mNSDListener;

    private NsdManager mNsdManager;
    private NsdManager.DiscoveryListener mDiscoveryListener;
    private NsdManager.RegistrationListener mRegistrationListener;
    private volatile String myIP;

    private final String SERVICE_TYPE = "_www._tcp.";
    private final String SERVICE_NAME = "cko";
    /**
     * this name can be changed if same name already exist in the network
     */
    private String mServiceName = SERVICE_NAME;

    private List<NsdServiceInfo> mNsdServiceInfoList;

    public static synchronized NSDHelper getInstance(Context context) {
        if (sNSDHelper == null) {
            synchronized (lock) {
                if (sNSDHelper == null) {
                    sNSDHelper = new NSDHelper(context);
                }
            }
        }

        return sNSDHelper;
    }

    private NSDHelper(Context context) {
        mNsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
        mNsdServiceInfoList = new ArrayList<>(3);
    }

    public void setNSDListener(NSDListener nsdListener) {
        mNSDListener = nsdListener;
    }

    public void initializeNsd() {
        initializeDiscoveryListener();
        initializeRegistrationListener();

    }

    public void initializeDiscoveryListener() {
        mDiscoveryListener = new NsdManager.DiscoveryListener() {

            @Override
            public void onDiscoveryStarted(String regType) {
                Log.v(getClass().getSimpleName(), "[NSD]Service discovery started");
            }

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                Log.v(getClass().getSimpleName(),"[NSD]Service discovery success:" + service);

                //Not our service
                if (!service.getServiceType().equals(SERVICE_TYPE)) {
                    Log.v(getClass().getSimpleName(),"[NSD]Unknown Service Type: " + service.getServiceType());
                } else if (service.getServiceName().equals(mServiceName)) {
                    //Same machine, same service
                    Log.v(getClass().getSimpleName(),"[NSD]Same machine:" + mServiceName);
                } else if (service.getServiceName().startsWith(SERVICE_NAME)) {
                    //If same service name in same network the NSD rename that automatically
                    //https://developer.android.com/training/connect-devices-wirelessly/nsd#register
                    mNsdManager.resolveService(service, buildResolveListener());
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo service) {
                Log.v(getClass().getSimpleName(),"[NSD]onServiceLost:" + service.toString());
                if (mNsdServiceInfoList != null && mNsdServiceInfoList.size() > 0) {
                    if (mNsdServiceInfoList.remove(service)) {

                        if (mNSDListener != null) {
                            InetAddress inetAddress = service.getHost();
                            String ip = AddressUtil.getIpAddress(inetAddress);
                            if (AddressUtil.isValidIPAddress(ip)) {
                                mNSDListener.onNodeGone(ip);
                            }
                        }
                    }
                }
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.v(getClass().getSimpleName(),"[NSD]Discovery stopped:" + serviceType);
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.v(getClass().getSimpleName(),"[NSD]Discovery failed: Error code:" + errorCode);
                mNsdManager.stopServiceDiscovery(this);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.v(getClass().getSimpleName(),"[NSD]Discovery failed: Error code:" + errorCode);
                mNsdManager.stopServiceDiscovery(this);
            }
        };
    }

    public NsdManager.ResolveListener buildResolveListener() {
        return new NsdManager.ResolveListener() {

            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.v(getClass().getSimpleName(),"[NSD]Resolve failed:" + errorCode);
            }

            @Override
            public synchronized void onServiceResolved(NsdServiceInfo serviceInfo) {
                Log.v(getClass().getSimpleName(),"[NSD]onServiceResolved:" + serviceInfo);

                if (serviceInfo.getServiceName().equals(mServiceName)) {
                    Log.v(getClass().getSimpleName(),"[NSD]Own Service !!!");
                    return;
                }

                if (!TextUtils.isEmpty(myIP) && myIP.equals(serviceInfo.getHost().getHostAddress())) {
                    Log.v(getClass().getSimpleName(),"[NSD] IP Conflicting in service Info !!!");
                    return;
                }

                mNsdServiceInfoList.add(serviceInfo);

                if (mNSDListener != null) {
                    InetAddress inetAddress = serviceInfo.getHost();
                    String ip = AddressUtil.getIpAddress(inetAddress);
                    Log.v(getClass().getSimpleName(),"[NSD] Host IP::" + serviceInfo.getHost().getHostAddress());
                    if (AddressUtil.isValidIPAddress(ip)) {
                        //tearDown();
                        //stopDiscovery();
                        mNSDListener.onNodeAvailable(ip, serviceInfo.getPort(), serviceInfo.getServiceName());
                    }
                }
            }
        };
    }

    public void initializeRegistrationListener() {
        mRegistrationListener = new NsdManager.RegistrationListener() {

            @Override
            public void onServiceRegistered(NsdServiceInfo NsdServiceInfo) {
                mServiceName = NsdServiceInfo.getServiceName();
            }

            @Override
            public void onRegistrationFailed(NsdServiceInfo arg0, int arg1) {
                Log.v(getClass().getSimpleName(),"[NSD]onRegistrationFailed");
            }

            @Override
            public void onServiceUnregistered(NsdServiceInfo arg0) {
                Log.v(getClass().getSimpleName(),"[NSD]onServiceUnregistered");
            }

            @Override
            public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.v(getClass().getSimpleName(),"[NSD]onUnregistrationFailed");
            }

        };
    }

    /**
     * Registers self to be discovered by others.
     *
     * @param name Should be as minimum as possible. Appends with {@link #SERVICE_NAME}.
     * @param port dynamic port association
     */
    public void registerService(String name, int port, InetAddress inetAddress) {
        NsdServiceInfo serviceInfo = new NsdServiceInfo();
        serviceInfo.setPort(port);
        serviceInfo.setServiceName(TextUtils.isEmpty(name) ? SERVICE_NAME : SERVICE_NAME + name);
        serviceInfo.setServiceType(SERVICE_TYPE);

        if (!TextUtils.isEmpty(inetAddress.getHostAddress())) {
            Log.v(getClass().getSimpleName(),"[NSD] OWN IP address" + inetAddress.getHostAddress());
            serviceInfo.setHost(inetAddress);
            myIP = inetAddress.getHostAddress();
        }
        mNsdManager.registerService(
                serviceInfo, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener);

    }

    public void discoverServices() {
        mNsdManager.discoverServices(
                SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
    }

    public void stopDiscovery() {
        Log.v(getClass().getSimpleName(),"[NSD]stopDiscovery");
        if (mDiscoveryListener != null) {
            try {
                mNsdManager.stopServiceDiscovery(mDiscoveryListener);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
    }

    public void tearDown() {
        if (mRegistrationListener != null) {
            Log.v(getClass().getSimpleName(),"[NSD]Stop Broadcasting");
            mNsdManager.unregisterService(mRegistrationListener);
        }
    }
}
