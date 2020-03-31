package com.github.ghmxr.ftpshare.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Network;
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
import android.support.v7.widget.SwitchCompat;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.github.ghmxr.ftpshare.Constants;
import com.github.ghmxr.ftpshare.MyApplication;
import com.github.ghmxr.ftpshare.R;
import com.github.ghmxr.ftpshare.activities.SettingActivity;
import com.github.ghmxr.ftpshare.services.FtpService;
import com.github.ghmxr.ftpshare.utils.CommonUtils;
import com.github.ghmxr.ftpshare.utils.NetworkEnvironmentUtil;
import com.github.ghmxr.ftpshare.utils.NetworkStatusMonitor;

import java.util.List;

public class MainFragment extends Fragment implements View.OnClickListener,FtpService.OnFTPServiceStatusChangedListener,
        NetworkStatusMonitor.NetworkStatusCallback {

    ViewGroup viewGroup_main,viewGroup_port,viewGroup_charset,viewGroup_wakelock,viewGroup_battery,viewGroup_more;
    SwitchCompat switchCompat;
    TextView tv_main,tv_port,tv_charset,tv_ips;
    CheckBox cb_wakelock;

    final SharedPreferences settings=CommonUtils.getSettingSharedPreferences();
    final SharedPreferences.Editor editor=settings.edit();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main,container,false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        FtpService.addOnFtpServiceStatusChangedListener(this);
        viewGroup_main=view.findViewById(R.id.main_area);
        viewGroup_port=view.findViewById(R.id.port_area);
        viewGroup_charset=view.findViewById(R.id.charset_area);
        viewGroup_wakelock=view.findViewById(R.id.wakelock_area);
        viewGroup_battery=view.findViewById(R.id.battery_area);
        viewGroup_more=view.findViewById(R.id.setting_area);
        switchCompat=view.findViewById(R.id.main_switch);
        tv_main=view.findViewById(R.id.main_att);
        tv_port=view.findViewById(R.id.port_att);
        tv_charset=view.findViewById(R.id.charset_att);
        tv_ips=view.findViewById(R.id.ftp_addresses);
        cb_wakelock=view.findViewById(R.id.wakelock_cb);

        viewGroup_main.setOnClickListener(this);
        viewGroup_port.setOnClickListener(this);
        viewGroup_charset.setOnClickListener(this);
        viewGroup_wakelock.setOnClickListener(this);
        viewGroup_battery.setOnClickListener(this);
        viewGroup_more.setOnClickListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        if(getContext()==null||getActivity()==null||getView()==null)return;
        switchCompat.setChecked(FtpService.isFTPServiceRunning());
        tv_main.setText(FtpService.getFTPStatusDescription(MyApplication.getGlobalBaseContext()));
        tv_port.setText(String.valueOf(settings.getInt(Constants.PreferenceConsts.PORT_NUMBER,Constants.PreferenceConsts.PORT_NUMBER_DEFAULT)));
        tv_charset.setText(CommonUtils.getDisplayCharsetValue());
        cb_wakelock.setChecked(settings.getBoolean(Constants.PreferenceConsts.WAKE_LOCK,Constants.PreferenceConsts.WAKE_LOCK_DEFAULT));
        refreshBatteryIgnoreStatus();
        refreshIpViews();
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
                        tv_charset.setText(CommonUtils.getDisplayCharsetValue());
                    }
                });
                if(ra_gbk!=null)ra_gbk.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        editor.putString(Constants.PreferenceConsts.CHARSET_TYPE,Constants.Charset.CHAR_GBK);
                        editor.apply();
                        dialog.cancel();
                        tv_charset.setText(CommonUtils.getDisplayCharsetValue());
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
                startActivityForResult(new Intent(getActivity(), SettingActivity.class),0);
            }
            break;
        }
    }

    @Override
    public void onFTPServiceStarted() {
        if(getActivity()==null||getContext()==null||getView()==null)return;
        switchCompat.setEnabled(true);
        switchCompat.setChecked(true);
        tv_main.setText(getResources().getString(R.string.ftp_status_running_head)+ CommonUtils.getFTPServiceDisplayAddress(getActivity()));
        viewGroup_main.setClickable(true);
        refreshIpViews();
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
        refreshIpViews();
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        FtpService.removeOnFtpServiceStatusChangedListener(this);
    }

    @Override
    public void onNetworkStatusRefreshed() {
        refreshIpViews();
    }

    @Override
    public void onNetworkConnected(NetworkStatusMonitor.NetworkType networkType) {}

    @Override
    public void onNetworkDisconnected(NetworkStatusMonitor.NetworkType networkType) {}

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==0&&resultCode== Activity.RESULT_OK&&getActivity()!=null){
            getActivity().recreate();
        }
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

    private void refreshIpViews(){
        if(!FtpService.isFTPServiceRunning()){
            tv_ips.setText("");
        }else{
            StringBuilder stringBuilder=new StringBuilder();
            List<String> ips= NetworkEnvironmentUtil.getLocalIpv4Addresses();
            for(String ip:ips){
                if(stringBuilder.length()>0)stringBuilder.append("\n");
                stringBuilder.append("ftp://"+ip+":"+CommonUtils.getSettingSharedPreferences()
                        .getInt(Constants.PreferenceConsts.PORT_NUMBER,Constants.PreferenceConsts.PORT_NUMBER_DEFAULT));
            }
            tv_ips.setText(stringBuilder.toString());
            tv_main.setText(CommonUtils.getFTPServiceDisplayAddress(MyApplication.getGlobalBaseContext()));
        }
    }
}
