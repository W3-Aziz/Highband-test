package com.w3engineers.highbandtest.protocol.wifi.libmeshx.adhoc.nsd;

/**
 * NSD listener
 */
public interface NSDListener {

    void onNodeAvailable(String ip, int port, String name);
    void onNodeGone(String ip);

}
