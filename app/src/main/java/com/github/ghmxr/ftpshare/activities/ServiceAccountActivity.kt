package com.github.ghmxr.ftpshare.activities

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ListView
import android.widget.TextView
import androidx.core.content.PermissionChecker
import com.github.ghmxr.ftpshare.Constants
import com.github.ghmxr.ftpshare.MyApplication
import com.github.ghmxr.ftpshare.R
import com.github.ghmxr.ftpshare.adapers.AccountListAdapter
import com.github.ghmxr.ftpshare.services.FtpService
import com.github.ghmxr.ftpshare.utils.CommonUtils

class ServiceAccountActivity : BaseActivity() {

    private var viewGroup_anonymous: ViewGroup? = null
    private var accountListView: ListView? = null
    private var viewGroup_no_account: ViewGroup? = null
    private var anonymous_path: TextView? = null
    private var writable_cb: CheckBox? = null
    private var menu: Menu? = null

    private val settings = CommonUtils.getSettingSharedPreferences(MyApplication.getGlobalBaseContext())
    private val editor = settings.edit()

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        setContentView(R.layout.fragment_account)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = resources?.getString(R.string.item_account_settings)
        viewGroup_anonymous = findViewById<ViewGroup>(R.id.mode_anonymous)
        accountListView = findViewById<ListView>(R.id.view_user_list)
        anonymous_path = findViewById<TextView>(R.id.mode_anonymous_value)
        writable_cb = findViewById<CheckBox>(R.id.anonymous_writable_cb)
        viewGroup_no_account = findViewById<ViewGroup>(R.id.add_user_att)

        findViewById<View>(R.id.anonymous_path).setOnClickListener(this::onClick)
        findViewById<View>(R.id.anonymous_writable).setOnClickListener(this::onClick)
        refreshContents()
    }

    fun onClick(v: View) {
        when (v.id) {
            R.id.anonymous_path -> {
                if (FtpService.isFTPServiceRunning()) {
                    CommonUtils.showSnackBarOfFtpServiceIsRunning(this)
                    return
                }
                if (Build.VERSION.SDK_INT >= 23 && PermissionChecker.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PermissionChecker.PERMISSION_GRANTED) {
                    CommonUtils.showSnackBarOfRequestingWritingPermission(this)
                    requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 0)
                    return
                }
                /*val activity: Activity = this
                val dialog = DialogOfFolderSelector(activity, settings.getString(Constants.PreferenceConsts.ANONYMOUS_MODE_PATH, Constants.PreferenceConsts.ANONYMOUS_MODE_PATH_DEFAULT).toString())
                dialog.show()
                dialog.setOnFolderSelectorDialogConfirmedListener(OnFolderSelectorDialogConfirmed { path ->
                    if (FtpService.isFTPServiceRunning()) {
                        Toast.makeText(activity, resources.getString(R.string.attention_ftp_is_running), Toast.LENGTH_SHORT).show()
                        return@OnFolderSelectorDialogConfirmed
                    }
                    editor.putString(Constants.PreferenceConsts.ANONYMOUS_MODE_PATH, path)
                    editor.apply()
                    anonymous_path!!.text = path
                })*/

                Intent(this, FolderSelectorActivity::class.java).apply {
                    putExtra(FolderSelectorActivity.EXTRA_CURRENT_PATH, settings.getString(Constants.PreferenceConsts.ANONYMOUS_MODE_PATH, Constants.PreferenceConsts.ANONYMOUS_MODE_PATH_DEFAULT))
                    startActivityForResult(this, 0)
                }
            }
            R.id.anonymous_writable -> {
                if (FtpService.isFTPServiceRunning()) {
                    CommonUtils.showSnackBarOfFtpServiceIsRunning(this)
                    return
                }
                writable_cb?.toggle()
                editor.putBoolean(Constants.PreferenceConsts.ANONYMOUS_MODE_WRITABLE, writable_cb!!.isChecked)
                editor.apply()
            }
            else -> {
            }
        }
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_service_account, menu)
        this.menu = menu
        menu?.let {
            it.getItem(0).setVisible(!CommonUtils.isAnonymousMode(this));
            it.getItem(1).title = if (CommonUtils.isAnonymousMode(this)) resources.getString(R.string.action_main_anonymous_opened) else resources.getString(R.string.action_main_anonymous_closed)
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.action_main_add -> {
                if (FtpService.isFTPServiceRunning()) {
                    CommonUtils.showSnackBarOfFtpServiceIsRunning(this);
                    return true;
                }
                startActivityForResult(Intent(this, AddAccountActivity::class.java), 1)
                return true;
            }
            R.id.action_main_anonymous_switch -> {
                if (FtpService.isFTPServiceRunning()) {
                    CommonUtils.showSnackBarOfFtpServiceIsRunning(this)
                    return true
                }
                try {
                    val settings = getSharedPreferences(Constants.PreferenceConsts.FILE_NAME, MODE_PRIVATE)
                    val editor = settings.edit()
                    val isAnonymousMode = settings.getBoolean(Constants.PreferenceConsts.ANONYMOUS_MODE, Constants.PreferenceConsts.ANONYMOUS_MODE_DEFAULT)
                    editor.putBoolean(Constants.PreferenceConsts.ANONYMOUS_MODE, !isAnonymousMode)
                    editor.apply()
                    menu?.getItem(1)?.setTitle(if (!isAnonymousMode) resources.getString(R.string.action_main_anonymous_opened) else resources.getString(R.string.action_main_anonymous_closed))
                    menu?.getItem(0)?.setVisible(isAnonymousMode)
                    refreshContents()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            android.R.id.home -> {
                onBackPressed()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun refreshContents() {
        anonymous_path?.text = settings.getString(Constants.PreferenceConsts.ANONYMOUS_MODE_PATH, Constants.PreferenceConsts.ANONYMOUS_MODE_PATH_DEFAULT)
        writable_cb?.isChecked = settings.getBoolean(Constants.PreferenceConsts.ANONYMOUS_MODE_WRITABLE, Constants.PreferenceConsts.ANONYMOUS_MODE_WRITABLE_DEFAULT)
        val accountListAdapter = AccountListAdapter(this, accountListView!!)
        accountListView?.adapter = accountListAdapter
        viewGroup_anonymous?.visibility = if (CommonUtils.isAnonymousMode(this)) View.VISIBLE else View.GONE
        accountListView?.visibility = if (CommonUtils.isAnonymousMode(this)) View.GONE else View.VISIBLE
        viewGroup_no_account?.visibility = if (CommonUtils.isAnonymousMode(this)) View.GONE else if (accountListAdapter.accountItems.size > 0) View.GONE else View.VISIBLE
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 0 && resultCode == RESULT_OK) {
            data?.getStringExtra(FolderSelectorActivity.EXTRA_SELECTED_PATH)?.let {
                editor.putString(Constants.PreferenceConsts.ANONYMOUS_MODE_PATH, it)
                editor.apply()
                anonymous_path?.text = it
            }
        }
        refreshContents()
    }
}