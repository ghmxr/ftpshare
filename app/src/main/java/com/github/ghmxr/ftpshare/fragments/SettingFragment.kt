package com.github.ghmxr.ftpshare.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatDelegate
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import com.github.ghmxr.ftpshare.Constants
import com.github.ghmxr.ftpshare.MyApplication
import com.github.ghmxr.ftpshare.R
import com.github.ghmxr.ftpshare.activities.MainActivity
import com.github.ghmxr.ftpshare.ui.DisconnectSelectionDialog
import com.github.ghmxr.ftpshare.ui.RadioSelectionDialog
import com.github.ghmxr.ftpshare.ui.RadioSelectionDialog.ConfirmedCallback
import com.github.ghmxr.ftpshare.utils.CommonUtils

class SettingFragment : Fragment() {
    private val settings = CommonUtils.getSettingSharedPreferences(MyApplication.getGlobalBaseContext())
    private val editor = settings.edit()
    private var resultCode = Activity.RESULT_CANCELED
    private val ACTIVITY_RESULT = "result_code"
    private var cb_auto_start: CheckBox? = null
    private var tv_disconnect: TextView? = null
    private var tv_night_mode: TextView? = null
    private var tv_language: TextView? = null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return LayoutInflater.from(context).inflate(R.layout.activity_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cb_auto_start = view.findViewById<CheckBox>(R.id.setting_auto_start_cb)
        tv_disconnect = view.findViewById<TextView>(R.id.setting_disconnect_value)
        tv_night_mode = view.findViewById<TextView>(R.id.setting_night_mode_value)
        tv_language = view.findViewById<TextView>(R.id.setting_language_value)
        view.findViewById<View>(R.id.setting_auto_start).setOnClickListener(this::onClick)
        view.findViewById<View>(R.id.setting_disconnect).setOnClickListener(this::onClick)
        view.findViewById<View>(R.id.setting_night_mode).setOnClickListener(this::onClick)
        view.findViewById<View>(R.id.setting_language).setOnClickListener(this::onClick)
        refreshSettingValues()
    }


    fun onClick(v: View) {
        when (v.id) {
            R.id.setting_night_mode -> {
                RadioSelectionDialog(context!!, resources.getString(R.string.setting_night_mode), arrayOf(resources.getString(R.string.setting_night_mode_auto), resources.getString(R.string.setting_night_mode_disabled),
                        resources.getString(R.string.setting_night_mode_enabled), resources.getString(R.string.setting_night_mode_follow_system)), arrayOf(AppCompatDelegate.MODE_NIGHT_AUTO, AppCompatDelegate.MODE_NIGHT_NO, AppCompatDelegate.MODE_NIGHT_YES, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM),
                        settings.getInt(Constants.PreferenceConsts.NIGHT_MODE, Constants.PreferenceConsts.NIGHT_MODE_DEFAULT),
                        object : ConfirmedCallback<Int> {
                            override fun onConfirmed(selection: String, value: Int) {
                                editor.putInt(Constants.PreferenceConsts.NIGHT_MODE, value)
                                editor.apply()
                                resultCode = Activity.RESULT_OK
                                AppCompatDelegate.setDefaultNightMode(value)
                                reopen()
                            }
                        }).show()
            }
            R.id.setting_language -> {
                RadioSelectionDialog(context!!, resources.getString(R.string.setting_language), arrayOf(resources.getString(R.string.setting_language_follow_system),
                        resources.getString(R.string.setting_language_chinese),
                        resources.getString(R.string.setting_language_english)), arrayOf(Constants.PreferenceConsts.LANGUAGE_FOLLOW_SYSTEM, Constants.PreferenceConsts.LANGUAGE_SIMPLIFIED_CHINESE,
                        Constants.PreferenceConsts.LANGUAGE_ENGLISH),
                        settings.getInt(Constants.PreferenceConsts.LANGUAGE_SETTING, Constants.PreferenceConsts.LANGUAGE_SETTING_DEFAULT),
                        object : ConfirmedCallback<Int> {
                            override fun onConfirmed(selection: String, value: Int) {
                                resultCode = Activity.RESULT_OK
                                editor.putInt(Constants.PreferenceConsts.LANGUAGE_SETTING, value)
                                editor.apply()
                                reopen()
                            }
                        }).show()
            }
            R.id.setting_disconnect -> {
                val dialog = DisconnectSelectionDialog(context!!)
                dialog.show()
                dialog.setOnCancelListener { refreshSettingValues() }
            }
            R.id.setting_auto_start -> {
                cb_auto_start!!.toggle()
                editor.putBoolean(Constants.PreferenceConsts.START_AFTER_BOOT, cb_auto_start!!.isChecked)
                editor.apply()
            }
            else -> {
            }
        }
    }

    fun processActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        refreshSettingValues()
    }


    private fun refreshSettingValues() {
        if(context==null)return
        cb_auto_start?.isChecked = settings.getBoolean(Constants.PreferenceConsts.START_AFTER_BOOT, Constants.PreferenceConsts.START_AFTER_BOOT_DEFAULT)
        var value_disconnect: String? = ""
        when (settings.getInt(Constants.PreferenceConsts.AUTO_STOP, Constants.PreferenceConsts.AUTO_STOP_DEFAULT)) {
            Constants.PreferenceConsts.AUTO_STOP_NONE -> {
                value_disconnect = resources.getString(R.string.word_none)
            }
            Constants.PreferenceConsts.AUTO_STOP_AP_DISCONNECTED -> {
                value_disconnect = resources.getString(R.string.setting_disconnect_ap_disconnected)
            }
            Constants.PreferenceConsts.AUTO_STOP_WIFI_DISCONNECTED -> {
                value_disconnect = resources.getString(R.string.setting_disconnect_wifi_disconnected)
            }
            Constants.PreferenceConsts.AUTO_STOP_TIME_COUNT -> {
                value_disconnect = resources.getString(R.string.setting_disconnect_time)
            }
            else -> {
            }
        }
        tv_disconnect?.text = value_disconnect
        var night_mode: String? = ""
        when (settings.getInt(Constants.PreferenceConsts.NIGHT_MODE, Constants.PreferenceConsts.NIGHT_MODE_DEFAULT)) {
            AppCompatDelegate.MODE_NIGHT_AUTO -> {
                night_mode = resources.getString(R.string.setting_night_mode_auto)
            }
            AppCompatDelegate.MODE_NIGHT_NO -> {
                night_mode = resources.getString(R.string.setting_night_mode_disabled)
            }
            AppCompatDelegate.MODE_NIGHT_YES -> {
                night_mode = resources.getString(R.string.setting_night_mode_enabled)
            }
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> {
                night_mode = resources.getString(R.string.setting_night_mode_follow_system)
            }
            else -> {
            }
        }
        tv_night_mode?.text = night_mode
        var language_value: String? = ""
        when (settings.getInt(Constants.PreferenceConsts.LANGUAGE_SETTING, Constants.PreferenceConsts.LANGUAGE_SETTING_DEFAULT)) {
            Constants.PreferenceConsts.LANGUAGE_FOLLOW_SYSTEM -> {
                language_value = resources.getString(R.string.setting_language_follow_system)
            }
            Constants.PreferenceConsts.LANGUAGE_SIMPLIFIED_CHINESE -> {
                language_value = resources.getString(R.string.setting_language_chinese)
            }
            Constants.PreferenceConsts.LANGUAGE_ENGLISH -> {
                language_value = resources.getString(R.string.setting_language_english)
            }
            else -> {
            }
        }
        tv_language?.text = language_value
    }

    private fun reopen(){
        activity?.let {
            Intent(it,MainActivity::class.java).apply {
                putExtra(MainActivity.STATE_CURRENT_TAB,2)
                it.finish()
                it.overridePendingTransition(0,0)
                startActivity(this)
            }
        }

    }


}