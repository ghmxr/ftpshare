package com.github.ghmxr.ftpshare.utils;

import android.content.Context;
import android.net.wifi.WifiManager;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class APUtil {
    /**
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
}
