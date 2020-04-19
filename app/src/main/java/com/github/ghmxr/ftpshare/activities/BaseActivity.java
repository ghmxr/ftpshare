package com.github.ghmxr.ftpshare.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.github.ghmxr.ftpshare.utils.CommonUtils;

public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle bundle){
        super.onCreate(bundle);
        setAndRefreshLanguage();
    }

    public void setAndRefreshLanguage(){
        CommonUtils.updateResourcesOfContext(this);
    }
}
