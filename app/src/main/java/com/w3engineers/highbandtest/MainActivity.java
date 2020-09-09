package com.w3engineers.highbandtest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.w3engineers.highbandtest.protocol.model.HelloMessage;
import com.w3engineers.highbandtest.protocol.util.Constant;
import com.w3engineers.highbandtest.protocol.util.WiFiUtil;
import com.w3engineers.highbandtest.protocol.wifi.httpservices.MeshHttpServer;
import com.w3engineers.highbandtest.util.MeshLog;
import com.w3engineers.highbandtest.util.PermissionUtil;

import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {
    private ProtocolManager protocolManager;
    public static final int REQUEST_ENABLE_DSC = 107;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkPermissionAndStartLib();
    }

    private void checkPermissionAndStartLib() {
        if (PermissionUtil.init(this).request(Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            requestDiscoverableTimePeriod();
            startProtocolManager();
        }
    }

    private void requestDiscoverableTimePeriod() {
        MeshLog.v("requestDiscoverableTimePeriod alert");
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 3600);
        startActivityForResult(intent, REQUEST_ENABLE_DSC);
    }


    private void startProtocolManager(){
        protocolManager = ProtocolManager.on(getApplicationContext());
        protocolManager.startProtocol();
        MeshHttpServer.on().start(protocolManager, ProtocolManager.HTTP_PORT);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.e("Permission-ask", "Protocol manager started :"+requestCode);
        if(PermissionUtil.REQUEST_CODE_PERMISSION_DEFAULT == requestCode){
            checkPermissionAndStartLib();
        }else if (requestCode == REQUEST_ENABLE_DSC) {
            startProtocolManager();
        }
    }

    public void sendMessage(View view) {
        HelloMessage helloMessage = new HelloMessage(null);
        helloMessage.hello = "Hello from:"+ WiFiUtil.getLocalIpAddress();

        try {
            MeshHttpServer.on().sendMessage(Constant.MASTER_IP_ADDRESS, helloMessage.toJson().getBytes());
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}