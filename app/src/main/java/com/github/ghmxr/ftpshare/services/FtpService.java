package com.github.ghmxr.ftpshare.services;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;

import com.github.ghmxr.ftpshare.Constants;
import com.github.ghmxr.ftpshare.R;
import com.github.ghmxr.ftpshare.activities.MainActivity;
import com.github.ghmxr.ftpshare.data.AccountItem;
import com.github.ghmxr.ftpshare.utils.APUtil;
import com.github.ghmxr.ftpshare.utils.MySQLiteOpenHelper;
import com.github.ghmxr.ftpshare.utils.ValueUtil;

import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.WritePermission;

import java.util.ArrayList;
import java.util.List;

public class FtpService extends Service {
    public static FtpServer server;
    public static PowerManager.WakeLock wakeLock;
    public static FtpService ftpService;
    private static MyHandler handler;
    public static OnFTPServiceStatusChangedListener listener;

    public static final int MESSAGE_START_FTP_COMPLETE=1;
    public static final int MESSAGE_START_FTP_ERROR=-1;
    public static final int MESSAGE_STOP_FTP_COMPLETE=0;
    public static final int MESSAGE_WAKELOCK_ACQUIRE=5;
    public static final int MESSAGE_WAKELOCK_RELEASE=6;

    private BroadcastReceiver receiver=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try{
                if(intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)){
                    NetworkInfo info=intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                    if(info.getState().equals(NetworkInfo.State.DISCONNECTED)&&info.getType()==ConnectivityManager.TYPE_WIFI
                            &&!info.isConnected()&&!APUtil.isAPEnabled(FtpService.this)){
                        stopService();
                        Log.d("Network",""+info.getState()+" "+info.getTypeName()+" "+info.isConnected());
                        try{
                            if(listener!=null) listener.onNetworkStatusChanged();
                        }catch (Exception e){e.printStackTrace();}
                    }
                }else if(intent.getAction().equals("android.net.wifi.WIFI_AP_STATE_CHANGED")){
                    int state=intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,0);
                    if(state==11&&!ValueUtil.isWifiConnected(FtpService.this)){
                        stopService();
                        Log.d("APState",""+state);
                        try{
                            if(listener!=null) listener.onNetworkStatusChanged();
                        }catch (Exception e){e.printStackTrace();}
                    }
                }
            }catch (Exception e){e.printStackTrace();}
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        ftpService=this;
        handler=new MyHandler();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (FtpService.class){
                    try{
                        startFTPService();
                        sendEmptyMessage(MESSAGE_START_FTP_COMPLETE);
                    }catch (Exception e){
                        e.printStackTrace();
                        Message msg=new Message();
                        msg.what=MESSAGE_START_FTP_ERROR;
                        msg.obj=e;
                        sendMessage(msg);
                    }
                }
            }
        }).start();
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static void startService(Activity activity){
        Intent intent=new Intent(activity,FtpService.class);
        activity.startService(intent);
    }

    public static List<AccountItem> getUserAccountList(Context context){
        List<AccountItem> list=new ArrayList<>();
        try{
            SQLiteDatabase db= new MySQLiteOpenHelper(context).getWritableDatabase();
            Cursor cursor=db.rawQuery("select * from "+Constants.SQLConsts.TABLE_NAME,null);
            while (cursor.moveToNext()){
                try{
                    AccountItem item=new AccountItem();
                    item.id=cursor.getInt(cursor.getColumnIndex(Constants.SQLConsts.COLUMN_ID));
                    item.account=cursor.getString(cursor.getColumnIndex(Constants.SQLConsts.COLUMN_ACCOUNT_NAME));
                    item.password=cursor.getString(cursor.getColumnIndex(Constants.SQLConsts.COLUMN_PASSWORD));
                    item.path=cursor.getString(cursor.getColumnIndex(Constants.SQLConsts.COLUMN_PATH));
                    item.writable=cursor.getInt(cursor.getColumnIndex(Constants.SQLConsts.COLUMN_WRITABLE))==1;
                    list.add(item);
                }catch (Exception e){e.printStackTrace();}
            }
            cursor.close();
        }catch (Exception e){
            e.printStackTrace();
        }
        return list;
    }

    private void startFTPService() throws Exception{
        FtpServerFactory factory=new FtpServerFactory();
        SharedPreferences settings = getSharedPreferences(Constants.PreferenceConsts.FILE_NAME,Context.MODE_PRIVATE);
        List<Authority> authorities_writable = new ArrayList<>();
        authorities_writable.add(new WritePermission());
        if(settings.getBoolean(Constants.PreferenceConsts.ANONYMOUS_MODE,Constants.PreferenceConsts.ANONYMOUS_MODE_DEFAULT)){
            BaseUser baseUser = new BaseUser();
            baseUser.setName(Constants.FTPConsts.NAME_ANONYMOUS);
            baseUser.setPassword("");
            baseUser.setHomeDirectory(settings.getString(Constants.PreferenceConsts.ANONYMOUS_MODE_PATH,Constants.PreferenceConsts.ANONYMOUS_MODE_PATH_DEFAULT));
            if(settings.getBoolean(Constants.PreferenceConsts.ANONYMOUS_MODE_WRITABLE,Constants.PreferenceConsts.ANONYMOUS_MODE_WRITABLE_DEFAULT)){
                baseUser.setAuthorities(authorities_writable);
            }
            factory.getUserManager().save(baseUser);
        }else{
            List<AccountItem> list=getUserAccountList(this);
            for(AccountItem item:list){
                BaseUser baseUser = new BaseUser();
                baseUser.setName(item.account);
                baseUser.setPassword(item.password);
                baseUser.setHomeDirectory(item.path);
                if(item.writable) baseUser.setAuthorities(authorities_writable);
                factory.getUserManager().save(baseUser);
            }
        }

        ListenerFactory lfactory = new ListenerFactory();
        lfactory.setPort(settings.getInt(Constants.PreferenceConsts.PORT_NUMBER,Constants.PreferenceConsts.PORT_NUMBER_DEFAULT)); //设置端口号 非ROOT不可使用1024以下的端口
        factory.addListener("default", lfactory.createListener());
        ConnectivityManager manager=null;
        try{
           manager=(ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        }catch (Exception e){e.printStackTrace();}
        if(manager!=null){
            NetworkInfo info=manager.getActiveNetworkInfo();
            if((info==null||info.getType()!=ConnectivityManager.TYPE_WIFI)&&!APUtil.isAPEnabled(this)) {
                throw new Exception(getResources().getString(R.string.attention_no_active_network));
            }
        }
        try{
            if(server!=null) server.stop();
        }catch (Exception e){}
        server=factory.createServer();
        server.start();
    }

    private void makeThisForeground(){
        try{
            NotificationManager manager=(NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
            if(Build.VERSION.SDK_INT>=26){
                NotificationChannel channel=new NotificationChannel("default","default",NotificationManager.IMPORTANCE_DEFAULT);
                manager.createNotificationChannel(channel);
            }
            NotificationCompat.Builder builder=new NotificationCompat.Builder(this,"default");
            builder.setSmallIcon(R.mipmap.ic_launcher);
            builder.setContentTitle(getResources().getString(R.string.notification_title));
            builder.setContentText(getFTPStatusDescription(this));

            RemoteViews rv=new RemoteViews(getPackageName(),R.layout.layout_notification);
            rv.setTextViewText(R.id.notification_title,getResources().getString(R.string.notification_title));
            rv.setTextViewText(R.id.notification_text,getFTPStatusDescription(this));
            builder.setCustomContentView(rv);

            builder.setContentIntent(PendingIntent.getActivity(this,0,new Intent(this, MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT));

            startForeground(1,builder.build());
        }catch (Exception e){e.printStackTrace();}
    }

    public static void stopService(){
        try{
            new Thread(new Runnable() {
                @Override
                public void run() {
                    synchronized (FtpService.class){
                        try{
                            if(server!=null){
                                server.stop();
                                server=null;
                            }
                        }catch (Exception e){e.printStackTrace();}
                        sendEmptyMessage(MESSAGE_STOP_FTP_COMPLETE);
                    }
                }
            }).start();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void sendEmptyMessage(int what){
        try{
            handler.sendEmptyMessage(what);
        }catch (Exception e){}
    }

    public static void sendMessage(Message msg){
        try{
            handler.sendMessage(msg);
        }catch (Exception e){}
    }

    private void processMessage(Message msg){
        try{
            switch (msg.what){
                default:break;
                case MESSAGE_START_FTP_COMPLETE:{
                    try{
                        IntentFilter filter=new IntentFilter();
                        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
                        filter.addAction("android.net.wifi.WIFI_AP_STATE_CHANGED");
                        registerReceiver(receiver,filter);
                    }catch (Exception e){e.printStackTrace();}

                    if(getSharedPreferences(Constants.PreferenceConsts.FILE_NAME,Context.MODE_PRIVATE).getBoolean(Constants.PreferenceConsts.WAKE_LOCK,Constants.PreferenceConsts.WAKE_LOCK_DEFAULT)){
                        sendEmptyMessage(MESSAGE_WAKELOCK_ACQUIRE);
                    }else {
                        sendEmptyMessage(MESSAGE_WAKELOCK_RELEASE);
                    }
                    Log.d("FTP",""+FtpService.isFTPServiceRunning());
                    try{
                        if(listener!=null) listener.onFTPServiceStarted();
                    }catch (Exception e){e.printStackTrace();}
                    makeThisForeground();
                }
                break;
                case MESSAGE_START_FTP_ERROR:{
                    stopService();
                    try{
                        if(listener!=null) listener.onFTPServiceStartError((Exception)msg.obj);
                    }catch (Exception e){e.printStackTrace();}
                }
                break;
                case MESSAGE_WAKELOCK_ACQUIRE:{
                    try{
                        try{
                            if(wakeLock!=null) wakeLock.release();
                        }catch (Exception e){}
                        PowerManager powerManager=(PowerManager)getSystemService(Context.POWER_SERVICE);
                        wakeLock=powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"ftp_wake_lock");
                        wakeLock.acquire();
                    }catch (Exception e){e.printStackTrace();}

                }
                break;
                case MESSAGE_WAKELOCK_RELEASE:{
                    try{
                        if(wakeLock!=null) wakeLock.release();
                        wakeLock=null;
                    }catch (Exception e){e.printStackTrace();}
                }
                break;
                case MESSAGE_STOP_FTP_COMPLETE:{
                    stopSelf();
                }
                break;
            }
        }catch (Exception e){e.printStackTrace();}
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("onDestroy","onDestroy method called");
        try{
            if(server!=null){
                server.stop();
                server=null;
            }
        }catch (Exception e){e.printStackTrace();}
        try{
            if(wakeLock!=null){
                wakeLock.release();
                wakeLock=null;
            }
        }catch (Exception e){e.printStackTrace();}
        try{
            unregisterReceiver(receiver);
        }catch (Exception e){e.printStackTrace();}
        try{
            if(listener!=null) listener.onFTPServiceDestroyed();
        }catch (Exception e){}
        handler=null;
        ftpService=null;
    }

    private static class MyHandler extends Handler{
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            try{
                if(ftpService!=null) ftpService.processMessage(msg);
            }catch (Exception e){e.printStackTrace();}
        }
    }

    public static void setOnFTPServiceStatusChangedListener(OnFTPServiceStatusChangedListener ls){
        listener=ls;
    }

    public static boolean isFTPServiceRunning(){
        try{
            return server!=null&&!server.isStopped();
        }catch (Exception e){e.printStackTrace();}
        return false;
    }

    public static String getFTPStatusDescription(Context context){
        try{
            if(!isFTPServiceRunning()) return context.getResources().getString(R.string.ftp_status_not_running);
            return context.getResources().getString(R.string.ftp_status_running_head)+ValueUtil.getFTPServiceFullAddress(context);
        }catch (Exception e){e.printStackTrace();}
        return "";
    }

    public interface OnFTPServiceStatusChangedListener {
        void onFTPServiceStarted();
        void onFTPServiceStartError(Exception e);
        void onNetworkStatusChanged();
        void onFTPServiceDestroyed();
    }
}
