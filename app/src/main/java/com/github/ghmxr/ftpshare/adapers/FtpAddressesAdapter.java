package com.github.ghmxr.ftpshare.adapers;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.github.ghmxr.ftpshare.R;
import com.github.ghmxr.ftpshare.utils.CommonUtils;
import com.github.ghmxr.ftpshare.utils.NetworkEnvironmentUtil;

import java.util.ArrayList;
import java.util.List;

public class FtpAddressesAdapter extends RecyclerView.Adapter<FtpAddressesAdapter.ViewHolder> {

    private final ArrayList<String> ftpAddresses=new ArrayList<>();
    private final Context context;

    public FtpAddressesAdapter(@NonNull Context context){
        this.context=context;
        List<String> ips= NetworkEnvironmentUtil.getLocalIpv4Addresses();
        for(String ip:ips){
            ftpAddresses.add(CommonUtils.getFtpServiceAddress(context,ip));
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.item_ftp_address,viewGroup,false));
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder viewHolder, int i) {
        viewHolder.tv.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG);
        viewHolder.tv.setText(ftpAddresses.get(viewHolder.getAdapterPosition()));
        viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try{
                    ClipboardManager manager=(ClipboardManager)context.getSystemService(Context.CLIPBOARD_SERVICE);
                    manager.setPrimaryClip(ClipData.newPlainText("message",viewHolder.tv.getText().toString()));
                    Toast.makeText(context,context.getResources().getString(R.string.attention_clipboard),Toast.LENGTH_SHORT).show();
                }catch (Exception e){e.printStackTrace();}
            }
        });
    }

    @Override
    public int getItemCount() {
        return ftpAddresses.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder{
        private TextView tv;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tv=itemView.findViewById(R.id.tv_ftp_address);
        }
    }
}
