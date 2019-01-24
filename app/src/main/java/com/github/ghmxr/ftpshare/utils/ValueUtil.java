package com.github.ghmxr.ftpshare.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;

import com.github.ghmxr.ftpshare.Constants;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.File;
import java.util.Hashtable;
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
    public static Bitmap getQrCodeBitmapOfString(String content,int w,int h){
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
    }

    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }
}
