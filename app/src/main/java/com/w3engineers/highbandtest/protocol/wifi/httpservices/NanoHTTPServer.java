package com.w3engineers.highbandtest.protocol.wifi.httpservices;

import com.w3engineers.highbandtest.protocol.util.AddressUtil;
import com.w3engineers.highbandtest.protocol.wifi.httpservices.nanohttpd.protocols.http.IHTTPSession;
import com.w3engineers.highbandtest.protocol.wifi.httpservices.nanohttpd.protocols.http.NanoHTTPD;
import com.w3engineers.highbandtest.protocol.wifi.httpservices.nanohttpd.protocols.http.response.Response;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import static com.w3engineers.highbandtest.protocol.wifi.httpservices.nanohttpd.protocols.http.response.Response.newFixedLengthResponse;

/**
 * NANO http server manager
 */
public class NanoHTTPServer extends NanoHTTPD {
    private HttpDataListener httpDataListener;

    public interface HttpDataListener {
        void receivedData(String ipAddress, String data);
    }

    public void setHttpDataListener(HttpDataListener httpDataListener) {
        this.httpDataListener = httpDataListener;
    }


    public NanoHTTPServer(int port) throws IOException {
        super(port);
        start();
    }

    @Override
    public Response serve(IHTTPSession session) {
        try {
            session.parseBody(new HashMap<String, String>());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ResponseException e) {
            e.printStackTrace();
        }

        List<String> data = session.getParameters().get("data");
        if (data == null || data.isEmpty()) {
            return newFixedLengthResponse("Invalid request: 404");
        }

        if (httpDataListener != null) {
            httpDataListener.receivedData(session.getRemoteIpAddress(), data.get(0));
            //new Thread(() -> httpDataListener.receivedData(session.getRemoteIpAddress(), data.get(0))).start();

        }
        return newFixedLengthResponse(AddressUtil.getLocalIpAddress() + " Received data");
    }
}
