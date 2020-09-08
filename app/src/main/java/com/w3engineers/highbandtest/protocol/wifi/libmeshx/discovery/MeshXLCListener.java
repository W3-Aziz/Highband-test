package com.w3engineers.mesh.libmeshx.discovery;

/**
 * Contains method to communicate at App layer
 */
public interface MeshXLCListener {

    /**
     * When a new Peer receive
     *
     */
    void onConnectWithGO(String ssid);

    /**
     * Called when current device is GO, still somehow connects with a GO
     */
    void onConnectWithGOBeingGO(boolean wasDisconnected);

    void onConnectWithAdhocPeer(String ip, int port);

    /**
     * on remove a peer
     */
    void onDisconnectWithGO(String disconnectedFrom);

    void onDisconnectedWithAdhoc(String address);
}
