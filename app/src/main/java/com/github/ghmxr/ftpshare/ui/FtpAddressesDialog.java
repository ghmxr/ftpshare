package com.github.ghmxr.ftpshare.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import com.github.ghmxr.ftpshare.R;
import com.github.ghmxr.ftpshare.adapers.FtpAddressesAdapter;

public class FtpAddressesDialog extends AlertDialog {

    public FtpAddressesDialog(@NonNull Context context) {
        super(context);
        RecyclerView recyclerView=new RecyclerView(context);
        ViewGroup.LayoutParams layoutParams=new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        recyclerView.setLayoutParams(layoutParams);
        LinearLayoutManager linearLayoutManager=new LinearLayoutManager(getContext());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(new FtpAddressesAdapter(context));
        setTitle(context.getResources().getString(R.string.item_ftp_addresses));
        setView(recyclerView);
        setButton(AlertDialog.BUTTON_NEGATIVE,context.getResources().getString(R.string.dialog_button_cancel),new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which) {}
        });
    }
}
