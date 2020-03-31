package com.github.ghmxr.ftpshare.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.github.ghmxr.ftpshare.R;
import com.github.ghmxr.ftpshare.utils.CommonUtils;
import com.github.ghmxr.ftpshare.utils.StorageUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DialogOfFolderSelector extends AlertDialog {
    private View dialogView;
    private Context context;
    private File file;
    private final List<File> list=new ArrayList<>();
    private Spinner spinner;
    private ListView listView;
    private FileListAdapter adapter;
    private OnFolderSelectorDialogConfirmed listener_confirmed;
    private final Handler handler=new Handler(Looper.getMainLooper());

    private final Bundle positionRecords=new Bundle();

    public DialogOfFolderSelector(@NonNull Context context,@NonNull String path) {
        super(context);
        this.context=context;
        dialogView=LayoutInflater.from(context).inflate(R.layout.layout_dialog_folder_selector,null);
        setView(dialogView);
        setTitle(context.getResources().getString(R.string.account_path_dialog_title));
        spinner=dialogView.findViewById(R.id.folder_storage);
        listView=dialogView.findViewById(R.id.folder_list);
        try{
            file=new File(path);
        }catch (Exception e){
            e.printStackTrace();
            file=new File(StorageUtil.getMainStoragePath());
        }
        try{
            List<String> storages;
            if(Build.VERSION.SDK_INT>=19){
                storages= StorageUtil.getAvailableStoragePaths(context);
            }else{
                storages= StorageUtil.getAvailableStoragePaths();
            }
            spinner.setAdapter(new ArrayAdapter<>(context,R.layout.item_spinner_storage,R.id.item_storage_text,storages));
            for(int i=0;i<storages.size();i++){
                if(CommonUtils.isChildPathOfCertainPath(file, new File(storages.get(i)))) {
                    spinner.setSelection(i);
                    break;
                }
            }
        }catch (Exception e){e.printStackTrace();}

        adapter=new FileListAdapter();
        listView.setAdapter(adapter);
        setButton(AlertDialog.BUTTON_POSITIVE, context.getResources().getString(R.string.dialog_button_confirm), new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which) {
                onDialogConfirmed();
            }
        });
        setButton(AlertDialog.BUTTON_NEGATIVE,context.getResources().getString(R.string.dialog_button_cancel),new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which) {
                cancel();
            }
        });
        setButton(AlertDialog.BUTTON_NEUTRAL,context.getResources().getString(R.string.dialog_button_default),new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which) {
                file=new File(StorageUtil.getMainStoragePath());
                onDialogConfirmed();
            }
        });
    }

    private void onDialogConfirmed(){
        try{
            listener_confirmed.onFolderSelectorDialogConfirmed(file.getAbsolutePath());
            cancel();
        }catch (Exception e){e.printStackTrace();}
    }

    @Override
    public void show(){
        super.show();
        refreshPath(file);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(file!=null)positionRecords.putInt(file.getAbsolutePath().toLowerCase(),listView.getFirstVisiblePosition());
                if(position==0){
                    try{
                        File parent_file=file.getParentFile();
                        if(parent_file!=null) refreshPath(parent_file);
                        else cancel();
                    }catch (Exception e){e.printStackTrace();}
                    return;
                }
                refreshPath(list.get(position-1));
            }
        });
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected=(String)spinner.getSelectedItem();
                if(!CommonUtils.isChildPathOfCertainPath(file,new File(selected))){
                    refreshPath(new File(selected));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void refreshPath(final File file){
        this.file=file;
        list.clear();
        adapter.notifyDataSetChanged();
        dialogView.findViewById(R.id.folder_load).setVisibility(View.VISIBLE);
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (DialogOfFolderSelector.this){
                    final ArrayList<File>fileArrayList=new ArrayList<>();
                    try{
                        File[] files=file.listFiles();
                        for(File currentFile:files){
                            if(currentFile.isDirectory()) fileArrayList.add(currentFile);
                        }
                        Collections.sort(fileArrayList);
                    }catch (Exception e){
                        e.printStackTrace();
                    }finally {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                list.clear();
                                list.addAll(fileArrayList);
                                listView.setAdapter(new FileListAdapter());
                                ((TextView)dialogView.findViewById(R.id.folder_path)).setText(DialogOfFolderSelector.this.file.getAbsolutePath());
                                dialogView.findViewById(R.id.folder_load).setVisibility(View.GONE);
                                if(DialogOfFolderSelector.this.file!=null){
                                    listView.setSelection(positionRecords.getInt(DialogOfFolderSelector.this.file.getAbsolutePath().toLowerCase()));
                                }
                            }
                        });
                    }
                }
            }
        }).start();
    }

    public void setOnFolderSelectorDialogConfirmedListener(OnFolderSelectorDialogConfirmed listener){
        this.listener_confirmed=listener;
    }

    private class FileListAdapter extends BaseAdapter{
        @Override
        public int getCount() {
            return list.size()+1;
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
                convertView= LayoutInflater.from(context).inflate(R.layout.item_folder,parent,false);
            }
            if(position==0) {
                ((TextView)convertView.findViewById(R.id.item_folder_name)).setText("..");
                return convertView;
            }
            ((TextView)convertView.findViewById(R.id.item_folder_name)).setText(list.get(position-1).getName());
            return convertView;
        }
    }

    public interface OnFolderSelectorDialogConfirmed{
        void onFolderSelectorDialogConfirmed(String path);
    }

}
