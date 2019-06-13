package com.github.ghmxr.ftpshare.data;

import com.github.ghmxr.ftpshare.utils.Storage;

import java.io.Serializable;

public class AccountItem implements Serializable{
    public long id=-1;
    public String account="";
    public String password="";
    public String path= Storage.getMainStoragePath();
    public boolean writable=false;

    @Override
    public String toString() {
        return "AccountItem{" +
                "id=" + id +
                ", account='" + account + '\'' +
                ", password='" + password + '\'' +
                ", path='" + path + '\'' +
                ", writable=" + writable +
                '}';
    }
}
