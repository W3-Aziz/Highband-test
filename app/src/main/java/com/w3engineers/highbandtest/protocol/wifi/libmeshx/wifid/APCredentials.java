package com.w3engineers.highbandtest.protocol.wifi.libmeshx.wifid;

public class APCredentials {

    public String mSSID, mPassPhrase;

    public APCredentials(String SSID, String passPhrase) {
        this.mSSID = SSID;
        this.mPassPhrase = passPhrase;
    }

    @Override
    public String toString() {
        return "APCredentials{" +
                "mSSID='" + mSSID + '\'' +
                ", mPassPhrase='" + mPassPhrase + '\'' +
                '}';
    }
}
