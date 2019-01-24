package com.github.ghmxr.ftpshare.activities;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.MenuItem;

import com.github.ghmxr.ftpshare.R;
import com.github.ghmxr.ftpshare.data.AccountItem;
import com.github.ghmxr.ftpshare.services.FtpService;

public class AddAccountActivity extends AccountActivity {

    @Override
    public void initializeAccountData() {
        item=new AccountItem();
        item.account="user"+ (FtpService.list_account.size()+1);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            default:break;
            case R.id.action_account_save:{
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
