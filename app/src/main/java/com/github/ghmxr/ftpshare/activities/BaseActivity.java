package com.github.ghmxr.ftpshare.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import java.util.LinkedList;

public abstract class BaseActivity extends AppCompatActivity {
    public static final LinkedList<BaseActivity> activityStack=new LinkedList<>();
    @Override
    protected void onCreate(Bundle bundle){
        super.onCreate(bundle);
        if(!activityStack.contains(this))activityStack.add(this);
    }

    @Override
    public void finish() {
        super.finish();
        activityStack.remove(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        activityStack.remove(this);
    }
}
