package com.github.ghmxr.ftpshare.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import java.util.LinkedList;

public abstract class BaseActivity extends AppCompatActivity {
    public static LinkedList<BaseActivity> activityStack;
    @Override
    protected void onCreate(Bundle bundle){
        super.onCreate(bundle);
        if(activityStack==null) activityStack=new LinkedList<>();
        if(!activityStack.contains(this))activityStack.add(this);
        //Log.d("activityStack","the size is "+activityStack.size());
    }
    @Override
    public void finish(){
        super.finish();
        if(activityStack!=null&&activityStack.contains(this)) {
            activityStack.remove(this);
            if(activityStack.size()==0) activityStack=null;
        }
    }
}
