package com.github.ghmxr.ftpshare.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.github.ghmxr.ftpshare.utils.CommonUtils;

public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setAndRefreshLanguage();
    }

    public void setAndRefreshLanguage() {
        CommonUtils.updateResourcesOfContext(this);
    }
}
