package com.github.ghmxr.ftpshare.data;

import com.github.ghmxr.ftpshare.utils.Storage;

public class AccountItem{
    public long id=-1;
    public String account="";
    public String password="";
    public String path= Storage.getMainStoragePath();
    public boolean writable=false;
    public AccountItem(){}
    public AccountItem(AccountItem another){
        this.account=new String(another.account);
        this.password=new String(another.account);
        this.path=new String(another.account);
        this.writable=another.writable;
    }
}
