package com.github.ghmxr.ftpshare.activities;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.github.ghmxr.ftpshare.R;
import com.github.ghmxr.ftpshare.data.AccountItem;
import com.github.ghmxr.ftpshare.services.FtpService;
import com.github.ghmxr.ftpshare.utils.MySQLiteOpenHelper;

public class EditAccountActivity extends AccountActivity {

    public static final String EXTRA_SERIALIZED_ACCOUNT_ITEM="account_item";
    private long first_clicked_delete =0;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try{
            getSupportActionBar().setTitle(getResources().getString(R.string.activity_title_edit));
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void initializeAccountItem() {
        try{
            //item= new AccountItem(FtpService.getUserAccountList(this).get(getIntent().getIntExtra(EXTRA_POSITION,-1)));
            item=(AccountItem)getIntent().getSerializableExtra(EXTRA_SERIALIZED_ACCOUNT_ITEM);
        }catch (Exception e){
            e.printStackTrace();
            Toast.makeText(this,e.toString(),Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_account,menu);
        return super.onCreateOptionsMenu(menu);
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
            case R.id.action_account_delete:{
                long time=System.currentTimeMillis();
                if(FtpService.isFTPServiceRunning()){
                    Snackbar.make(findViewById(android.R.id.content),getResources().getString(R.string.attention_ftp_is_running),Snackbar.LENGTH_SHORT).show();
                    return true;
                }
                if(time- first_clicked_delete >1000){
                    Snackbar.make(findViewById(android.R.id.content),getResources().getString(R.string.attention_delete_confirm),Snackbar.LENGTH_SHORT).show();
                    first_clicked_delete =time;
                    return true;
                }
                if(deleteRow(this.item.id)<=0){
                    Toast.makeText(this,"Can not find the row",Toast.LENGTH_SHORT).show();
                }
                setResult(RESULT_OK);
                finish();
            }
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event){
        if(keyCode==KeyEvent.KEYCODE_BACK){
            checkChangesAndExit();
            return true;
        }
        return super.onKeyDown(keyCode,event);
    }

    private long deleteRow(long id){
        try{
            return MySQLiteOpenHelper.deleteRow(this,id);
        }catch (Exception e){
            e.printStackTrace();
        }
        return -1;
    }

}
