package com.github.ghmxr.ftpshare.services;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.PermissionChecker;
import android.view.View;
import android.widget.Toast;

import com.github.ghmxr.ftpshare.Constants;
import com.github.ghmxr.ftpshare.MyApplication;
import com.github.ghmxr.ftpshare.R;
import com.github.ghmxr.ftpshare.activities.MainActivity;
import com.github.ghmxr.ftpshare.data.AccountItem;
import com.github.ghmxr.ftpshare.utils.CommonUtils;
import com.github.ghmxr.ftpshare.utils.MySQLiteOpenHelper;
import com.github.ghmxr.ftpshare.utils.NetworkStatusMonitor;

import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.WritePermission;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class FtpService extends Service implements NetworkStatusMonitor.NetworkStatusCallback {
    private static FtpServer server;//this static field is guarded by FtpService.class
    private static PowerManager.WakeLock wakeLock;
    private static FtpService ftpService;
    private static MyHandler handler;
    private static final LinkedList<OnFTPServiceStatusChangedListener> listeners=new LinkedList<>();

    public static final int MESSAGE_START_FTP_COMPLETE=1;
    public static final int MESSAGE_START_FTP_ERROR=-1;
    public static final int MESSAGE_WAKELOCK_ACQUIRE=5;
    public static final int MESSAGE_WAKELOCK_RELEASE=6;

    private boolean isIgnoreAutoDisconnect=false;

    //public static final int MESSAGE_REFRESH_FOREGROUND_NOTIFICATION=7;

    /*private BroadcastReceiver receiver=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try{
                if(intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)){
                    NetworkInfo info=intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                    if(info.getState().equals(NetworkInfo.State.DISCONNECTED)&&info.getType()==ConnectivityManager.TYPE_WIFI
                            &&!info.isConnected()&&!APUtil.isAPEnabled(FtpService.this)){
                        stopSelf();
                        //Log.d("Network",""+info.getState()+" "+info.getTypeName()+" "+info.isConnected());
                    }
                }else if(intent.getAction().equals("android.net.wifi.WIFI_AP_STATE_CHANGED")){
                    int state=intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,0);
                    if(state==11&&!CommonUtils.isWifiConnected(FtpService.this)){
                        stopSelf();
                        //Log.d("APState",""+state);
                    }
                }
            }catch (Exception e){e.printStackTrace();}
        }
    };*/

    @Override
    public void onCreate() {
        super.onCreate();
        ftpService=this;
        handler=new MyHandler();
        NetworkStatusMonitor.addNetworkStatusCallback(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        makeThisForeground(getResources().getString(R.string.app_name),getResources().getString(R.string.attention_opening_ftp));
        if(!beforeStartCheck(this)){
            stopSelf();
            return super.onStartCommand(intent,flags,startId);
        }
        final boolean isAnonymousMode=CommonUtils.getSettingSharedPreferences(this)
                .getBoolean(Constants.PreferenceConsts.ANONYMOUS_MODE,Constants.PreferenceConsts.ANONYMOUS_MODE_DEFAULT);
        final List<AccountItem>accountItems=getUserAccountList(this);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    startFTPService(isAnonymousMode,accountItems);
                    sendEmptyMessage(MESSAGE_START_FTP_COMPLETE);
                }catch (Exception e){
                    e.printStackTrace();
                    Message msg=new Message();
                    msg.what=MESSAGE_START_FTP_ERROR;
                    msg.obj=e;
                    sendMessage(msg);
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

    @Override
    public void onNetworkStatusRefreshed() {
        makeThisForeground(getResources().getString(R.string.notification_title),
                getResources().getString(R.string.ftp_status_running_head)+ CommonUtils.getFTPServiceDisplayAddress(this));
    }

    @Override
    public void onNetworkConnected(NetworkStatusMonitor.NetworkType networkType) {}

    @Override
    public void onNetworkDisconnected(NetworkStatusMonitor.NetworkType networkType) {
        if(isIgnoreAutoDisconnect) return;
        int config=CommonUtils.getSettingSharedPreferences(this).getInt(Constants.PreferenceConsts.AUTO_STOP,Constants.PreferenceConsts.AUTO_STOP_DEFAULT);
        switch (config){
            default:case Constants.PreferenceConsts.AUTO_STOP_NONE:return;
            case Constants.PreferenceConsts.AUTO_STOP_WIFI_DISCONNECTED:{
                if(networkType== NetworkStatusMonitor.NetworkType.WIFI){
                    stopSelf();
                }
            }
            break;
            case Constants.PreferenceConsts.AUTO_STOP_AP_DISCONNECTED:{
                if(networkType== NetworkStatusMonitor.NetworkType.AP){
                    stopSelf();
                }
            }
            break;
        }
    }

    /**
     * 如果FTP Service 没有实例将创建实例并启动
     * @return true 启动成功
     */
    public static boolean startService(@NonNull Context context){
        if(!beforeStartCheck(context)){
            return false;
        }
        if(ftpService==null) {
            if(Build.VERSION.SDK_INT>=26)context.startForegroundService(new Intent(context,FtpService.class));
            else context.startService(new Intent(context,FtpService.class));
        }
        return true;
    }

    /**
     * 启动前检查读写权限以及用户模式下是否正确设置账户，如果上下文为Activity将会向Activity发送SnackBar，否则会通过
     * 上下文发送Toast
     * @param context 启动服务的上下文
     * @return true - 检查正常  -false 账户错误或者没有权限
     */
    private static boolean beforeStartCheck(@NonNull Context context){
        if(Build.VERSION.SDK_INT>=23&& PermissionChecker.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PermissionChecker.PERMISSION_GRANTED){
            if(context instanceof Activity){
                final Activity activity=(Activity)context;
                Snackbar snackbar=Snackbar.make(activity.findViewById(android.R.id.content),activity.getResources().getString(R.string.permission_write_external),Snackbar.LENGTH_SHORT);
                snackbar.setAction(activity.getResources().getString(R.string.snackbar_action_goto), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent appdetail = new Intent();
                        appdetail.setAction(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        appdetail.setData(Uri.fromParts("package", activity.getApplication().getPackageName(), null));
                        activity.startActivity(appdetail);
                    }
                });
                snackbar.show();
                activity.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},0);
            }else{
                Toast.makeText(context,context.getResources().getString(R.string.permission_write_external),Toast.LENGTH_SHORT).show();
            }
            return false;
        }
        if(!context.getSharedPreferences(Constants.PreferenceConsts.FILE_NAME,Context.MODE_PRIVATE)
                .getBoolean(Constants.PreferenceConsts.ANONYMOUS_MODE,Constants.PreferenceConsts.ANONYMOUS_MODE_DEFAULT)
                &&FtpService.getUserAccountList(context).size()==0){
            if(context instanceof Activity){
                final Activity activity=(Activity)context;
                Snackbar.make(activity.findViewById(android.R.id.content),activity.getResources().getString(R.string.attention_no_user_account),Snackbar.LENGTH_SHORT).show();
            }else{
                Toast.makeText(context,context.getResources().getString(R.string.attention_no_user_account),Toast.LENGTH_SHORT).show();
            }
            return false;
        }
        return true;
    }

    public static List<AccountItem> getUserAccountList(@NonNull Context context){
        List<AccountItem> list=new ArrayList<>();
        try{
            SQLiteDatabase db= new MySQLiteOpenHelper(context).getReadableDatabase();
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
            db.close();
        }catch (Exception e){
            e.printStackTrace();
        }
        return list;
    }

    private void startFTPService(boolean isAnonymousMode,List<AccountItem>list) throws Exception{
        FtpServerFactory factory=new FtpServerFactory();
        SharedPreferences settings = getSharedPreferences(Constants.PreferenceConsts.FILE_NAME,Context.MODE_PRIVATE);
        List<Authority> authorities_writable = new ArrayList<>();
        authorities_writable.add(new WritePermission());
        if(isAnonymousMode){
            BaseUser baseUser = new BaseUser();
            baseUser.setName(Constants.FTPConsts.NAME_ANONYMOUS);
            baseUser.setPassword("");
            baseUser.setHomeDirectory(settings.getString(Constants.PreferenceConsts.ANONYMOUS_MODE_PATH,Constants.PreferenceConsts.ANONYMOUS_MODE_PATH_DEFAULT));
            if(settings.getBoolean(Constants.PreferenceConsts.ANONYMOUS_MODE_WRITABLE,Constants.PreferenceConsts.ANONYMOUS_MODE_WRITABLE_DEFAULT)){
                baseUser.setAuthorities(authorities_writable);
            }
            factory.getUserManager().save(baseUser);
        }else{
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
            /*try{
                manager=(ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            }catch (Exception e){e.printStackTrace();}
            if(manager!=null){
                NetworkInfo info=manager.getActiveNetworkInfo();
                if((info==null||info.getType()!=ConnectivityManager.TYPE_WIFI)&&!APUtil.isAPEnabled(this)) {
                    throw new Exception(getResources().getString(R.string.attention_no_active_network));
                }
            }*/
        synchronized (FtpService.class) {
            try{
                if(server!=null) server.stop();
            }catch (Exception e){}
            server=factory.createServer();
            server.start();
        }
    }

    private void makeThisForeground(String title,String content){
        try{
            NotificationManager manager=(NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
            if(Build.VERSION.SDK_INT>=26){
                NotificationChannel channel=new NotificationChannel("default"
                        ,getResources().getString(R.string.notification_channel_foreground_service)
                        ,NotificationManager.IMPORTANCE_NONE);
                manager.createNotificationChannel(channel);
            }
            NotificationCompat.Builder builder=new NotificationCompat.Builder(this,"default");
            builder.setSmallIcon(R.drawable.ic_ex_24dp);
            builder.setContentTitle(title);
            builder.setContentText(content);
            builder.setContentIntent(PendingIntent.getActivity(this,0,new Intent(this, MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT));
            startForeground(1,builder.build());
        }catch (Exception e){e.printStackTrace();}
    }

    /**
     *check if have ftp service instance and kill it
     */
    public static void stopService(){
        try{
            if(ftpService!=null) ftpService.stopSelf();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void sendEmptyMessage(int what){
        try{
            if(handler!=null) handler.sendEmptyMessage(what);
        }catch (Exception e){e.printStackTrace();}
    }

    public static void sendMessage(Message msg){
        try{
            if(handler!=null) handler.sendMessage(msg);
        }catch (Exception e){e.printStackTrace();}
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
                        //registerReceiver(receiver,filter);
                    }catch (Exception e){e.printStackTrace();}

                    if(getSharedPreferences(Constants.PreferenceConsts.FILE_NAME,Context.MODE_PRIVATE).getBoolean(Constants.PreferenceConsts.WAKE_LOCK,Constants.PreferenceConsts.WAKE_LOCK_DEFAULT)){
                        sendEmptyMessage(MESSAGE_WAKELOCK_ACQUIRE);
                    }else {
                        sendEmptyMessage(MESSAGE_WAKELOCK_RELEASE);
                    }
                    //Log.d("FTP",""+FtpService.isFTPServiceRunning());
                    for(OnFTPServiceStatusChangedListener listener:listeners){
                        listener.onFTPServiceStarted();
                    }

                    if(getSharedPreferences(Constants.PreferenceConsts.FILE_NAME,Context.MODE_PRIVATE)
                            .getInt(Constants.PreferenceConsts.AUTO_STOP,Constants.PreferenceConsts.AUTO_STOP_DEFAULT)
                    ==Constants.PreferenceConsts.AUTO_STOP_TIME_COUNT){
                        setCountSecondsAndStart(getSharedPreferences(Constants.PreferenceConsts.FILE_NAME,Context.MODE_PRIVATE)
                                .getInt(Constants.PreferenceConsts.AUTO_STOP_VALUE,Constants.PreferenceConsts.AUTO_STOP_VALUE_DEFAULT));
                    }
                    makeThisForeground(getResources().getString(R.string.notification_title),
                            getResources().getString(R.string.ftp_status_running_head)+ CommonUtils.getFTPServiceDisplayAddress(this));
                    //FtpWidget.sendUpdateWidgetBroadcast(this,null);
                    //MyTileService.requestRefreshTile(this);
                }
                break;
                case MESSAGE_START_FTP_ERROR:{
                    for(OnFTPServiceStatusChangedListener listener:listeners){
                        listener.onFTPServiceStartError((Exception)msg.obj);
                    }
                    stopSelf();
                }
                break;
                case MESSAGE_WAKELOCK_ACQUIRE:{
                    try{
                        try{
                            if(wakeLock!=null) wakeLock.release();
                        }catch (Exception e){}
                        PowerManager powerManager=(PowerManager)getSystemService(Context.POWER_SERVICE);
                        wakeLock=powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"FTP Share:ftp_wake_lock");
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
            }
        }catch (Exception e){e.printStackTrace();}
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //Log.d("onDestroy","onDestroy method called");
        NetworkStatusMonitor.removeNetworkStatusCallback(this);
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
                }
            }
        }).start();
        try{
            if(wakeLock!=null){
                wakeLock.release();
                wakeLock=null;
            }
        }catch (Exception e){e.printStackTrace();}
       /* try{
            unregisterReceiver(receiver);
        }catch (Exception e){e.printStackTrace();}*/
        countHandler.removeCallbacks(stopExecutor);
        handler=null;
        ftpService=null;

        for(OnFTPServiceStatusChangedListener listener:listeners){
            listener.onFTPServiceDestroyed();
        }
        //FtpWidget.sendUpdateWidgetBroadcast(this,null);
        //MyTileService.requestRefreshTile(this);
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


    public static synchronized void addOnFtpServiceStatusChangedListener(OnFTPServiceStatusChangedListener listener){
        if(!listeners.contains(listener)){
            listeners.add(listener);
        }
    }

    public static synchronized void removeOnFtpServiceStatusChangedListener(OnFTPServiceStatusChangedListener listener){
        listeners.remove(listener);
    }

    public static boolean isFTPServiceRunning(){
        return ftpService!=null&&server!=null&&!server.isStopped();
    }

    public static String getFTPStatusDescription(Context context){
        try{
            if(!isFTPServiceRunning()) return context.getResources().getString(R.string.ftp_status_not_running);
            return context.getResources().getString(R.string.ftp_status_running_head)+ CommonUtils.getFTPServiceDisplayAddress(context);
        }catch (Exception e){e.printStackTrace();}
        return "";
    }

    public interface OnFTPServiceStatusChangedListener {
        void onFTPServiceStarted();
        void onFTPServiceStartError(Exception e);
        void onRemainingSeconds(int seconds);
        void onFTPServiceDestroyed();
    }

    public static String getCharsetFromSharedPreferences(){
        return MyApplication.getGlobalBaseContext().getSharedPreferences(Constants.PreferenceConsts.FILE_NAME, Context.MODE_PRIVATE)
                .getString(Constants.PreferenceConsts.CHARSET_TYPE,Constants.PreferenceConsts.CHARSET_TYPE_DEFAULT);
    }

    private int countSeconds=0;
    private final Handler countHandler=new Handler();
    private final Runnable stopExecutor=new Runnable() {
        @Override
        public void run() {
            if(countSeconds<=0){
                stopSelf();
            }else{
                countSeconds--;
                for(OnFTPServiceStatusChangedListener listener:listeners){
                    listener.onRemainingSeconds(countSeconds);
                }
                countHandler.postDelayed(this,1000L);
            }
        }
    };

    private void setCountSecondsAndStart(int seconds){
        if(seconds<0)return;
        countHandler.removeCallbacks(stopExecutor);
        this.countSeconds=seconds;
        if(seconds==0||isIgnoreAutoDisconnect)return;
        for(OnFTPServiceStatusChangedListener listener:listeners){
            listener.onRemainingSeconds(countSeconds);
        }
        countHandler.postDelayed(stopExecutor,1000L);
    }

    /**
     * 执行此方法后，在指定秒数后会自动停止Ftp服务（有实例在运行的情况下）
     * @param seconds 倒计时秒数
     */
    public static void setTimeCounts(int seconds){
        if(ftpService==null)return;
        ftpService.setCountSecondsAndStart(seconds);
    }

    /**
     * 获得当前ftp服务的倒计时秒数
     */
    public static int getTimeCounts(){
        if(ftpService==null) return -1;
        return ftpService.countSeconds;
    }

    /**
     * 取消本此倒计时断开计时器
     */
    public static void cancelTimeCounts(){
        if(ftpService==null)return;
        ftpService.setCountSecondsAndStart(0);
    }

    /**
     * 取消本次FTP服务的自动断开触发机制
     */
    public static void disableAutoDisconnectThisTime(){
        if(ftpService==null)return;
        ftpService.isIgnoreAutoDisconnect=true;
        cancelTimeCounts();
    }

    /**
     * 打开本此FTP服务的自动断开机制(如果SP已设置)
     */
    public static void enableAutoDisconnectThisTime(){
        if(ftpService==null)return;
        ftpService.isIgnoreAutoDisconnect=false;
    }

    public static boolean getIsIgnoreAutoCancelThisTime(){
        return ftpService!=null&&ftpService.isIgnoreAutoDisconnect;
    }
}
