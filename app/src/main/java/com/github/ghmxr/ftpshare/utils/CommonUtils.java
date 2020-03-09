package com.github.ghmxr.ftpshare.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.support.annotation.NonNull;
import android.text.format.Formatter;

import com.github.ghmxr.ftpshare.Constants;

import java.io.File;

public class CommonUtils {
    /**
     * judge if it is a child path of parent
     */
    public static boolean isChildPathOfCertainPath(File child,File parent){
        try {
            return child.getAbsolutePath().toLowerCase().startsWith(parent.getAbsolutePath().toLowerCase());
        }catch (Exception e){e.printStackTrace();}
        return false;
    }

    private static String getIPAddressForFTPService(Context context){
        try{
            ConnectivityManager manager=(ConnectivityManager)context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo=manager.getActiveNetworkInfo();
            if(networkInfo!=null&&networkInfo.getType()==ConnectivityManager.TYPE_WIFI){
                return Formatter.formatIpAddress(((WifiManager)context.getApplicationContext().getSystemService(Context.WIFI_SERVICE)).getConnectionInfo().getIpAddress());
            }
        }catch (Exception e){e.printStackTrace();}
        try{
            if(APUtil.isAPEnabled(context)){
                if(isCellularNetworkConnected(context)) return APUtil.AP_HOST_IP;
                else return APUtil.getAvailableIP();
            }
        }catch (Exception e){e.printStackTrace();}
        return "0.0.0.0";
    }

    public static String getFTPServiceFullAddress(Context context){
        return "ftp://"+ CommonUtils.getIPAddressForFTPService(context)+":"+context.getSharedPreferences(Constants.PreferenceConsts.FILE_NAME,Context.MODE_PRIVATE).getInt(Constants.PreferenceConsts.PORT_NUMBER,Constants.PreferenceConsts.PORT_NUMBER_DEFAULT);
    }
    /*public static Bitmap getQrCodeBitmapOfString(String content,int w,int h){
        try{
            Hashtable<EncodeHintType,String> hints=new Hashtable<>();
            hints.put(EncodeHintType.CHARACTER_SET,"utf-8");
            BitMatrix bitMatrix=new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE,w,h,hints);
            int []pixels=new int[w*h];
            for (int y = 0; y < h; y++){
                for (int x = 0; x < w; x++){
                    if (bitMatrix.get(x, y)){
                        pixels[y * w + x] = 0xff000000;
                    }
                    else{
                        pixels[y * w + x] = 0xffffffff;
                    }
                }
            }
            Bitmap bitmap= Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels,0,w,0,0,w,h);
            return bitmap;
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }*/

    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    public static boolean isWifiConnected(Context context){
        try{
            ConnectivityManager connectivityManager=(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if(connectivityManager==null) return false;
            if(connectivityManager.getActiveNetworkInfo().getType()==ConnectivityManager.TYPE_WIFI) return true;
        }catch (Exception e){e.printStackTrace();}
        return false;
    }

    private static boolean isCellularNetworkConnected(Context context){
        try{
            ConnectivityManager connectivityManager=(ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo=connectivityManager.getActiveNetworkInfo();
            if(networkInfo==null) return false;
            if(networkInfo.getType()==ConnectivityManager.TYPE_MOBILE) return true;
            if(networkInfo.getType()==ConnectivityManager.TYPE_WIFI){
                //Method getMobileDataEnabledMethod = ConnectivityManager.class.getDeclaredMethod("getMobileDataEnabled");
                //getMobileDataEnabledMethod.setAccessible(true);
                //return (Boolean) getMobileDataEnabledMethod.invoke(connectivityManager);
                return false;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 获取本应用名称
     */
    public static @NonNull
    String getAppName(@NonNull Context context){
        try{
            PackageManager packageManager=context.getPackageManager();
            ApplicationInfo applicationInfo=packageManager.getApplicationInfo(context.getPackageName(),0);
            return String.valueOf(packageManager.getApplicationLabel(applicationInfo));
        }catch (Exception e){e.printStackTrace();}
        return "";
    }

    /**
     * 获取本应用版本名
     */
    public static @NonNull String getAppVersionName(@NonNull Context context){
        try{
            PackageManager packageManager=context.getPackageManager();
            return String.valueOf(packageManager.getPackageInfo(context.getPackageName(),0).versionName);
        }catch (Exception e){e.printStackTrace();}
        return "";
    }
}
