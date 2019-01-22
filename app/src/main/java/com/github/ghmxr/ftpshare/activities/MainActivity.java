package com.github.ghmxr.ftpshare.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.github.ghmxr.ftpshare.R;
import com.github.ghmxr.ftpshare.data.AccountItem;
import com.github.ghmxr.ftpshare.services.FtpService;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Menu menu;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_main:
                    findViewById(R.id.view_main).setVisibility(View.VISIBLE);
                    findViewById(R.id.view_settings).setVisibility(View.GONE);
                    menu.getItem(0).setVisible(false);
                    return true;
                case R.id.navigation_settings:
                    findViewById(R.id.view_main).setVisibility(View.GONE);
                    findViewById(R.id.view_settings).setVisibility(View.VISIBLE);
                    menu.getItem(0).setVisible(true);
                    return true;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        startService(new Intent(this, FtpService.class));
        SharedPreferences settings=getSharedPreferences("settings", Activity.MODE_PRIVATE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main,menu);
        this.menu=menu;
        menu.getItem(0).setVisible(false);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            default:break;
            case R.id.action_main_add:{
                startActivity(new Intent(this,AddAccountActivity.class));
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean ifFtpIsRunning(){
        return false;
    }

    private class AccountAdapter extends BaseAdapter{
        List<AccountItem> list;
        AccountAdapter(List<AccountItem> list){
            if(list==null) this.list=new ArrayList<>();
            this.list=list;
        }
        @Override
        public int getCount() {
            return list.size()+3;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if(convertView==null){
                convertView= LayoutInflater.from(MainActivity.this).inflate(R.layout.item_account,parent,false);
            }
            if(position>=list.size()) convertView.setVisibility(View.INVISIBLE);
            else convertView.setVisibility(View.VISIBLE);

            AccountItem item=null;
            try{
                item=list.get(position);
            }catch (Exception e){e.printStackTrace();}
            if(item==null) {
                convertView.setVisibility(View.GONE);
                return convertView;
            }

            TextView tv_account=convertView.findViewById(R.id.text_account);
            TextView tv_path=convertView.findViewById(R.id.text_path);
            View writable=convertView.findViewById(R.id.area_writable);
            tv_account.setText(item.account);
            tv_path.setText(item.path);
            writable.setVisibility(item.writable?View.VISIBLE:View.GONE);
            return convertView;
        }
    }

}
