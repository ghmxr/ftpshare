package org.apache.ftpserver.command.impl;

import com.github.ghmxr.ftpshare.Constants;
import com.github.ghmxr.ftpshare.services.FtpService;

import java.io.IOException;
import org.apache.ftpserver.command.AbstractCommand;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpRequest;
import org.apache.ftpserver.impl.FtpIoSession;
import org.apache.ftpserver.impl.FtpServerContext;
import org.apache.ftpserver.impl.LocalizedFtpReply;

public class OPTS_UTF8 extends AbstractCommand {
    public OPTS_UTF8() {
    }

    public void execute(FtpIoSession session, FtpServerContext context, FtpRequest request) throws IOException, FtpException {
        session.resetState();
        session.write(LocalizedFtpReply.translate(session, request, context,
                FtpService.getCharsetFromSharedPreferences().equals(Constants.PreferenceConsts.CHARSET_TYPE_DEFAULT)?200:502
                , "OPTS.UTF8", (String)null));
    }
}
