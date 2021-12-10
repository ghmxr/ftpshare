package com.github.ghmxr.ftpshare.activities;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
import com.github.ghmxr.ftpshare.fragments.ClientFragment;
import com.github.ghmxr.ftpshare.fragments.MainFragment;
import com.github.ghmxr.ftpshare.fragments.SettingFragment;
import com.github.ghmxr.ftpshare.services.FtpService;
import com.github.ghmxr.ftpshare.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BaseActivity implements FtpService.OnFTPServiceStatusChangedListener{

    public static MainActivity mainActivity;

    private Menu menu;

    /*private static final int MENU_ACCOUNT_ADD =0;
    private static final int MENU_CLIENT_ADD=1;
    private static final int MENU_ANONYMOUS_SWITCH=2;*/

    public static final String STATE_CURRENT_TAB="current_tab";
    private int current_tab=0;

    private final MainFragment mainFragment=new MainFragment();
    private final ClientFragment clientFragment=new ClientFragment();
    private final SettingFragment settingFragment=new SettingFragment();

    private ViewGroup disconnect_viewgroup;
    private ImageView disconnect_icon;
    private TextView disconnect_tv_value;
    private BottomNavigationView navigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainActivity=this;
        setContentView(R.layout.activity_main);
        FtpService.addOnFtpServiceStatusChangedListener(this);
        navigation = findViewById(R.id.navigation);
        disconnect_viewgroup=findViewById(R.id.main_count);
        disconnect_icon=findViewById(R.id.layout_disconnect_icon);
        disconnect_tv_value =findViewById(R.id.layout_disconnect_value);
        navigation.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                if(R.id.navigation_main==item.getItemId()){
                    setBottomNavAndTab(0);
                    return true;
                }
                else if(R.id.navigation_device==item.getItemId()){
                    setBottomNavAndTab(1);
                    return true;
                }
                else if(R.id.navigation_settings==item.getItemId()){
                    setBottomNavAndTab(2);
                    return true;
                }
                return false;
            }
        });

        if(savedInstanceState==null){
            setBottomNavAndTab(0);
        }else{
            setBottomNavAndTab(savedInstanceState.getInt(STATE_CURRENT_TAB));
        }
        findViewById(R.id.layout_disconnection_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FtpService.disableAutoDisconnectThisTime();
                setAutoDisconnectViewVisibleWithAnim(false);
                Toast.makeText(MainActivity.this,
                        getResources().getString(R.string.toast_cancel_auto_disconnect_att),Toast.LENGTH_SHORT).show();
            }
        });

        final int selectedTab=getIntent().getIntExtra(STATE_CURRENT_TAB,-1);
        if(selectedTab>=0){
            setBottomNavAndTab(selectedTab);
        }

        checkAndShowUploadIntent(getIntent());
    }

    private void setBottomNavAndTab(int current_tab){
        if(current_tab==0){
            this.current_tab=0;
            onNavigationMainSelected();
            navigation.getMenu().getItem(0).setChecked(true);
        }else if(current_tab==1){
            this.current_tab=1;
            onClientDeviceSelected();
            navigation.getMenu().getItem(1).setChecked(true);
        }else if(current_tab==2){
            this.current_tab=2;
            onNavigationAccountSelected();
            navigation.getMenu().getItem(2).setChecked(true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshDisconnectView();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_CURRENT_TAB,current_tab);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        checkAndShowUploadIntent(intent);
    }

    private void checkAndShowUploadIntent(Intent intent){
        if(Intent.ACTION_SEND.equalsIgnoreCase(intent.getAction())){
            navigation.getMenu().getItem(1).setChecked(true);
            onClientDeviceSelected();
            Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if(uri==null){
                Toast.makeText(this,getResources().getString(R.string.share_invalid_data_source),Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            ArrayList<Uri>uris=new ArrayList<>();
            uris.add(uri);
            clientFragment.setSharingUriAndView(uris);
            Toast.makeText(this,getResources().getString(R.string.share_select_server),Toast.LENGTH_LONG).show();
        }

        if(Intent.ACTION_SEND_MULTIPLE.equalsIgnoreCase(intent.getAction())){
            navigation.getMenu().getItem(1).setChecked(true);
            onClientDeviceSelected();
            List<Uri> uris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            if(uris==null){
                Toast.makeText(this,getResources().getString(R.string.share_invalid_data_source),Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            clientFragment.setSharingUriAndView(uris);
            Toast.makeText(this,getResources().getString(R.string.share_select_server),Toast.LENGTH_LONG).show();
        }
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

    public void refreshDisconnectView(){
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
            getSupportActionBar().setTitle(getResources().getString(R.string.app_name));
            refreshMenuVisibilitiesForCurrentTab();
        }catch (Exception e){e.printStackTrace();}

    }

    private void onClientDeviceSelected(){
        try{
            getSupportFragmentManager().beginTransaction().replace(R.id.main_frame,clientFragment).commit();
            getSupportActionBar().setTitle(getResources().getString(R.string.title_device));
            refreshMenuVisibilitiesForCurrentTab();
        }catch (Exception e){e.printStackTrace();}
    }

    private void onNavigationAccountSelected(){
        try{
            getSupportFragmentManager().beginTransaction().replace(R.id.main_frame,settingFragment).commit();
            getSupportActionBar().setTitle(getResources().getString(R.string.title_settings));
            refreshMenuVisibilitiesForCurrentTab();
        }catch (Exception e){e.printStackTrace();}

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mainFragment.processingActivityResult(requestCode,resultCode,data);
        //accountFragment.processingActivityResult(requestCode,resultCode,data);
        clientFragment.processActivityResult(requestCode,resultCode,data);
        settingFragment.processActivityResult(requestCode,resultCode,data);
    }

    private void refreshMenuVisibilitiesForCurrentTab(){
        if(menu==null)return;
        if(current_tab==0){
            menu.getItem(0).setVisible(false);

        }else if(current_tab==1){
            menu.getItem(0).setVisible(true);

        }else if(current_tab==2){
            menu.getItem(0).setVisible(false);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main,menu);
        this.menu=menu;
        refreshMenuVisibilitiesForCurrentTab();
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId()==R.id.action_client_add){
            startActivityForResult(new Intent(this,AddClientActivity.class),1);
        }
        if(item.getItemId()==R.id.action_main_about){
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
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mainActivity=null;
        FtpService.removeOnFtpServiceStatusChangedListener(this);
    }

    public static void killInstanceAndRestart(@NonNull Activity activity){
        if(mainActivity!=null){
            mainActivity.finish();
        }
        activity.startActivity(new Intent(activity,MainActivity.class));
    }
}
