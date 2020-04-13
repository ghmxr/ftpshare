# FTP Share (Scroll down for English)
一个基于Apache FtpServer组件开发的Android平台实现FTP文件共享的应用。

+ 可使用匿名模式登录，并指定共享主路径和设置写入权限；

+ 可设定若干用户，并单独指定共享主路径和写入权限；

+ 可以设定主路径至外置存储（由于Android版本和设备型号不同，实际使用可能会有差异）；

+ 可以选择在某些情况下自动断开FTP服务，例如WiFi连接断开时，热点断开时，指定时间后；

+ 暗色模式，中英文模式。

关于外部调用FTP服务:

本应用的FTP服务是对外暴露的，如果希望通过其他应用或者自动化应用例如Tasker来启动本应用的FTP服务，请参考下面的代码和参数：

~~~
Intent intent=new Intent();
intent.setComponent(new ComponentName("com.github.ghmxr.ftpshare","com.github.ghmxr.ftpshare.services.FtpService"));
context.startService(intent);//启动FTP服务
context.stopService(intent);//停止FTP服务
~~~


*********************
The English contents are translated by machine:

An application of FTP file sharing based on Android platform developed by Apache FTP server component.

+ You can use anonymous mode to log in, specify the shared main path and set the write permission;

+ Several users can be set, and the shared main path and write permission can be specified separately;

+ You can set the main path to external storage (due to different Android versions and device models, the actual use may be different);

+ You can choose to disconnect FTP service automatically in some cases, such as when WiFi connection is disconnected, when hotspot is disconnected, after a specified time;

+ Dark mode, Chinese and English support.

About external calls to the FTP service:

The FTP service of this application is exposed to outside. If you want to start the FTP service of this application through other applications or automation applications such as Tasker, please refer to the following codes and parameters:

~~~
Intent intent=new Intent();
intent.setComponent(new ComponentName("com.github.ghmxr.ftpshare","com.github.ghmxr.ftpshare.services.FtpService"));
context.startService(intent);//Start the FTP service
context.stopService(intent);//Stop the FTP service
~~~

*********************
酷安市场：<a href="https://www.coolapk.com/apk/com.github.ghmxr.ftpshare">https://www.coolapk.com/apk/com.github.ghmxr.ftpshare</a>

<div align="center">
    <img src="https://github.com/ghmxr/ftpshare/raw/master/preview/ftpshare_1.png" alt="avator" title="" width="200"/>
	<img src="https://github.com/ghmxr/ftpshare/raw/master/preview/ftpshare_2.png" alt="avator" title="" width="200"/>
	<img src="https://github.com/ghmxr/ftpshare/raw/master/preview/ftpshare_3.png" alt="avator" title="" width="200"/>
	<img src="https://github.com/ghmxr/ftpshare/raw/master/preview/ftpshare_4.png" alt="avator" title="" width="200"/>
</div>
