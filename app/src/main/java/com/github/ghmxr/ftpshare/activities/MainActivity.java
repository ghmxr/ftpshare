package com.github.ghmxr.ftpshare.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import com.github.ghmxr.ftpshare.R;
import com.github.ghmxr.ftpshare.data.AccountItem;
import com.github.ghmxr.ftpshare.services.FtpService;
import com.github.ghmxr.ftpshare.utils.APUtil;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Menu menu;
    private SwitchCompat switchCompat;
    private CheckBox cb_wakelock,cb_anonymous,cb_anonymous_writable;
    private TextView tv_main_value,tv_port,tv_anonymous_path;
    private SwipeRefreshLayout swr_users;
    private ListView listview_users;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.navigation_main:
                        onMainInterfaceClicked();
                        return true;
                    case R.id.navigation_settings:
                        onUserAccountInterfaceClicked();
                        return true;
                }
                return false;
            }
        });

        switchCompat=findViewById(R.id.main_switch);
        cb_wakelock=findViewById(R.id.wakelock_cb);
        cb_anonymous=findViewById(R.id.anonymous_cb);
        cb_anonymous_writable=findViewById(R.id.anonymous_writable_cb);
        tv_main_value=findViewById(R.id.main_att);
        tv_port=findViewById(R.id.port_att);
        tv_anonymous_path=findViewById(R.id.mode_anonymous_value);
        swr_users=findViewById(R.id.view_normal_swr);
        listview_users=findViewById(R.id.view_user_list);

        initializeValues();
    }

    private void initializeValues(){
        switchCompat.setChecked(FtpService.isFTPServiceRunning());
    }

    private void onMainInterfaceClicked(){
        findViewById(R.id.view_main).setVisibility(View.VISIBLE);
        findViewById(R.id.view_settings).setVisibility(View.GONE);
        menu.getItem(0).setVisible(false);
    }

    private void onUserAccountInterfaceClicked(){
        findViewById(R.id.view_main).setVisibility(View.GONE);
        findViewById(R.id.view_settings).setVisibility(View.VISIBLE);
        menu.getItem(0).setVisible(true);
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
