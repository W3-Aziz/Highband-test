package com.w3engineers.highbandtest.protocol.model;

public class Credential extends BaseMessage{
    public String ssid;
    public String password;

    public Credential(String ssid, String password) {
        this.ssid = ssid;
        this.password = password;
    }

    @Override
    public String toString() {
        return "Credential{" +
                "ssid='" + ssid + '\'' +
                ", password='" + password + '\'' +
                '}';
    }
}
