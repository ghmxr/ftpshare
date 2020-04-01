package com.github.ghmxr.ftpshare.activities;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.ghmxr.ftpshare.Constants;
import com.github.ghmxr.ftpshare.R;
import com.github.ghmxr.ftpshare.fragments.AccountFragment;
import com.github.ghmxr.ftpshare.fragments.MainFragment;
import com.github.ghmxr.ftpshare.services.FtpService;
import com.github.ghmxr.ftpshare.utils.CommonUtils;

public class MainActivity extends BaseActivity implements FtpService.OnFTPServiceStatusChangedListener{

    private Menu menu;

    private static int MENU_ACCOUNT_ADD =0;
    private static int MENU_ANONYMOUS_SWITCH=1;

    private final MainFragment mainFragment=new MainFragment();
    private final AccountFragment accountFragment=new AccountFragment();

    private ViewGroup disconnect_viewgroup;
    private ImageView disconnect_icon;
    private TextView disconnect_tv_value;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FtpService.addOnFtpServiceStatusChangedListener(this);
        BottomNavigationView navigation = findViewById(R.id.navigation);
        disconnect_viewgroup=findViewById(R.id.main_count);
        disconnect_icon=findViewById(R.id.layout_disconnect_icon);
        disconnect_tv_value =findViewById(R.id.layout_disconnect_value);
        navigation.setSelectedItemId(R.id.navigation_main);
        navigation.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.navigation_main:
                        onNavigationMainSelected();
                        return true;
                    case R.id.navigation_settings:
                        onNavigationAccountSelected();
                        return true;
                }
                return false;
            }
        });
        onNavigationMainSelected();
        findViewById(R.id.layout_disconnection_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FtpService.disableAutoDisconnectThisTime();
                setAutoDisconnectViewVisibleWithAnim(false);
                Toast.makeText(MainActivity.this,
                        getResources().getString(R.string.toast_cancel_auto_disconnect_att),Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshDisconnectView();
    }

    @Override
    public void onFTPServiceStarted() {
        refreshDisconnectView();
    }

    @Override
    public void onFTPServiceStartError(Exception e) {}

    @Override
    public void onRemainingSeconds(int seconds) {
        if(disconnect_viewgroup.getVisibility()!=View.VISIBLE){
            disconnect_icon.setImageResource(R.drawable.icon_alarm);
            disconnect_viewgroup.startAnimation(AnimationUtils.loadAnimation(this,R.anim.entry_300));
            disconnect_viewgroup.setVisibility(View.VISIBLE);
        }
        disconnect_tv_value.setText(CommonUtils.getDisplayTimeOfSeconds(seconds));
    }

    @Override
    public void onFTPServiceDestroyed() {
        setAutoDisconnectViewVisibleWithAnim(false);
    }

    private void refreshDisconnectView(){
        if(!FtpService.isFTPServiceRunning()||FtpService.getIsIgnoreAutoCancelThisTime()){
            disconnect_viewgroup.setVisibility(View.GONE);
            return;
        }

        int disconnect_value=CommonUtils.getSettingSharedPreferences(this)
                .getInt(Constants.PreferenceConsts.AUTO_STOP,Constants.PreferenceConsts.AUTO_STOP_DEFAULT);
        switch (disconnect_value){
            default:break;
            case Constants.PreferenceConsts.AUTO_STOP_NONE:{
                disconnect_viewgroup.setVisibility(View.GONE);
            }
            break;
            case Constants.PreferenceConsts.AUTO_STOP_TIME_COUNT:{
                setAutoDisconnectViewVisibleWithAnim(true);
                disconnect_icon.setImageResource(R.drawable.icon_alarm);
                disconnect_tv_value.setText(CommonUtils.getDisplayTimeOfSeconds(FtpService.getTimeCounts()));
            }
            break;
            case Constants.PreferenceConsts.AUTO_STOP_WIFI_DISCONNECTED:{
                setAutoDisconnectViewVisibleWithAnim(true);
                disconnect_icon.setImageResource(R.drawable.icon_wifi_disconnected);
                disconnect_tv_value.setText(getResources().getString(R.string.setting_disconnect_wifi_disconnected));
            }
            break;
            case Constants.PreferenceConsts.AUTO_STOP_AP_DISCONNECTED:{
                setAutoDisconnectViewVisibleWithAnim(true);
                disconnect_icon.setImageResource(R.drawable.icon_ap_disconnected);
                disconnect_tv_value.setText(getResources().getString(R.string.setting_disconnect_ap_disconnected));
            }
            break;
        }
    }

    private void setAutoDisconnectViewVisibleWithAnim(boolean b){
        if(b){
            if(disconnect_viewgroup.getVisibility()!=View.VISIBLE){
                disconnect_viewgroup.startAnimation(AnimationUtils.loadAnimation(this,R.anim.entry_300));
            }
            disconnect_viewgroup.setVisibility(View.VISIBLE);
        }else{
            if(disconnect_viewgroup.getVisibility()!=View.GONE){
                disconnect_viewgroup.startAnimation(AnimationUtils.loadAnimation(this,R.anim.exit_300));
                disconnect_viewgroup.setVisibility(View.GONE);
            }
        }
    }

    private void onNavigationMainSelected(){
        try{
            getSupportFragmentManager().beginTransaction().replace(R.id.main_frame,mainFragment).commit();
            menu.getItem(MENU_ACCOUNT_ADD).setVisible(false);
            menu.getItem(MENU_ANONYMOUS_SWITCH).setVisible(false);
            getSupportActionBar().setTitle(getResources().getString(R.string.app_name));
        }catch (Exception e){e.printStackTrace();}

    }

    private void onNavigationAccountSelected(){
        try{
            getSupportFragmentManager().beginTransaction().replace(R.id.main_frame,accountFragment).commit();
            menu.getItem(MENU_ACCOUNT_ADD).setVisible(!CommonUtils.isAnonymousMode(this));
            menu.getItem(MENU_ANONYMOUS_SWITCH).setVisible(true);
            getSupportActionBar().setTitle(getResources().getString(R.string.title_settings));
        }catch (Exception e){e.printStackTrace();}

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main,menu);
        menu.getItem(MENU_ANONYMOUS_SWITCH).setTitle(CommonUtils.isAnonymousMode(this)?getResources().getString(R.string.action_main_anonymous_opened):getResources().getString(R.string.action_main_anonymous_closed));
        this.menu=menu;
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            default:break;
            case R.id.action_main_add:{
                if(FtpService.isFTPServiceRunning()){
                    CommonUtils.showSnackBarOfFtpServiceIsRunning(this);
                    return true;
                }
                startActivityForResult(new Intent(this,AddAccountActivity.class),1);
                return true;
            }
            case R.id.action_main_anonymous_switch:{
                if(FtpService.isFTPServiceRunning()){
                    CommonUtils.showSnackBarOfFtpServiceIsRunning(this);
                    return true;
                }
                try{
                    SharedPreferences settings=getSharedPreferences(Constants.PreferenceConsts.FILE_NAME, Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor=settings.edit();
                    boolean isAnonymousMode=settings.getBoolean(Constants.PreferenceConsts.ANONYMOUS_MODE,Constants.PreferenceConsts.ANONYMOUS_MODE_DEFAULT);
                    editor.putBoolean(Constants.PreferenceConsts.ANONYMOUS_MODE,!isAnonymousMode);
                    editor.apply();
                    menu.getItem(MENU_ANONYMOUS_SWITCH).setTitle((!isAnonymousMode)?getResources().getString(R.string.action_main_anonymous_opened):getResources().getString(R.string.action_main_anonymous_closed));
                    menu.getItem(MENU_ACCOUNT_ADD).setVisible(isAnonymousMode);
                    accountFragment.refreshContents();
                }catch (Exception e){e.printStackTrace();}
            }
            break;
            case R.id.action_main_about:{
                View dialogView=LayoutInflater.from(this).inflate(R.layout.layout_dialog_about,null);
                dialogView.findViewById(R.id.donate).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try{
                            startActivity(Intent.parseUri("https://qr.alipay.com/FKX08041Y09ZGT6ZT91FA5",Intent.URI_INTENT_SCHEME));
                        }
                        catch (Exception e){
                            e.printStackTrace();
                            Toast.makeText(MainActivity.this,e.toString(),Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                new AlertDialog.Builder(this)
                        .setTitle(CommonUtils.getAppName(this)+"("+CommonUtils.getAppVersionName(this)+")")
                        .setView(dialogView)
                        .setPositiveButton(getResources().getString(R.string.dialog_button_confirm), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        })
                        .show();
            }
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        FtpService.removeOnFtpServiceStatusChangedListener(this);
    }
}
