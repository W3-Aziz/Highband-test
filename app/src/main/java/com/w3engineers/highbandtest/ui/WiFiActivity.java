package com.w3engineers.highbandtest.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.w3engineers.highbandtest.ProtocolManager;
import com.w3engineers.highbandtest.R;
import com.w3engineers.highbandtest.protocol.data.AppMessageListener;
import com.w3engineers.highbandtest.protocol.model.HelloMessage;
import com.w3engineers.highbandtest.protocol.util.P2PUtil;
import com.w3engineers.highbandtest.protocol.util.WiFiUtil;
import com.w3engineers.highbandtest.protocol.wifi.httpservices.MeshHttpServer;
import com.w3engineers.highbandtest.protocol.wifi.libmeshx.wifid.WiFiDirectManagerLegacy;
import com.w3engineers.highbandtest.protocol.wifi.libmeshx.wifid.WiFiMeshConfig;

import java.util.concurrent.ExecutionException;

public class WiFiActivity extends AppCompatActivity {

    WiFiDirectManagerLegacy mWiFiDirectManagerLegacy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wi_fi);

        mWiFiDirectManagerLegacy = WiFiDirectManagerLegacy.getInstance(getApplicationContext(),
                null, null, null);

        ProtocolManager protocolManager = ProtocolManager.on(getApplicationContext());
        MeshHttpServer.on().start(protocolManager, ProtocolManager.HTTP_PORT);

        protocolManager.mAppMessageListener = message -> {
            if(!TextUtils.isEmpty(message)) {
                runOnUiThread(() -> Toast.makeText(WiFiActivity.this, message,
                        Toast.LENGTH_SHORT).show() );
            }
        };
    }

    public void onGO(View view) {
        mWiFiDirectManagerLegacy.mWiFiMeshConfig = new WiFiMeshConfig();
        mWiFiDirectManagerLegacy.mWiFiMeshConfig.mIsGroupOwner = true;

        mWiFiDirectManagerLegacy.start();
    }

    public void onLC(View view) {
        mWiFiDirectManagerLegacy.mWiFiMeshConfig = new WiFiMeshConfig();
        mWiFiDirectManagerLegacy.mWiFiMeshConfig.mIsClient = true;

        mWiFiDirectManagerLegacy.start();
    }

    public void sendMessage(View view) {
        HelloMessage helloMessage = new HelloMessage();
        helloMessage.hello = "Hello from:"+ WiFiUtil.getLocalIpAddress();

        try {
            MeshHttpServer.on().sendMessage("192.168.49.1", helloMessage.toJson().getBytes());
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}