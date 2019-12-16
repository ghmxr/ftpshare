package com.github.ghmxr.ftpshare.widgets;

import android.Manifest;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.PermissionChecker;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.github.ghmxr.ftpshare.R;
import com.github.ghmxr.ftpshare.activities.MainActivity;
import com.github.ghmxr.ftpshare.services.FtpService;

public class FtpWidget extends AppWidgetProvider {
    public static final String INTENT_ACTION_UPDATE_WIDGET="com.github.ghmxr.ftpshare.UPDATE_WIDGET";
    public static final String EXTRA_ERROR_MESSAGE="error_message";

    public FtpWidget() {
        super();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if(intent!=null&&String.valueOf(intent.getAction()).equals(INTENT_ACTION_UPDATE_WIDGET)){
            try {
                AppWidgetManager.getInstance(context).updateAppWidget(new ComponentName(context,FtpWidget.class),getCurrentRemoteView(context));
            } catch (Exception e) {
                e.printStackTrace();
            }
            String error_message=intent.getStringExtra(EXTRA_ERROR_MESSAGE);
            if(error_message!=null){
                Toast.makeText(context,error_message,Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        appWidgetManager.updateAppWidget(new ComponentName(context,FtpWidget.class),getCurrentRemoteView(context));
    }

    private RemoteViews getCurrentRemoteView(Context context){
        RemoteViews remoteViews=new RemoteViews(context.getPackageName(), R.layout.widget_switch);
        boolean isRunning= FtpService.isFTPServiceRunning();
        remoteViews.setImageViewResource(R.id.widget_switch_icon,isRunning?R.drawable.switch_on:R.drawable.switch_off);
        remoteViews.setTextViewText(R.id.widget_description,isRunning?FtpService.getFTPStatusDescription(context):
                context.getResources().getString(R.string.ftp_status_not_running));
        remoteViews.setOnClickPendingIntent(R.id.widget_switch_area,
                PendingIntent.getBroadcast(context,
                        1,
                        new Intent(context, FtpWidgetReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT));
        remoteViews.setOnClickPendingIntent(R.id.widget_switch_root,
                PendingIntent.getActivity(context,2,new Intent(context, MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT));
        return remoteViews;
    }

    public static class FtpWidgetReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            if(PermissionChecker.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PermissionChecker.PERMISSION_GRANTED){
                Toast.makeText(context,context.getResources().getString(R.string.permission_write_external),Toast.LENGTH_SHORT).show();
                return;
            }
            boolean b=FtpService.isFTPServiceRunning();
            if(b)FtpService.stopService();
            else FtpService.startService(context);
        }
    }

    public static void sendUpdateWidgetBroadcast(@NonNull Context context, @Nullable String error_message){
        Intent intent=new Intent(INTENT_ACTION_UPDATE_WIDGET);
        intent.setComponent(new ComponentName(context,FtpWidget.class));
        if(error_message!=null)intent.putExtra(EXTRA_ERROR_MESSAGE,error_message);
        context.sendBroadcast(intent);
    }
}
