package com.w3engineers.highbandtest.protocol.model;

public class HelloMessage extends BaseMessage{
    public String hello = "Hello";
    public String bleName;

    public HelloMessage(String bleName) {
        this.bleName = bleName;
    }
}
