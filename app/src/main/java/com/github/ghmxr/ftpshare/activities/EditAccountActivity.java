package com.github.ghmxr.ftpshare.activities;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.github.ghmxr.ftpshare.R;
import com.github.ghmxr.ftpshare.data.AccountItem;
import com.github.ghmxr.ftpshare.services.FtpService;

public class EditAccountActivity extends AccountActivity {
    /**
     * this stands for a int value
     */
    public static final String EXTRA_POSITION="position";

    @Override
    public void initializeAccountData() {
        try{
            item= new AccountItem(FtpService.getUserAccountList(this).get(getIntent().getIntExtra(EXTRA_POSITION,-1)));
        }catch (Exception e){
            e.printStackTrace();
            Toast.makeText(this,e.toString(),Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            default:break;
            case R.id.action_account_save:{
                if(save2DB(this.item.id)>=0){
                    setResult(RESULT_OK);
                    finish();
                    return true;
                }

            }
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }
}
