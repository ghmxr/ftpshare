package com.github.ghmxr.ftpshare.adapers;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.github.ghmxr.ftpshare.R;
import com.github.ghmxr.ftpshare.activities.EditAccountActivity;
import com.github.ghmxr.ftpshare.data.AccountItem;
import com.github.ghmxr.ftpshare.services.FtpService;

import java.util.ArrayList;

public class AccountListAdapter extends BaseAdapter implements AdapterView.OnItemClickListener {

    private final ArrayList<AccountItem>accountItems=new ArrayList<>();
    private final Activity activity;

    public AccountListAdapter(@NonNull Activity activity, @NonNull ListView listView) {
        super();
        this.activity=activity;
        accountItems.addAll(FtpService.getUserAccountList(activity));
        listView.setOnItemClickListener(this);
        listView.setDivider(null);
    }

    @Override
    public int getCount() {
        return accountItems.size()+2;
    }

    @Override
    public Object getItem(int position) {
        return accountItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(convertView==null){
            convertView= LayoutInflater.from(activity).inflate(R.layout.item_account,parent,false);
        }
        ViewGroup viewGroup=convertView.findViewById(R.id.item_account_root);
        if(position>=accountItems.size()){
            viewGroup.setVisibility(View.INVISIBLE);
            return convertView;
        }else{
            viewGroup.setVisibility(View.VISIBLE);
        }
        final AccountItem accountItem=accountItems.get(position);
        TextView tv_account=convertView.findViewById(R.id.text_account);
        TextView tv_path=convertView.findViewById(R.id.text_path);
        View writable=convertView.findViewById(R.id.area_writable);
        tv_account.setText(accountItem.account);
        tv_path.setText(accountItem.path);
        writable.setVisibility(accountItem.writable?View.VISIBLE:View.GONE);
        return convertView;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Intent intent=new Intent(activity, EditAccountActivity.class);
        intent.putExtra(EditAccountActivity.EXTRA_SERIALIZED_ACCOUNT_ITEM,accountItems.get(position));
        activity.startActivityForResult(intent,1);
    }

    public ArrayList<AccountItem> getAccountItems(){
        return accountItems;
    }
}
