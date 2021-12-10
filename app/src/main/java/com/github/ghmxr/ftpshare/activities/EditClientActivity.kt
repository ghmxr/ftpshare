package com.github.ghmxr.ftpshare.activities

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.view.Menu
import android.view.MenuItem
import com.github.ghmxr.ftpshare.R
import com.github.ghmxr.ftpshare.ftpclient.FtpClientManager

class EditClientActivity : ClientInfoActivity() {

    companion object {
        @JvmStatic
        val EXTRA_CLIENT_BEAN_ID: String = "client_bean_id"
    }

    private var lastClick = 0L

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        supportActionBar?.title = resources?.getString(R.string.activity_title_edit)
    }

    override fun getClientBean() = FtpClientManager.instance.getClientBeanOfId(intent.getIntExtra(EXTRA_CLIENT_BEAN_ID, -1))

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_account, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == R.id.action_account_delete) {
            val current = System.currentTimeMillis()
            if (current - lastClick > 2000L) {
                lastClick = current
                Snackbar.make(findViewById(android.R.id.content), resources.getString(R.string.attention_delete_confirm), Snackbar.LENGTH_SHORT).show()
                return super.onOptionsItemSelected(item)
            }
            if (deleteClientBean()) {
                setResult(RESULT_OK)
                finish()
            }
        }
        if (item?.itemId == R.id.action_account_save) {
            if (saveOrUpdateClientBean()) {
                setResult(RESULT_OK)
                finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (saveOrUpdateClientBean()) {
            setResult(RESULT_OK)
        }
        super.onBackPressed()
    }
}