package com.github.ghmxr.ftpshare.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.WritePermission;

import java.util.ArrayList;
import java.util.List;

public class FtpService extends Service {
    public static FtpServer server;
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try{
           FtpServerFactory factory=new FtpServerFactory();

            BaseUser baseUser = new BaseUser();
            baseUser.setName("user");
            baseUser.setPassword("123");
            baseUser.setHomeDirectory("/storage/emulated/0");

            List<Authority> authorities = new ArrayList<Authority>();
            authorities.add(new WritePermission());
            baseUser.setAuthorities(authorities);
            factory.getUserManager().save(baseUser);

            ListenerFactory lfactory = new ListenerFactory();
            lfactory.setPort(65535); //设置端口号 非ROOT不可使用1024以下的端口
            factory.addListener("default", lfactory.createListener());

           server=factory.createServer();
           server.start();
        }catch (Exception e){
            e.printStackTrace();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try{
            server.stop();
            server=null;
        }catch (Exception e){
            e.printStackTrace();
        }

    }
}
