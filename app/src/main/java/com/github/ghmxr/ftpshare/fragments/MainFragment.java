package com.github.ghmxr.ftpshare.fragments;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.text.method.DigitsKeyListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.ghmxr.ftpshare.Constants;
import com.github.ghmxr.ftpshare.MyApplication;
import com.github.ghmxr.ftpshare.R;
import com.github.ghmxr.ftpshare.activities.ServiceAccountActivity;
import com.github.ghmxr.ftpshare.services.FtpService;
import com.github.ghmxr.ftpshare.ui.FtpAddressesDialog;
import com.github.ghmxr.ftpshare.utils.CommonUtils;
import com.github.ghmxr.ftpshare.utils.NetworkStatusMonitor;

public class MainFragment extends Fragment implements View.OnClickListener,FtpService.OnFTPServiceStatusChangedListener,
        NetworkStatusMonitor.NetworkStatusCallback {

    ViewGroup viewGroup_main,viewGroup_port,viewGroup_charset,viewGroup_wakelock,viewGroup_battery,viewGroup_more,viewGroup_addresses;
    CompoundButton switchCompat;
    TextView tv_main,tv_port,tv_charset,tv_login_mode;
    CheckBox cb_wakelock;
    ScrollView scrollView;

    public static final String ACTION_FLASH_ACCOUNT_ITEM=MyApplication.getGlobalBaseContext().getPackageName()+":action_flash_account_item";

    final SharedPreferences settings=CommonUtils.getSettingSharedPreferences(MyApplication.getGlobalBaseContext());
    final SharedPreferences.Editor editor=settings.edit();

    private final BroadcastReceiver flashItemReceiver=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(viewGroup_more!=null && getActivity()!=null){
                viewGroup_more.clearAnimation();
                scrollView.smoothScrollTo(0,scrollView.getHeight());
                AlphaAnimation alphaAnimation1 = new AlphaAnimation(1.0f, 0.3f);
                alphaAnimation1.setDuration(800);
                alphaAnimation1.setRepeatCount(2);
                alphaAnimation1.setRepeatMode(Animation.RESTART);
                viewGroup_more.setAnimation(alphaAnimation1);
                alphaAnimation1.start();
            }
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FtpService.addOnFtpServiceStatusChangedListener(this);
        NetworkStatusMonitor.addNetworkStatusCallback(this);
        try{
            getContext().registerReceiver(flashItemReceiver,new IntentFilter(ACTION_FLASH_ACCOUNT_ITEM));
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main,container,false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        scrollView=view.findViewById(R.id.main_frame_sr);
        viewGroup_main=view.findViewById(R.id.main_area);
        viewGroup_port=view.findViewById(R.id.port_area);
        viewGroup_charset=view.findViewById(R.id.charset_area);
        viewGroup_wakelock=view.findViewById(R.id.wakelock_area);
        viewGroup_battery=view.findViewById(R.id.battery_area);
        viewGroup_more=view.findViewById(R.id.setting_area);
        viewGroup_addresses=view.findViewById(R.id.ftp_addresses_area);
        switchCompat=view.findViewById(R.id.main_switch);
        tv_main=view.findViewById(R.id.main_att);
        tv_port=view.findViewById(R.id.port_att);
        tv_charset=view.findViewById(R.id.charset_att);
        cb_wakelock=view.findViewById(R.id.wakelock_cb);
        tv_login_mode=view.findViewById(R.id.setting_area_value);

        viewGroup_main.setOnClickListener(this);
        viewGroup_port.setOnClickListener(this);
        viewGroup_charset.setOnClickListener(this);
        viewGroup_wakelock.setOnClickListener(this);
        viewGroup_battery.setOnClickListener(this);
        viewGroup_addresses.setOnClickListener(this);
        viewGroup_more.setOnClickListener(this);
        view.findViewById(R.id.login_connect_num_area).setOnClickListener(this);
        view.findViewById(R.id.anonymous_connect_num_area).setOnClickListener(this);
        //refreshContents();
    }

    @Override
    public void onResume() {
        super.onResume();
        //refreshBatteryIgnoreStatus();
        refreshContents();
    }

    @Override
    public void onClick(View v){
        if(getContext()==null||getActivity()==null||getView()==null)return;
        switch(v.getId()){
            default:break;
            case R.id.main_area:{
                if(!FtpService.isFTPServiceRunning()){
                    if(FtpService.startService(getActivity())){
                        viewGroup_main.setClickable(false);
                        switchCompat.setChecked(true);
                        switchCompat.setEnabled(false);
                        tv_main.setText(getResources().getString(R.string.attention_opening_ftp));
                    }
                }else{
                    viewGroup_main.setClickable(false);
                    switchCompat.setChecked(false);
                    switchCompat.setEnabled(false);
                    tv_main.setText(getResources().getString(R.string.attention_closing_ftp));
                    FtpService.stopService();
                }
            }
            break;
            case R.id.port_area:{
                if(FtpService.isFTPServiceRunning()){
                    CommonUtils.showSnackBarOfFtpServiceIsRunning(getActivity());
                    return;
                }
                View dialogView=LayoutInflater.from(getActivity()).inflate(R.layout.layout_with_edittext,null);
                final EditText edit=dialogView.findViewById(R.id.dialog_edittext);
                edit.setInputType(InputType.TYPE_CLASS_NUMBER);
                edit.setSingleLine(true);
                edit.setHint(getResources().getString(R.string.item_port_hint));
                edit.setText(String.valueOf(settings.getInt(Constants.PreferenceConsts.PORT_NUMBER,Constants.PreferenceConsts.PORT_NUMBER_DEFAULT)));
                final AlertDialog dialog=new AlertDialog.Builder(getActivity())
                        .setTitle(getResources().getString(R.string.item_port))
                        .setView(dialogView)
                        .setPositiveButton(getResources().getString(R.string.dialog_button_confirm),null)
                        .setNegativeButton(getResources().getString(R.string.dialog_button_cancel), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        })
                        .show();
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int port=5656;
                        try{
                            port=Integer.parseInt(edit.getText().toString().trim());
                            if(!(port>=1024&&port<=65535)){
                                Toast.makeText(getActivity(),getResources().getString(R.string.attention_port_number_out_of_range),Toast.LENGTH_SHORT).show();
                                return;
                            }
                        }catch (Exception e){
                            e.printStackTrace();
                            Toast.makeText(getActivity(),getResources().getString(R.string.attention_invalid_port_number),Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if(FtpService.isFTPServiceRunning()){
                            Toast.makeText(getActivity(),getResources().getString(R.string.attention_ftp_is_running),Toast.LENGTH_SHORT).show();
                            return;
                        }
                        editor.putInt(Constants.PreferenceConsts.PORT_NUMBER,port);
                        editor.apply();
                        dialog.cancel();
                        tv_port.setText(String.valueOf(port));
                    }
                });
            }
            break;
            case R.id.charset_area:{
                if(FtpService.isFTPServiceRunning()){
                    CommonUtils.showSnackBarOfFtpServiceIsRunning(getActivity());
                    return;
                }
                final AlertDialog dialog=new AlertDialog.Builder(getActivity())
                        .setTitle(getResources().getString(R.string.item_charset))
                        .setView(R.layout.layout_dialog_charset)
                        .show();
                RadioButton ra_utf=dialog.findViewById(R.id.charset_selection_utf);
                RadioButton ra_gbk=dialog.findViewById(R.id.charset_selection_gbk);
                String selection=settings.getString(Constants.PreferenceConsts.CHARSET_TYPE,Constants.PreferenceConsts.CHARSET_TYPE_DEFAULT);
                if(ra_utf!=null)ra_utf.setChecked(Constants.Charset.CHAR_UTF.equals(selection));
                if(ra_gbk!=null)ra_gbk.setChecked(Constants.Charset.CHAR_GBK.equals(selection));
                if(ra_utf!=null)ra_utf.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        editor.putString(Constants.PreferenceConsts.CHARSET_TYPE,Constants.Charset.CHAR_UTF);
                        editor.apply();
                        dialog.cancel();
                        if(getActivity()!=null)tv_charset.setText(CommonUtils.getDisplayCharsetValue(getActivity()));
                    }
                });
                if(ra_gbk!=null)ra_gbk.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        editor.putString(Constants.PreferenceConsts.CHARSET_TYPE,Constants.Charset.CHAR_GBK);
                        editor.apply();
                        dialog.cancel();
                        if(getActivity()!=null)tv_charset.setText(CommonUtils.getDisplayCharsetValue(getActivity()));
                    }
                });
            }
            break;
            case R.id.wakelock_area:{
                if(settings.getBoolean(Constants.PreferenceConsts.WAKE_LOCK,Constants.PreferenceConsts.WAKE_LOCK_DEFAULT)){
                    cb_wakelock.setChecked(false);
                    FtpService.sendEmptyMessage(FtpService.MESSAGE_WAKELOCK_RELEASE);
                    editor.putBoolean(Constants.PreferenceConsts.WAKE_LOCK,false);
                    editor.apply();
                }else{
                    cb_wakelock.setChecked(true);
                    FtpService.sendEmptyMessage(FtpService.MESSAGE_WAKELOCK_ACQUIRE);
                    editor.putBoolean(Constants.PreferenceConsts.WAKE_LOCK,true);
                    editor.apply();
                }
            }
            break;
            case R.id.setting_area:{
                getActivity().startActivityForResult(new Intent(getActivity(), ServiceAccountActivity.class),0);
            }
            break;
            case R.id.ftp_addresses_area:{
                if(!FtpService.isFTPServiceRunning()){
                    return;
                }
                new FtpAddressesDialog(getActivity()).show();
            }
            break;
            case R.id.anonymous_connect_num_area:{
                if(FtpService.isFTPServiceRunning()){
                    CommonUtils.showSnackBarOfFtpServiceIsRunning(getActivity());
                    return;
                }
                inlineMethod(R.string.item_anonymous_num,Constants.PreferenceConsts.MAX_ANONYMOUS_NUM);
            }
            break;
            case R.id.login_connect_num_area:{
                if(FtpService.isFTPServiceRunning()){
                    CommonUtils.showSnackBarOfFtpServiceIsRunning(getActivity());
                    return;
                }
                inlineMethod(R.string.item_login_num,Constants.PreferenceConsts.MAX_LOGIN_NUM);
            }
            break;
        }
    }

    private void inlineMethod(int var1,String var2){
        final AlertDialog dialog=createAndShowEditDialog();
        dialog.setTitle(getResources().getString(var1));
        dialog.show();
        EditText editText=dialog.<EditText>findViewById(R.id.dialog_edittext);
        editText.setKeyListener(DigitsKeyListener.getInstance("1234567890"));
        editText.setText(String.valueOf(settings.getInt(var2,10)));
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                int var;
                try{
                    var=Integer.parseInt(editText.getText().toString());
                }catch (Exception e){
                    e.printStackTrace();
                    Toast.makeText(getContext(),getResources().getString(R.string.client_connect_timeout_invalid_value),Toast.LENGTH_SHORT).show();
                    return;
                }
                if(var<=0){
                    Toast.makeText(getContext(),getResources().getString(R.string.client_connect_timeout_invalid_value),Toast.LENGTH_SHORT).show();
                    return;
                }
                settings.edit().putInt(var2,var).apply();
                dialog.cancel();
                refreshContents();
            }
        });
    }

    private AlertDialog createAndShowEditDialog(){
        View dialogView=LayoutInflater.from(getActivity()).inflate(R.layout.layout_with_edittext,null);
        final EditText edit=dialogView.findViewById(R.id.dialog_edittext);
        edit.setInputType(InputType.TYPE_CLASS_NUMBER);
        edit.setSingleLine(true);
        edit.setHint(getResources().getString(R.string.item_port_hint));
        edit.setText(String.valueOf(settings.getInt(Constants.PreferenceConsts.PORT_NUMBER,Constants.PreferenceConsts.PORT_NUMBER_DEFAULT)));
        final AlertDialog dialog=new AlertDialog.Builder(getActivity())
                .setTitle(getResources().getString(R.string.item_port))
                .setView(dialogView)
                .setPositiveButton(getResources().getString(R.string.dialog_button_confirm),null)
                .setNegativeButton(getResources().getString(R.string.dialog_button_cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .create();
        return dialog;
    }

    @Override
    public void onFTPServiceStarted() {
        if(getActivity()==null||getContext()==null||getView()==null)return;
        switchCompat.setEnabled(true);
        switchCompat.setChecked(true);
        tv_main.setText(getResources().getString(R.string.ftp_status_running_head)+ CommonUtils.getFTPServiceDisplayAddress(getActivity()));
        viewGroup_main.setClickable(true);
        viewGroup_addresses.setVisibility(View.VISIBLE);
    }

    @Override
    public void onFTPServiceStartError(Exception e) {
        if(getActivity()==null||getContext()==null||getView()==null)return;
        switchCompat.setEnabled(true);
        switchCompat.setChecked(false);
        tv_main.setText(getResources().getString(R.string.ftp_status_not_running));
        Snackbar.make(getActivity().findViewById(android.R.id.content),e.toString(),Snackbar.LENGTH_SHORT).show();
        viewGroup_main.setClickable(true);
    }

    @Override
    public void onRemainingSeconds(int seconds) {}

    @Override
    public void onFTPServiceDestroyed() {
        if(getActivity()==null||getContext()==null||getView()==null)return;
        switchCompat.setChecked(false);
        switchCompat.setEnabled(true);
        tv_main.setText(getResources().getString(R.string.ftp_status_not_running));
        viewGroup_main.setClickable(true);
        viewGroup_addresses.setVisibility(View.GONE);
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        FtpService.removeOnFtpServiceStatusChangedListener(this);
        NetworkStatusMonitor.removeNetworkStatusCallback(this);
        try{
            getContext().unregisterReceiver(flashItemReceiver);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onNetworkStatusRefreshed() {
        tv_main.setText(FtpService.getFTPStatusDescription(MyApplication.getGlobalBaseContext()));
    }

    @Override
    public void onNetworkConnected(NetworkStatusMonitor.NetworkType networkType) {}

    @Override
    public void onNetworkDisconnected(NetworkStatusMonitor.NetworkType networkType) {}

    public void processingActivityResult(int requestCode, int resultCode, Intent data) {
        /*if(requestCode==0&&resultCode== Activity.RESULT_OK&&getActivity()!=null){
            getActivity().recreate();
        }*/
        refreshContents();
        //refreshBatteryIgnoreStatus();
    }

    private void refreshContents(){
        if(getContext()==null||getActivity()==null||getView()==null)return;
        //switchCompat.post(() -> switchCompat.setChecked(FtpService.isFTPServiceRunning()));
        switchCompat.setChecked(FtpService.isFTPServiceRunning());
        tv_main.setText(FtpService.getFTPStatusDescription(getActivity()));
        tv_port.setText(String.valueOf(settings.getInt(Constants.PreferenceConsts.PORT_NUMBER,Constants.PreferenceConsts.PORT_NUMBER_DEFAULT)));
        tv_charset.setText(CommonUtils.getDisplayCharsetValue(getActivity()));
        cb_wakelock.setChecked(settings.getBoolean(Constants.PreferenceConsts.WAKE_LOCK,Constants.PreferenceConsts.WAKE_LOCK_DEFAULT));
        viewGroup_addresses.setVisibility(FtpService.isFTPServiceRunning()?View.VISIBLE:View.GONE);
        tv_login_mode.setText(getContext().getResources().getString(CommonUtils.isAnonymousMode(getContext())?
                R.string.item_account_settings_anonymous_mode:R.string.item_account_settings_login_mode));
        refreshBatteryIgnoreStatus();
        getView().<TextView>findViewById(R.id.anonymous_connect_num).setText(settings.getInt(Constants.PreferenceConsts.MAX_ANONYMOUS_NUM,10)+"");
        getView().<TextView>findViewById(R.id.login_connect_num).setText(settings.getInt(Constants.PreferenceConsts.MAX_LOGIN_NUM,10)+"");
    }

    private void refreshBatteryIgnoreStatus(){
        if(viewGroup_battery==null)return;
        if(getContext()==null)return;
        try{
            if(Build.VERSION.SDK_INT>=23){
                PowerManager powerManager=(PowerManager)getContext().getSystemService(Context.POWER_SERVICE);
                if(!powerManager.isIgnoringBatteryOptimizations(getContext().getPackageName())){
                    viewGroup_battery.setVisibility(View.VISIBLE);
                    viewGroup_battery.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                            intent.setData(Uri.parse("package:"+getContext().getPackageName()));
                            startActivity(intent);
                        }
                    });
                }else {
                    viewGroup_battery.setVisibility(View.GONE);
                }
            }
        }catch (Exception e){e.printStackTrace();}
    }

}
