package com.w3engineers.highbandtest;

import android.content.Context;

class ProtocolManager {
    private static ProtocolManager protocolManager;
    private Context mContext;
    private ProtocolManager(Context context){
        this.mContext = context;
    }

    public ProtocolManager on(Context context){
        if(protocolManager != null){
            protocolManager = new ProtocolManager(context);
        }
        return protocolManager;
    }
}
