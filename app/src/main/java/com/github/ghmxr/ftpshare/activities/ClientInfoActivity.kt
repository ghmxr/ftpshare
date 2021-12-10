package com.github.ghmxr.ftpshare.activities

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.text.method.DigitsKeyListener
import android.text.method.PasswordTransformationMethod
import android.text.method.SingleLineTransformationMethod
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.PermissionChecker
import com.github.ghmxr.ftpshare.Constants
import com.github.ghmxr.ftpshare.R
import com.github.ghmxr.ftpshare.data.ClientBean
import com.github.ghmxr.ftpshare.ftpclient.FtpClientManager
import com.google.android.material.snackbar.Snackbar

abstract class ClientInfoActivity : BaseActivity() {

    private val bean: ClientBean by lazy { getClientBean() }

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        setContentView(R.layout.layout_client_info)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        findViewById<View>(R.id.client_host).setOnClickListener {
            createAndShowAlertDialog(resources.getString(R.string.client_host), bean.host, resources.getString(R.string.client_host_hint)) { dialog, s ->
                run {
                    if (s.isEmpty() || s.trim().isEmpty()) {
                        Toast.makeText(this, resources.getString(R.string.client_host_name_blank), Toast.LENGTH_SHORT).show()
                        return@run
                    }
                    bean.host = s
                    dialog.cancel()
                    refreshAllViews()
                }
            }
        }

        findViewById<View>(R.id.client_port).setOnClickListener {
            createAndShowAlertDialog(resources.getString(R.string.client_port), bean.port.toString(), resources.getString(R.string.client_port_hint)) { dialog, s ->
                run {
                    if (s.isEmpty() || s.trim().isEmpty()) {
                        Toast.makeText(this, resources.getString(R.string.client_port_invalid), Toast.LENGTH_SHORT).show()
                        return@run
                    } else {
                        try {
                            val i = s.toInt()
                            if (i < 0 || i > 65535) {
                                Toast.makeText(this, resources.getString(R.string.client_port_invalid), Toast.LENGTH_SHORT).show()
                                return@run
                            }
                            bean.port = i
                            dialog.cancel()
                            refreshAllViews()
                        } catch (e: NumberFormatException) {
                            Toast.makeText(this, resources.getString(R.string.client_port_invalid), Toast.LENGTH_SHORT).show()
                            return@run
                        }
                    }

                }
            }.apply {
                findViewById<EditText>(R.id.dialog_edittext)?.let {
                    it.keyListener = DigitsKeyListener.getInstance("0123456789")
                    it.inputType = InputType.TYPE_CLASS_NUMBER
                }

            }
        }

        findViewById<View>(R.id.client_user_name_area).setOnClickListener {
            createAndShowAlertDialog(resources.getString(R.string.client_user), bean.userName, resources.getString(R.string.client_user_hint)) { d, s ->
                bean.userName = s
                d.cancel()
                refreshAllViews()
            }
        }

        findViewById<View>(R.id.client_nickname_area).setOnClickListener {
            createAndShowAlertDialog(resources.getString(R.string.client_nickname), bean.nickName, resources.getString(R.string.client_nickname)) { d, s ->
                bean.nickName = s
                d.cancel()
                refreshAllViews()
            }
        }

        findViewById<View>(R.id.client_password_area).setOnClickListener {
            createAlertDialogBuilder(resources.getString(R.string.client_password), bean.password, resources.getString(R.string.client_user_hint)).apply {
                setNeutralButton(resources.getString(R.string.dialog_button_show_password), null)
            }.create().apply {
                show()
                val et = this.findViewById<EditText>(R.id.dialog_edittext)
                et?.transformationMethod = PasswordTransformationMethod.getInstance()
                et?.tag = true
                getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    bean.password = et?.text.toString()
                    this.cancel()
                    refreshAllViews()
                }
                getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                    val b = if (et?.tag == null) false else et.tag
                    et?.transformationMethod = if (b == true) SingleLineTransformationMethod.getInstance() else PasswordTransformationMethod.getInstance()
                    et?.tag = b != true
                }
            }
        }

        findViewById<View>(R.id.client_download_path_area).setOnClickListener {
            /*val dialog = DialogOfFolderSelector(this, bean.downloadPath)
            dialog.setOnFolderSelectorDialogConfirmedListener {
                bean.downloadPath = it
                refreshAllViews()
            }
            dialog.show()*/
            if (checkAndRequestWritingPermission()) {
                return@setOnClickListener
            }
            Intent(this, FolderSelectorActivity::class.java).apply {
                putExtra(FolderSelectorActivity.EXTRA_CURRENT_PATH, bean.downloadPath)
                startActivityForResult(this, 0)
            }
        }

        findViewById<View>(R.id.client_download_select_area).setOnClickListener {
            bean.downloadConfirm = !bean.downloadConfirm == true
            refreshAllViews()
        }

        findViewById<View>(R.id.client_encode_area).setOnClickListener {
            AlertDialog.Builder(this)
                    .setTitle(resources.getString(R.string.item_charset))
                    .setView(R.layout.layout_dialog_charset)
                    .show().apply {
                        val ra_utf = findViewById<RadioButton>(R.id.charset_selection_utf)
                        val ra_gbk = findViewById<RadioButton>(R.id.charset_selection_gbk)
                        ra_utf?.isChecked = Constants.Charset.CHAR_UTF == bean.encode
                        ra_gbk?.isChecked = Constants.Charset.CHAR_GBK == bean.encode
                        ra_utf?.setOnClickListener {
                            bean.encode = Constants.Charset.CHAR_UTF
                            cancel()
                            refreshAllViews()
                        }
                        ra_gbk?.setOnClickListener {
                            bean.encode = Constants.Charset.CHAR_GBK
                            cancel()
                            refreshAllViews()
                        }
                    }
        }

        findViewById<View>(R.id.client_passive_area).setOnClickListener {
            bean.passiveMode = bean.passiveMode != true
            refreshAllViews()
        }

        findViewById<View>(R.id.client_connect_timeout_area).setOnClickListener {
            createAlertDialogBuilder(resources.getString(R.string.client_connect_timeout), bean.connectTimeout.toString(), resources.getString(R.string.word_second))
                    .show().apply {
                        val e = findViewById<EditText>(R.id.dialog_edittext)
                        e?.keyListener = DigitsKeyListener.getInstance("0123456789")
                        getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                            val s = e?.text?.toString()
                            if (s.isNullOrBlank() || s.toInt() <= 0) {
                                Toast.makeText(this@ClientInfoActivity, resources.getString(R.string.client_connect_timeout_invalid_value), Toast.LENGTH_SHORT).show()
                                return@setOnClickListener
                            }
                            bean.connectTimeout = s.toInt()
                            cancel()
                            refreshAllViews()
                        }
                    }
        }

        refreshAllViews()

        checkAndRequestWritingPermission()

    }

    private fun checkAndRequestWritingPermission(): Boolean {
        var b = false
        if (PermissionChecker.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PermissionChecker.PERMISSION_GRANTED && Build.VERSION.SDK_INT >= 23) {
            b = true
            //requestPermissions(Array(1){Manifest.permission.WRITE_EXTERNAL_STORAGE},0)
            val snackbar: Snackbar = Snackbar.make(findViewById<View>(android.R.id.content), getResources().getString(R.string.permission_write_external), Snackbar.LENGTH_SHORT)
            snackbar.setAction(getResources().getString(R.string.snackbar_action_goto), View.OnClickListener {
                val appdetail = Intent()
                appdetail.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                appdetail.data = Uri.fromParts("package", getApplication().getPackageName(), null)
                startActivity(appdetail)
            })
            snackbar.show()
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 0)
        }
        return b
    }

    abstract fun getClientBean(): ClientBean

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == android.R.id.home) {
            onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 0 && resultCode == RESULT_OK) {
            data?.getStringExtra(FolderSelectorActivity.EXTRA_SELECTED_PATH)?.let {
                bean.downloadPath = it
                refreshAllViews()
            }
        }
    }

    private fun refreshAllViews() {
        findViewById<TextView>(R.id.client_host_value).text = /*if (bean.host.isEmpty()) resources.getString(R.string.word_none) else */bean.host
        findViewById<TextView>(R.id.client_port_value).text = bean.port.toString()
        findViewById<TextView>(R.id.client_user_name).text = if (bean.userName.isEmpty()) resources.getString(R.string.word_none) else bean.userName
        if (bean.password.length > 5) {
            findViewById<TextView>(R.id.client_password_value).text = "*****"
        } else if (bean.password.isEmpty() || bean.password.trim().isEmpty()) {
            findViewById<TextView>(R.id.client_password_value).text = resources.getText(R.string.word_none)
        } else {
            val builder = StringBuilder()
            for (i in bean.password.indices) {
                builder.append("*")
            }
            findViewById<TextView>(R.id.client_password_value).text = builder.toString()
        }
        findViewById<TextView>(R.id.client_nickname_value).text = bean.nickName
        findViewById<TextView>(R.id.client_download_path_value).text = bean.downloadPath
        findViewById<CheckBox>(R.id.client_download_select_cb).isChecked = bean.downloadConfirm
        findViewById<TextView>(R.id.client_encode_value).text = bean.encode
        findViewById<CheckBox>(R.id.client_passive_cb).isChecked = bean.passiveMode
        findViewById<TextView>(R.id.client_connect_timeout_value).text = "${bean.connectTimeout}${resources.getString(R.string.word_second)}"
    }

    private fun createAndShowAlertDialog(title: String, s: String, h: String, c: ((AlertDialog, String) -> Unit)) = createAlertDialogBuilder(title, s, h).create().apply {
        show()
        getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            c.invoke(this, this.findViewById<EditText>(R.id.dialog_edittext)?.text.toString())
        }
    }

    private fun createAlertDialogBuilder(title: String, s: String, h: String) = AlertDialog.Builder(this).apply {
        setTitle(title)
        this.setView(LayoutInflater.from(this@ClientInfoActivity).inflate(R.layout.layout_with_edittext, null).apply {
            this.findViewById<EditText>(R.id.dialog_edittext).setText(s)
            this.findViewById<EditText>(R.id.dialog_edittext).hint = h
        })
        setPositiveButton(resources.getString(R.string.dialog_button_confirm), null)
        setNegativeButton(resources.getString(R.string.dialog_button_cancel)) { _, _ -> }
    }

    protected fun saveOrUpdateClientBean(): Boolean {
        if (bean.host.isEmpty() || bean.host.trim().isEmpty()) {
            Snackbar.make(findViewById(android.R.id.content), resources.getString(R.string.client_host_name_blank), Snackbar.LENGTH_SHORT).show()
            return false
        }
        if (bean.port < 0 || bean.port > 65535) {
            Snackbar.make(findViewById(android.R.id.content), resources.getString(R.string.client_port_invalid), Snackbar.LENGTH_SHORT).show()
            return false
        }
        if (bean._id == -1) FtpClientManager.instance.saveClientBean(bean)
        else FtpClientManager.instance.updateClientBean(bean._id, bean)
        return true
    }

    protected fun deleteClientBean(): Boolean {
        return FtpClientManager.instance.deleteClientBean(bean)
    }


}