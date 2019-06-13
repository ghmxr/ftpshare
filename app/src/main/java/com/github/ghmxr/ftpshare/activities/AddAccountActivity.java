package com.github.ghmxr.ftpshare.activities;

import android.view.Menu;
import android.view.MenuItem;

import com.github.ghmxr.ftpshare.R;
import com.github.ghmxr.ftpshare.data.AccountItem;
import com.github.ghmxr.ftpshare.services.FtpService;

public class AddAccountActivity extends AccountActivity {

    @Override
    public void initializeAccountItem() {
        item=new AccountItem();
        item.account="user"+ (FtpService.getUserAccountList(this).size()+1);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_account_add,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            default:break;
            case R.id.action_account_add_save:{
                if(save2DB(null)>=0) {
                    setResult(RESULT_OK);
                    finish();
                    return true;
                }
            }
            break;
        }
        return super.onOptionsItemSelected(item);
    }
}
