package com.github.ghmxr.ftpshare.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.github.ghmxr.ftpshare.R;
import com.github.ghmxr.ftpshare.activities.MainActivity;
import com.github.ghmxr.ftpshare.services.FtpService;

public class FtpWidget extends AppWidgetProvider implements FtpService.OnFTPServiceStatusChangedListener {

    private Context context;

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        this.context=context;
        FtpService.addOnFtpServiceStatusChangedListener(this);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        appWidgetManager.updateAppWidget(new ComponentName(context,FtpWidget.class),getCurrentRemoteView(context));
    }

    @Override
    public void onFTPServiceStarted() {
        if(context==null)return;
        AppWidgetManager.getInstance(context).updateAppWidget(new ComponentName(context,FtpWidget.class),getCurrentRemoteView(context));
    }

    @Override
    public void onFTPServiceStartError(Exception e) {
        if(context==null)return;
        Toast.makeText(context,String.valueOf(e),Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRemainingSeconds(int seconds) {}

    @Override
    public void onFTPServiceDestroyed() {
        if(context==null)return;
        AppWidgetManager.getInstance(context).updateAppWidget(new ComponentName(context,FtpWidget.class),getCurrentRemoteView(context));
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

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        FtpService.removeOnFtpServiceStatusChangedListener(this);
    }

    public static class FtpWidgetReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean b=FtpService.isFTPServiceRunning();
            if(b)FtpService.stopService();
            else FtpService.startService(context);
        }
    }

}
