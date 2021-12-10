package com.github.ghmxr.ftpshare.ftpclient;

import com.github.ghmxr.ftpshare.data.ClientBean;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import java.io.IOException;
import java.net.SocketException;

public class FtpUtil {
    //private static Logger logger = Logger.getLogger(FtpUtil.class);


    /**
     * 获取FTPClient对象
     *
     * @return
     */
    public static FTPClient getFTPClient(ClientBean bean) {
        FTPClient ftpClient = new FTPClient();
        FtpClientManager.getInstance();
        try {
            ftpClient.connect(bean.getHost(), bean.getPort());// 连接FTP服务器
            ftpClient.login(bean.getUserName(), bean.getPassword());// 登陆FTP服务器
            if (!FTPReply.isPositiveCompletion(ftpClient.getReplyCode())) {
                //logger.info("未连接到FTP，用户名或密码错误。");
                ftpClient.disconnect();
            } else {
                //logger.info("FTP连接成功。");
                FTPFile[] ftpFiles = ftpClient.listFiles("/");

                ftpFiles[0].getName();
                ftpFiles[0].getType();
            }

        } catch (SocketException e) {
            e.printStackTrace();
            //logger.info("FTP的IP地址可能错误，请正确配置。");
        } catch (IOException e) {
            e.printStackTrace();
            //logger.info("FTP的端口错误,请正确配置。");
        }
        return ftpClient;
    }
}