package com.github.ghmxr.ftpshare.activities

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.github.ghmxr.ftpshare.R
import com.github.ghmxr.ftpshare.data.ClientBean
import com.google.android.material.snackbar.Snackbar

class AddClientActivity : ClientInfoActivity() {

    private var lastClick = 0L

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        supportActionBar?.title = resources?.getString(R.string.activity_title_add)
    }

    override fun getClientBean(): ClientBean = ClientBean()

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_account_add, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == R.id.action_account_add_save) {
            if (saveOrUpdateClientBean()) {
                setResult(RESULT_OK)
                finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        val current = System.currentTimeMillis()
        if (current - lastClick > 2000L) {
            lastClick = current
            Snackbar.make(findViewById(android.R.id.content), resources.getString(R.string.attention_changes_confirm), Snackbar.LENGTH_SHORT).show()
            return
        }
        super.onBackPressed()
    }
}