package com.w3engineers.mesh.libmeshx.discovery;



import android.net.wifi.p2p.WifiP2pDevice;

import java.util.Collection;

/**
 * Contains method to communicate at App layer
 */
public interface MeshXAPListener {

    void onSoftAPStateChanged(boolean isEnabled, String Ssid, String passPhrase);

    void onGOConnectedWith(Collection<WifiP2pDevice> wifiP2pDevices);

    void onGODisconnectedWith(String ip);
}
