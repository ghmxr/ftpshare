package com.github.ghmxr.ftpshare.utils;

import android.content.Context;
import android.net.wifi.WifiManager;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class APUtil {
    /**
     * @deprecated
     * AP Host IP Address
     */
    public static final String AP_HOST_IP="192.168.43.1";
    public static boolean isAPEnabled(Context context){
        try{
            WifiManager wifiManager=(WifiManager)context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            Method method=wifiManager.getClass().getDeclaredMethod("getWifiApState");
            Field field=wifiManager.getClass().getDeclaredField("WIFI_AP_STATE_ENABLED");
            int value_wifi_enabled=(int)field.get(wifiManager);
            return ((int)method.invoke(wifiManager))==value_wifi_enabled;
        }catch (Exception e){e.printStackTrace();}
        return false;
    }
    public static String getAvailableIP(){
        try{
            Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
            NetworkInterface nif;
            while((nif=en.nextElement())!=null){
                Enumeration<InetAddress> enumIpAddr = nif.getInetAddresses();
                try{
                    InetAddress mInetAddress;
                    while((mInetAddress=enumIpAddr.nextElement())!=null){
                        if(!mInetAddress.isLoopbackAddress()&&(mInetAddress instanceof Inet4Address)){
                            return mInetAddress.getHostAddress();
                        }
                    }
                }catch(Exception e){e.printStackTrace();}
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return "0.0.0.0";
    }
}
