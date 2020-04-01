package com.github.ghmxr.ftpshare.activities;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;

import com.github.ghmxr.ftpshare.Constants;
import com.github.ghmxr.ftpshare.utils.CommonUtils;

import java.util.Locale;

public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle bundle){
        super.onCreate(bundle);
        setAndRefreshLanguage();
    }

    @Override
    public void finish() {
        super.finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void setAndRefreshLanguage(){
        // 获得res资源对象
        Resources resources = getResources();
        // 获得屏幕参数：主要是分辨率，像素等。
        DisplayMetrics metrics = resources.getDisplayMetrics();
        // 获得配置对象
        Configuration config = resources.getConfiguration();
        //区别17版本（其实在17以上版本通过 config.locale设置也是有效的，不知道为什么还要区别）
        //在这里设置需要转换成的语言，也就是选择用哪个values目录下的strings.xml文件
        int value= CommonUtils.getSettingSharedPreferences(this).getInt(Constants.PreferenceConsts.LANGUAGE_SETTING,Constants.PreferenceConsts.LANGUAGE_FOLLOW_SYSTEM);
        Locale locale=null;
        switch (value){
            default:break;
            case Constants.PreferenceConsts.LANGUAGE_FOLLOW_SYSTEM:locale=Locale.getDefault();break;
            case Constants.PreferenceConsts.LANGUAGE_SIMPLIFIED_CHINESE:locale=Locale.SIMPLIFIED_CHINESE;break;
            case Constants.PreferenceConsts.LANGUAGE_ENGLISH:locale=Locale.ENGLISH;break;
        }
        if(locale==null)return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            config.setLocale(locale);
        } else {
            config.locale =locale;
        }
        resources.updateConfiguration(config, metrics);
    }
}
