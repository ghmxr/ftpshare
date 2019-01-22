package com.github.ghmxr.ftpshare.ui;

import android.content.Context;
import android.content.DialogInterface;
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
import com.github.ghmxr.ftpshare.utils.Storage;
import com.github.ghmxr.ftpshare.utils.ValueUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DialogOfFolderSelector extends AlertDialog {
    private View dialogView;
    private Context context;
    private File file;
    private List<File> list=new ArrayList<>();
    private String storage_path="";
    private Spinner spinner;
    private FileListAdapter adapter;
    private OnFolderSelectorDialogConfirmed listener_confirmed;

    public DialogOfFolderSelector(@NonNull Context context,@NonNull String path) {
        super(context);
        this.context=context;
        dialogView=LayoutInflater.from(context).inflate(R.layout.layout_dialog_folder_selector,null);
        setView(dialogView);
        setTitle(context.getResources().getString(R.string.account_path_dialog_title));
        spinner=dialogView.findViewById(R.id.folder_storage);
        try{
            file=new File(path);
        }catch (Exception e){
            e.printStackTrace();
            file=new File(Storage.getMainStoragePath());
        }
        try{
            List<String> storages=Storage.getAvailableStoragePaths();
            spinner.setAdapter(new ArrayAdapter<>(context,R.layout.item_spinner_storage,R.id.item_storage_text,storages));
            for(int i=0;i<storages.size();i++){
                if(ValueUtil.isChildPathOfCertainPath(file, new File(storages.get(i)))) {
                    spinner.setSelection(i);
                    storage_path=(String)spinner.getSelectedItem();
                    break;
                }
            }
        }catch (Exception e){e.printStackTrace();}

        adapter=new FileListAdapter();
        ((ListView)dialogView.findViewById(R.id.folder_list)).setAdapter(adapter);
        setButton(AlertDialog.BUTTON_POSITIVE, context.getResources().getString(R.string.dialog_button_confirm), new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try{
                    listener_confirmed.onFolderSelectorDialogConfirmed(file.getAbsolutePath());
                    cancel();
                }catch (Exception e){e.printStackTrace();}
            }
        });
        setButton(AlertDialog.BUTTON_NEGATIVE,context.getResources().getString(R.string.dialog_button_cancel),new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which) {
                cancel();
            }
        });
    }

    @Override
    public void show(){
        super.show();
        refreshPath(file);
        ((ListView)dialogView.findViewById(R.id.folder_list)).setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
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
                if(!storage_path.equals((String)spinner.getSelectedItem())){
                    refreshPath(new File((String)spinner.getSelectedItem()));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void refreshPath(File file){
        this.file=file;
        list.clear();
        try{
            File[] files=this.file.listFiles();
            for(File currentFile:files){
                if(currentFile.isDirectory()) list.add(currentFile);
            }
        }catch (Exception e){e.printStackTrace();}
        adapter.notifyDataSetChanged();
        String display_path=this.file.getAbsolutePath();
        if(display_path.length()>70) display_path="..."+display_path.substring(display_path.length()-70,display_path.length());
        ((TextView)dialogView.findViewById(R.id.folder_path)).setText(display_path);
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
                convertView= LayoutInflater.from(context).inflate(R.layout.item_folder,null);
            }
            if(position==0) {
                ((TextView)convertView.findViewById(R.id.item_folder_name)).setText("...");
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
