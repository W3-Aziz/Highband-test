package com.w3engineers.highbandtest.protocol.wifi.libmeshx.wifid;

import androidx.annotation.NonNull;

import java.util.Objects;

/**
 * Provides P2P configuration information
 */
public class WiFiMeshConfig {

    public boolean mIsGroupOwner, mIsClient;


    /**
     * Configure whether our system should be greedy to connect to desired network.
     */
    public boolean mIsForceFulReconnectionAllowed = true;

    @NonNull
    @Override
    public String toString() {
        return "mIsGroupOwner:"+mIsGroupOwner+"-mIsClient:"+mIsClient;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WiFiMeshConfig that = (WiFiMeshConfig) o;
        return mIsGroupOwner == that.mIsGroupOwner &&
                mIsClient == that.mIsClient;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mIsGroupOwner, mIsClient);
    }
}
