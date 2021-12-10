package com.github.ghmxr.ftpshare.fragments;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.PermissionChecker;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.ghmxr.ftpshare.Constants;
import com.github.ghmxr.ftpshare.MyApplication;
import com.github.ghmxr.ftpshare.R;
import com.github.ghmxr.ftpshare.adapers.AccountListAdapter;
import com.github.ghmxr.ftpshare.services.FtpService;
import com.github.ghmxr.ftpshare.ui.DialogOfFolderSelector;
import com.github.ghmxr.ftpshare.utils.CommonUtils;

@Deprecated
public class AccountFragment extends Fragment implements View.OnClickListener{

    private ViewGroup viewGroup_anonymous;
    private ListView accountListView;
    private ViewGroup viewGroup_no_account;
    private TextView anonymous_path;
    private CheckBox writable_cb;

    private final SharedPreferences settings= CommonUtils.getSettingSharedPreferences(MyApplication.getGlobalBaseContext());
    private final SharedPreferences.Editor editor=settings.edit();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_account,container,false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewGroup_anonymous=view.findViewById(R.id.mode_anonymous);
        accountListView=view.findViewById(R.id.view_user_list);
        anonymous_path=view.findViewById(R.id.mode_anonymous_value);
        writable_cb=view.findViewById(R.id.anonymous_writable_cb);
        viewGroup_no_account=view.findViewById(R.id.add_user_att);

        view.findViewById(R.id.anonymous_path).setOnClickListener(this);
        view.findViewById(R.id.anonymous_writable).setOnClickListener(this);
        refreshContents();
    }

    /**
     * 在API22的虚拟机测试，当startActivityForResult()回来以后再切换fragment，这个方法不会被回调回来，暂不知道原因，所以
     * {@link #refreshContents()}方法只能调用多次来适配这些系统。(在{@link #onViewCreated(View, Bundle)})方法里加入了这个方法。
     * 重写这个方法是用来适配API21及以下版本系统的，因为onActivityResult()方法并不会在界面完成后回调，而是调用startActivityForResult()方法的瞬时就完成
     * 了回调，而且页面完成了也就不再回调了，暂不知原因。
     */
    @Override
    public void onResume() {
        super.onResume();
        refreshContents();
    }

    @Override
    public void onClick(View v) {
        if(getActivity()==null||getContext()==null||getView()==null)return;
        switch (v.getId()){
            default:break;
            case R.id.anonymous_path:{
                if(FtpService.isFTPServiceRunning()){
                    CommonUtils.showSnackBarOfFtpServiceIsRunning(getActivity());
                    return;
                }
                if(Build.VERSION.SDK_INT>=23&& PermissionChecker.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PermissionChecker.PERMISSION_GRANTED){
                    CommonUtils.showSnackBarOfRequestingWritingPermission(getActivity());
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},0);
                    return;
                }
                final Activity activity=getActivity();
                DialogOfFolderSelector dialog=new DialogOfFolderSelector(activity,
                        String.valueOf(settings.getString(Constants.PreferenceConsts.ANONYMOUS_MODE_PATH,Constants.PreferenceConsts.ANONYMOUS_MODE_PATH_DEFAULT)));
                dialog.show();
                dialog.setOnFolderSelectorDialogConfirmedListener(new DialogOfFolderSelector.OnFolderSelectorDialogConfirmed() {
                    @Override
                    public void onFolderSelectorDialogConfirmed(String path) {
                        if(FtpService.isFTPServiceRunning()){
                            Toast.makeText(activity,getResources().getString(R.string.attention_ftp_is_running),Toast.LENGTH_SHORT).show();
                            return;
                        }
                        editor.putString(Constants.PreferenceConsts.ANONYMOUS_MODE_PATH,path);
                        editor.apply();
                        anonymous_path.setText(path);
                    }
                });
            }
            break;
            case R.id.anonymous_writable:{
                if(FtpService.isFTPServiceRunning()){
                    CommonUtils.showSnackBarOfFtpServiceIsRunning(getActivity());
                    return;
                }
                writable_cb.toggle();
                editor.putBoolean(Constants.PreferenceConsts.ANONYMOUS_MODE_WRITABLE,writable_cb.isChecked());
                editor.apply();
            }
            break;
        }
    }

    public void processingActivityResult(int requestCode, int resultCode, Intent data) {
        refreshContents();
    }

    public void refreshContents(){
        if(getActivity()==null||getContext()==null||getView()==null)return;
        anonymous_path.setText(settings.getString(Constants.PreferenceConsts.ANONYMOUS_MODE_PATH,Constants.PreferenceConsts.ANONYMOUS_MODE_PATH_DEFAULT));
        writable_cb.setChecked(settings.getBoolean(Constants.PreferenceConsts.ANONYMOUS_MODE_WRITABLE,Constants.PreferenceConsts.ANONYMOUS_MODE_WRITABLE_DEFAULT));
        AccountListAdapter accountListAdapter=new AccountListAdapter(getActivity(),accountListView);
        accountListView.setAdapter(accountListAdapter);
        viewGroup_anonymous.setVisibility(CommonUtils.isAnonymousMode(getActivity())?View.VISIBLE:View.GONE);
        accountListView.setVisibility(CommonUtils.isAnonymousMode(getActivity())?View.GONE:View.VISIBLE);
        viewGroup_no_account.setVisibility(CommonUtils.isAnonymousMode(getActivity())?View.GONE:(accountListAdapter.getAccountItems().size()>0?View.GONE:View.VISIBLE));
    }

}
