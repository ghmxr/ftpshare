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

    @Override
    public void finish() {
        super.finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void setAndRefreshLanguage(){
        CommonUtils.updateResourcesOfContext(this);
    }
}
