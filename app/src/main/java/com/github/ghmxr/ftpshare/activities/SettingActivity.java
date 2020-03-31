package com.github.ghmxr.ftpshare.activities;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatDelegate;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import com.github.ghmxr.ftpshare.Constants;
import com.github.ghmxr.ftpshare.R;
import com.github.ghmxr.ftpshare.ui.DisconnectSelectionDialog;
import com.github.ghmxr.ftpshare.ui.RadioSelectionDialog;
import com.github.ghmxr.ftpshare.utils.CommonUtils;

public class SettingActivity extends BaseActivity implements View.OnClickListener{

    private final SharedPreferences settings=CommonUtils.getSettingSharedPreferences();
    private final SharedPreferences.Editor editor=settings.edit();
    private int resultCode=RESULT_CANCELED;
    private static final String ACTIVITY_RESULT="result";
    private CheckBox cb_auto_start;
    private TextView tv_disconnect,tv_night_mode,tv_language;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.activity_settings);
        cb_auto_start=findViewById(R.id.setting_auto_start_cb);
        tv_disconnect=findViewById(R.id.setting_disconnect_value);
        tv_night_mode=findViewById(R.id.setting_night_mode_value);
        tv_language=findViewById(R.id.setting_language_value);
        if(bundle!=null){
            setResult(bundle.getInt(ACTIVITY_RESULT));
        }
        refreshSettingValues();
    }

    @Override
    public void onClick(View v){
        switch (v.getId()){
            default:break;
            case R.id.setting_night_mode:{
                new RadioSelectionDialog<>(this, getResources().getString(R.string.setting_night_mode),
                        new String[]{getResources().getString(R.string.setting_night_mode_auto), getResources().getString(R.string.setting_night_mode_disabled),
                                getResources().getString(R.string.setting_night_mode_enabled), getResources().getString(R.string.setting_night_mode_follow_system)},
                        new Integer[]{AppCompatDelegate.MODE_NIGHT_AUTO,AppCompatDelegate.MODE_NIGHT_NO,AppCompatDelegate.MODE_NIGHT_YES,AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM},
                        settings.getInt(Constants.PreferenceConsts.NIGHT_MODE,Constants.PreferenceConsts.NIGHT_MODE_DEFAULT),
                        new RadioSelectionDialog.ConfirmedCallback<Integer>() {
                    @Override
                    public void onConfirmed(String selection, Integer value) {
                        editor.putInt(Constants.PreferenceConsts.NIGHT_MODE,value);
                        editor.apply();
                        resultCode=RESULT_OK;
                        AppCompatDelegate.setDefaultNightMode(value);
                        recreate();
                    }
                }).show();
            }
            break;
            case R.id.setting_language:{
                new RadioSelectionDialog<>(this, getResources().getString(R.string.setting_language),
                        new String[]{getResources().getString(R.string.setting_language_follow_system),
                                getResources().getString(R.string.setting_language_chinese),
                                getResources().getString(R.string.setting_language_english)},
                        new Integer[]{Constants.PreferenceConsts.LANGUAGE_FOLLOW_SYSTEM, Constants.PreferenceConsts.LANGUAGE_SIMPLIFIED_CHINESE,
                                Constants.PreferenceConsts.LANGUAGE_ENGLISH},
                        settings.getInt(Constants.PreferenceConsts.LANGUAGE_SETTING, Constants.PreferenceConsts.LANGUAGE_SETTING_DEFAULT),
                        new RadioSelectionDialog.ConfirmedCallback<Integer>() {
                            @Override
                            public void onConfirmed(String selection, Integer value) {
                                resultCode=RESULT_OK;
                                editor.putInt(Constants.PreferenceConsts.LANGUAGE_SETTING,value);
                                editor.apply();
                                recreate();
                            }
                        }).show();
            }
            break;
            case R.id.setting_disconnect:{
                DisconnectSelectionDialog dialog=new DisconnectSelectionDialog(this);
                dialog.show();
                dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        refreshSettingValues();
                    }
                });
            }
            break;
            case R.id.setting_auto_start:{
                cb_auto_start.toggle();
                editor.putBoolean(Constants.PreferenceConsts.START_AFTER_BOOT,cb_auto_start.isChecked());
                editor.apply();
            }
            break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId()==android.R.id.home){
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(ACTIVITY_RESULT,resultCode);
    }

    private void refreshSettingValues(){
        cb_auto_start.setChecked(settings.getBoolean(Constants.PreferenceConsts.START_AFTER_BOOT,Constants.PreferenceConsts.START_AFTER_BOOT_DEFAULT));
        String value_disconnect="";
        switch (settings.getInt(Constants.PreferenceConsts.AUTO_STOP,Constants.PreferenceConsts.AUTO_STOP_DEFAULT)){
            default:break;
            case Constants.PreferenceConsts.AUTO_STOP_NONE:{
                value_disconnect=getResources().getString(R.string.word_none);
            }
            break;
            case Constants.PreferenceConsts.AUTO_STOP_AP_DISCONNECTED:{
                value_disconnect=getResources().getString(R.string.setting_disconnect_ap_disconnected);
            }
            break;
            case Constants.PreferenceConsts.AUTO_STOP_WIFI_DISCONNECTED:{
                value_disconnect=getResources().getString(R.string.setting_disconnect_wifi_disconnected);
            }
            break;
            case Constants.PreferenceConsts.AUTO_STOP_TIME_COUNT:{
                value_disconnect=getResources().getString(R.string.setting_disconnect_time);
            }
            break;
        }
        tv_disconnect.setText(value_disconnect);

        String night_mode="";
        switch (settings.getInt(Constants.PreferenceConsts.NIGHT_MODE,Constants.PreferenceConsts.NIGHT_MODE_DEFAULT)){
            default:break;
            case AppCompatDelegate.MODE_NIGHT_AUTO:{
                night_mode=getResources().getString(R.string.setting_night_mode_auto);
            }
            break;
            case AppCompatDelegate.MODE_NIGHT_NO:{
                night_mode=getResources().getString(R.string.setting_night_mode_disabled);
            }
            break;
            case AppCompatDelegate.MODE_NIGHT_YES:{
                night_mode=getResources().getString(R.string.setting_night_mode_enabled);
            }
            break;
            case AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM:{
                night_mode=getResources().getString(R.string.setting_night_mode_follow_system);
            }
            break;
        }
        tv_night_mode.setText(night_mode);

        String language_value="";
        switch (settings.getInt(Constants.PreferenceConsts.LANGUAGE_SETTING,Constants.PreferenceConsts.LANGUAGE_SETTING_DEFAULT)){
            default:break;
            case Constants.PreferenceConsts.LANGUAGE_FOLLOW_SYSTEM:{
                language_value=getResources().getString(R.string.setting_language_follow_system);
            }
            break;
            case Constants.PreferenceConsts.LANGUAGE_SIMPLIFIED_CHINESE:{
                language_value=getResources().getString(R.string.setting_language_chinese);
            }
            break;
            case Constants.PreferenceConsts.LANGUAGE_ENGLISH:{
                language_value=getResources().getString(R.string.setting_language_english);
            }
            break;
        }
        tv_language.setText(language_value);
    }
}
