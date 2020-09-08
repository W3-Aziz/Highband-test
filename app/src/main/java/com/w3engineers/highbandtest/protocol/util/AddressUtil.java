package com.w3engineers.highbandtest.protocol.util;

import android.net.wifi.WifiInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.util.Patterns;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides Node Address utility
 */
public class AddressUtil {

    public static String deviceToString(WifiP2pDevice device) {
        return device.deviceName + " " + device.deviceAddress;
    }

    public static String getIpAddress(InetAddress inetAddress) {
        if(inetAddress != null) {
            return inetAddress.getHostAddress();
        }

        return null;
    }

    public static String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        return getIpAddress(inetAddress);
                    }
                }
            }
        } catch (SocketException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static String getWiFiIpAddress() {
        try {
            NetworkInterface intf = NetworkInterface.getByName("wlan0");
            for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                InetAddress inetAddress = enumIpAddr.nextElement();
                if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                    return getIpAddress(inetAddress);
                }
            }
        } catch (SocketException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static String getIpAddress(WifiInfo wifiInfo) {
        String result;
        int ip = wifiInfo.getIpAddress();

        result = String.format(Locale.ENGLISH, "%d.%d.%d.%d", (ip & 0xff), (ip >> 8 & 0xff), (ip >> 16 & 0xff),
                (ip >> 24 & 0xff));

        return result;
    }

    public static Inet6Address getIpv6() {
        List<NetworkInterface> ifaces;
        try {
            ifaces = Collections.list(NetworkInterface.getNetworkInterfaces());
        } catch(SocketException e) {
            return null;
        }
        String stuff = "Local IP addresses: \n";
        for(NetworkInterface iface : ifaces) {
            for(InetAddress addr : Collections.list(iface.getInetAddresses())) {

                String hostName = addr.getHostName();
                String canonicalName = addr.getCanonicalHostName();
                if(addr instanceof Inet6Address && hostName.contains("p2p") || canonicalName.contains("p2p")) {
                    return (Inet6Address) addr;
                }

            }
        }

        return null;
    }

    private static final String ipv6Pattern = "([0-9a-f]{1,4}:){7}([0-9a-f]){1,4}";
    private static Pattern VALID_IPV6_PATTERN = Pattern.compile(ipv6Pattern, Pattern.CASE_INSENSITIVE);
    //ipv6
    public static String getLocalIpV6() {
        Inet6Address inet6Address = getIpv6();
        if(inet6Address == null)
            return null;
        Matcher m2 = VALID_IPV6_PATTERN.matcher(inet6Address.getHostAddress());
        String[] strings = inet6Address.getHostAddress().split("%");
        return inet6Address.getHostAddress();
    }
    //ipv6
    public static String getLocalIpV6(InetAddress inetAddress) {
        Inet6Address inet6Address = getIpv6();
        if(inet6Address == null)
            return null;
        Matcher m2 = VALID_IPV6_PATTERN.matcher(inet6Address.getHostAddress());
        String[] strings = inet6Address.getHostAddress().split("%");
        return new String(inet6Address.getAddress());
    }

    public static String makeShortAddress(String address) {

        if(address != null) {
            if(address.length() > 2) {
                return address.substring(address.length() - 3);
            }
            return address;

        }
        return null;
    }

    //https://ethereum.stackexchange.com/a/21186
    public static boolean isValidEthAddress(String address) {
        return address != null && address.length() > 41 && address.startsWith("0x");
    }

    public static boolean isValidIPAddress(String ipAddress) {
        return ipAddress != null && Patterns.IP_ADDRESS.matcher(ipAddress).matches();
    }
}
