package com.github.ghmxr.ftpshare.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;

import com.github.ghmxr.ftpshare.Constants;

import java.io.File;
import java.util.Locale;

public class ValueUtil {
    /**
     * judge if it is a child path of parent
     */
    public static boolean isChildPathOfCertainPath(File child,File parent){
        try {
            if(child.getAbsolutePath().trim().toLowerCase(Locale.getDefault()).equals(parent.getAbsolutePath().trim().toLowerCase(Locale.getDefault()))) return true;
            else{
                while((child=child.getParentFile())!=null){
                    if(child.getAbsolutePath().trim().toLowerCase(Locale.getDefault()).equals(parent.getAbsolutePath().trim().toLowerCase(Locale.getDefault()))) return true;
                }
            }
        }catch (Exception e){e.printStackTrace();}
        return false;
    }

    public static String getIPAddressForFTPService(Context context){
        try{
            if(APUtil.isAPEnabled(context)) return APUtil.AP_HOST_IP;
            ConnectivityManager manager=(ConnectivityManager)context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo=manager.getActiveNetworkInfo();
            if(networkInfo!=null&&networkInfo.getType()==ConnectivityManager.TYPE_WIFI){
                return Formatter.formatIpAddress(((WifiManager)context.getApplicationContext().getSystemService(Context.WIFI_SERVICE)).getConnectionInfo().getIpAddress());
            }
        }catch (Exception e){e.printStackTrace();}
        return "0.0.0.0";
    }

    public static String getFTPServiceFullAddress(Context context){
        return "ftp://"+ValueUtil.getIPAddressForFTPService(context)+":"+context.getSharedPreferences(Constants.PreferenceConsts.FILE_NAME,Context.MODE_PRIVATE).getInt(Constants.PreferenceConsts.PORT_NUMBER,Constants.PreferenceConsts.PORT_NUMBER_DEFAULT);
    }
}
