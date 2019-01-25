package com.github.ghmxr.ftpshare.activities;

import android.database.sqlite.SQLiteDatabase;
import android.support.design.widget.Snackbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.github.ghmxr.ftpshare.Constants;
import com.github.ghmxr.ftpshare.R;
import com.github.ghmxr.ftpshare.data.AccountItem;
import com.github.ghmxr.ftpshare.services.FtpService;
import com.github.ghmxr.ftpshare.utils.MySQLiteOpenHelper;

public class EditAccountActivity extends AccountActivity {
    /**
     * this stands for a int value
     */
    public static final String EXTRA_POSITION="position";

    private long first_clicked=0;

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
                if(time-first_clicked>1000){
                    Snackbar.make(findViewById(R.id.view_account_root),getResources().getString(R.string.attention_delete_confirm),Snackbar.LENGTH_SHORT).show();
                    first_clicked=time;
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

    private long deleteRow(long id){
        try{
            SQLiteDatabase database=new MySQLiteOpenHelper(this).getWritableDatabase();
            return database.delete(Constants.SQLConsts.TABLE_NAME,Constants.SQLConsts.COLUMN_ID+"="+id,null);
        }catch (Exception e){
            e.printStackTrace();
        }
        return -1;
    }

}
