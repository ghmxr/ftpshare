package com.github.ghmxr.ftpshare.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.github.ghmxr.ftpshare.Constants;
import com.github.ghmxr.ftpshare.services.FtpService;
import com.github.ghmxr.ftpshare.utils.CommonUtils;

public class BootCompletedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if(Intent.ACTION_BOOT_COMPLETED.equalsIgnoreCase(intent.getAction())){
            if(CommonUtils.getSettingSharedPreferences(context)
                    .getBoolean(Constants.PreferenceConsts.START_AFTER_BOOT,Constants.PreferenceConsts.START_AFTER_BOOT_DEFAULT)){
                FtpService.startService(context);
            }
        }

    }
}
