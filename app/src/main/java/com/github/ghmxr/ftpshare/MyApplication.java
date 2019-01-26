package com.github.ghmxr.ftpshare;

import android.app.Application;
import android.view.ViewConfiguration;

import java.lang.reflect.Field;

public class MyApplication extends Application{
    @Override
    public void onCreate() {
        try {
            ViewConfiguration config = ViewConfiguration.get(this);
            Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
            if (menuKeyField != null) {
                menuKeyField.setAccessible(true);
                menuKeyField.setBoolean(config, false);
            }
        }
        catch (Exception e) {e.printStackTrace();}
        super.onCreate();
    }
}
