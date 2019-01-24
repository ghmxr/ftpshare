package com.github.ghmxr.ftpshare.activities;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.method.PasswordTransformationMethod;
import android.text.method.SingleLineTransformationMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.github.ghmxr.ftpshare.Constants;
import com.github.ghmxr.ftpshare.R;
import com.github.ghmxr.ftpshare.data.AccountItem;
import com.github.ghmxr.ftpshare.services.FtpService;
import com.github.ghmxr.ftpshare.ui.DialogOfFolderSelector;
import com.github.ghmxr.ftpshare.utils.MySQLiteOpenHelper;

public abstract class AccountActivity extends AppCompatActivity {
    public AccountItem item;
    public TextView tv_account,tv_password,tv_path;
    public CheckBox cb_writable;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);
        try{
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }catch (Exception e){e.printStackTrace();}
        tv_account=findViewById(R.id.account_user_value);
        tv_password=findViewById(R.id.account_password_value);
        tv_path=findViewById(R.id.account_path_value);
        cb_writable=findViewById(R.id.account_writable_cb);
        initializeAccountData();
        try{
           tv_account.setText(item.account);
           tv_password.setText(getPasswordDisplayValue(item.password));
           tv_path.setText(item.path);
           cb_writable.setChecked(item.writable);

           findViewById(R.id.account_user).setOnClickListener(new View.OnClickListener() {
               @Override
               public void onClick(View v) {
                   View dialogView=LayoutInflater.from(AccountActivity.this).inflate(R.layout.layout_with_edittext,null);
                   final EditText editText=dialogView.findViewById(R.id.dialog_edittext);
                   editText.setText(item.account);
                   editText.setHint(getResources().getString(R.string.account_user_dialog_edittext_hint));
                   final AlertDialog dialog=new AlertDialog.Builder(AccountActivity.this)
                           .setTitle(getResources().getString(R.string.account_user_dialog_title))
                           .setView(dialogView)
                           .setPositiveButton(getResources().getString(R.string.dialog_button_confirm),null)
                           .setNegativeButton(getResources().getString(R.string.dialog_button_cancel), new DialogInterface.OnClickListener() {
                               @Override
                               public void onClick(DialogInterface dialog, int which) {

                               }
                           })
                           .show();
                   dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                       @Override
                       public void onClick(View v) {
                           item.account=editText.getText().toString().trim();
                           dialog.cancel();
                           tv_account.setText(item.account);
                       }
                   });
               }
           });

           findViewById(R.id.account_password).setOnClickListener(new View.OnClickListener() {
               @Override
               public void onClick(View v) {
                   View dialogView=LayoutInflater.from(AccountActivity.this).inflate(R.layout.layout_with_edittext,null);
                   final EditText editText=dialogView.findViewById(R.id.dialog_edittext);
                   editText.setText(item.password);
                   editText.setTransformationMethod(PasswordTransformationMethod.getInstance());
                   editText.setTag(true);
                   editText.setHint(getResources().getString(R.string.account_password_dialog_edittext_hint));
                   final AlertDialog dialog=new AlertDialog.Builder(AccountActivity.this)
                           .setTitle(getResources().getString(R.string.account_password_dialog_title))
                           .setView(dialogView)
                           .setPositiveButton(getResources().getString(R.string.dialog_button_confirm),null)
                           .setNegativeButton(getResources().getString(R.string.dialog_button_cancel), new DialogInterface.OnClickListener() {
                               @Override
                               public void onClick(DialogInterface dialog, int which) {

                               }
                           })
                           .setNeutralButton(getResources().getString(R.string.dialog_button_show_password), null)
                           .show();
                   dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(new View.OnClickListener() {
                       @Override
                       public void onClick(View v) {
                           try{
                               Boolean b=(Boolean) editText.getTag();
                               editText.setTransformationMethod(b?SingleLineTransformationMethod.getInstance():PasswordTransformationMethod.getInstance());
                               editText.setTag(!b);
                           }catch (Exception e){}
                       }
                   });
                   dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                       @Override
                       public void onClick(View v) {
                           item.password=editText.getText().toString().trim();
                           dialog.cancel();
                           tv_password.setText(getPasswordDisplayValue(item.password));
                       }
                   });
               }
           });

           findViewById(R.id.account_path).setOnClickListener(new View.OnClickListener() {
               @Override
               public void onClick(View v) {
                   DialogOfFolderSelector dialog=new DialogOfFolderSelector(AccountActivity.this,item.path);
                   dialog.show();
                   dialog.setOnFolderSelectorDialogConfirmedListener(new DialogOfFolderSelector.OnFolderSelectorDialogConfirmed() {
                       @Override
                       public void onFolderSelectorDialogConfirmed(String path) {
                           item.path=path;
                           ((TextView)findViewById(R.id.account_path_value)).setText(path);
                       }
                   });
               }
           });

           findViewById(R.id.account_writable).setOnClickListener(new View.OnClickListener() {
               @Override
               public void onClick(View v) {
                   CheckBox cb=findViewById(R.id.account_writable_cb);
                   cb.toggle();
                   item.writable=cb.isChecked();
               }
           });
        }catch (Exception e){
            Toast.makeText(this,e.toString(),Toast.LENGTH_SHORT).show();
        }
    }

    public abstract void initializeAccountData();

    private String getPasswordDisplayValue(String password){
        if(password==null||password.trim().length()==0) return getResources().getString(R.string.account_password_att);
        StringBuilder builder=new StringBuilder("");
        for(int i=0;i<password.length();i++){
            builder.append("*");
            if(i>16) break;
        }
        return builder.toString();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_account,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId()==android.R.id.home){
            setResult(RESULT_CANCELED);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public long save2DB(@Nullable Long id_update){
        if(this.item.account.equals("")){
            Snackbar.make(findViewById(R.id.view_account_root),getResources().getString(R.string.account_null_att),Snackbar.LENGTH_SHORT).show();
            return -1;
        }
        if(this.item.account.equals(Constants.FTPConsts.NAME_ANONYMOUS)){
            Snackbar.make(findViewById(R.id.view_account_root),getResources().getString(R.string.account_anonymous_name_set_att),Snackbar.LENGTH_SHORT).show();
            return -1;
        }
        for(AccountItem check: FtpService.getUserAccountList(this)){
            if(check.account.equals(this.item.account)&&id_update==null){
                Snackbar.make(findViewById(R.id.view_account_root),getResources().getString(R.string.account_duplicate),Snackbar.LENGTH_SHORT).show();
                return -1;
            }
        }
        try{
            SQLiteDatabase db=new MySQLiteOpenHelper(this).getWritableDatabase();
            ContentValues contentValues=new ContentValues();
            contentValues.put(Constants.SQLConsts.COLUMN_ACCOUNT_NAME,this.item.account);
            contentValues.put(Constants.SQLConsts.COLUMN_PASSWORD,this.item.password);
            contentValues.put(Constants.SQLConsts.COLUMN_PATH,this.item.path);
            contentValues.put(Constants.SQLConsts.COLUMN_WRITABLE,this.item.writable?1:0);
            if(id_update==null) return db.insert(Constants.SQLConsts.TABLE_NAME,null,contentValues);
            else return db.update(Constants.SQLConsts.TABLE_NAME, contentValues,Constants.SQLConsts.COLUMN_ID+"="+id_update,null);
        }catch (Exception e){
            e.printStackTrace();
            Toast.makeText(this,e.toString(),Toast.LENGTH_SHORT).show();
        }

        return -1;
    }
}
