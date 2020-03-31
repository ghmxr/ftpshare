package com.github.ghmxr.ftpshare.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;

import com.github.ghmxr.ftpshare.Constants;
import com.github.ghmxr.ftpshare.R;
import com.github.ghmxr.ftpshare.services.FtpService;
import com.github.ghmxr.ftpshare.utils.CommonUtils;

public class DisconnectSelectionDialog extends AlertDialog implements View.OnClickListener{

    private RadioButton ra_none,ra_wifi,ra_ap,ra_time;
    private EditText editText_time;
    private int selection=-1;

    public DisconnectSelectionDialog(@NonNull Context context) {
        super(context);
        View dialogView=LayoutInflater.from(context).inflate(R.layout.layout_dialog_disconnection,null);
        setView(dialogView);
        ra_none=dialogView.findViewById(R.id.dialog_disconnection_none);
        ra_wifi=dialogView.findViewById(R.id.dialog_disconnection_wifi);
        ra_ap=dialogView.findViewById(R.id.dialog_disconnection_ap);
        ra_time=dialogView.findViewById(R.id.dialog_disconnect_time);
        editText_time=dialogView.findViewById(R.id.dialog_disconnect_time_edit);
        setTitle(context.getResources().getString(R.string.setting_disconnect));
        setButton(AlertDialog.BUTTON_POSITIVE,getContext().getResources().getString(R.string.dialog_button_confirm),(DialogInterface.OnClickListener)null);
        setButton(AlertDialog.BUTTON_NEGATIVE,getContext().getResources().getString(R.string.dialog_button_cancel),new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            default:break;
            case R.id.dialog_disconnection_none:{
                selection=Constants.PreferenceConsts.AUTO_STOP_NONE;
            }
            break;
            case R.id.dialog_disconnection_wifi:{
                selection=Constants.PreferenceConsts.AUTO_STOP_WIFI_DISCONNECTED;
            }
            break;
            case R.id.dialog_disconnection_ap:{
                selection=Constants.PreferenceConsts.AUTO_STOP_AP_DISCONNECTED;
            }
            break;
            case R.id.dialog_disconnect_time:{
                selection=Constants.PreferenceConsts.AUTO_STOP_TIME_COUNT;
            }
            break;
        }
        refreshViews();
    }

    @Override
    public void show() {
        super.show();
        selection=CommonUtils.getSettingSharedPreferences().getInt(Constants.PreferenceConsts.AUTO_STOP,Constants.PreferenceConsts.AUTO_STOP_DEFAULT);
        ra_time.setOnClickListener(this);
        ra_ap.setOnClickListener(this);
        ra_wifi.setOnClickListener(this);
        ra_none.setOnClickListener(this);
        getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor editor=CommonUtils.getSettingSharedPreferences().edit();
                if(selection==Constants.PreferenceConsts.AUTO_STOP_TIME_COUNT){
                    final String value=editText_time.getText().toString();
                    if(TextUtils.isEmpty(value)){
                        Toast.makeText(getContext(),getContext().getResources().getString(R.string.toast_invalid_value),Toast.LENGTH_SHORT).show();
                        return;
                    }
                    int seconds;
                    try{
                        seconds=Integer.parseInt(value);
                    }catch (Exception e){
                        e.printStackTrace();
                        Toast.makeText(getContext(),getContext().getResources().getString(R.string.toast_invalid_value),Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if(seconds<=0){
                        Toast.makeText(getContext(),getContext().getResources().getString(R.string.toast_invalid_value),Toast.LENGTH_SHORT).show();
                        return;
                    }
                    editor.putInt(Constants.PreferenceConsts.AUTO_STOP_VALUE,seconds);
                    FtpService.enableAutoDisconnectThisTime();
                    FtpService.setTimeCounts(seconds);
                }else{
                    FtpService.cancelTimeCounts();
                }
                FtpService.enableAutoDisconnectThisTime();
                editor.putInt(Constants.PreferenceConsts.AUTO_STOP,selection);
                editor.apply();
                cancel();
            }
        });
        refreshViews();
    }

    private void refreshViews(){
        ra_none.setChecked(selection==Constants.PreferenceConsts.AUTO_STOP_NONE);
        ra_wifi.setChecked(selection==Constants.PreferenceConsts.AUTO_STOP_WIFI_DISCONNECTED);
        ra_ap.setChecked(selection==Constants.PreferenceConsts.AUTO_STOP_AP_DISCONNECTED);
        final boolean isTime=selection==Constants.PreferenceConsts.AUTO_STOP_TIME_COUNT;
        ra_time.setChecked(isTime);
        editText_time.setText(String.valueOf(CommonUtils.getSettingSharedPreferences()
                .getInt(Constants.PreferenceConsts.AUTO_STOP_VALUE,Constants.PreferenceConsts.AUTO_STOP_VALUE_DEFAULT)));
        editText_time.setEnabled(isTime);
    }
}
