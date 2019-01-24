package com.github.ghmxr.ftpshare;

import com.github.ghmxr.ftpshare.utils.Storage;

public class Constants {
    public static class SQLConsts{
        public static final String SQL_USERS_FILENAME ="ftp_accounts.db";
        public static final int SQL_VERSION=1;
        public static final String TABLE_NAME="ftp_account_table";
        public static final String COLUMN_ID="_id";
        public static final String COLUMN_ACCOUNT_NAME="name";
        public static final String COLUMN_PASSWORD="password";
        public static final String COLUMN_PATH ="path";
        public static final String COLUMN_WRITABLE ="writable";
    }
    public static class FTPConsts{
        public static final String NAME_ANONYMOUS="anonymous";
    }
    public static class PreferenceConsts{
        public static final String FILE_NAME ="settings";
        /**
         * this stands for a boolean value
         */
        public static final String ANONYMOUS_MODE="anonymous_mode";
        public static final boolean ANONYMOUS_MODE_DEFAULT=true;
        /**
         * this stands for a string value
         */
        public static final String ANONYMOUS_MODE_PATH="anonymous_mode_path";
        public static final String ANONYMOUS_MODE_PATH_DEFAULT=Storage.getMainStoragePath();
        /**
         * this stands for a boolean value
         */
        public static final String ANONYMOUS_MODE_WRITABLE="anonymous_mode_writable";
        public static final boolean ANONYMOUS_MODE_WRITABLE_DEFAULT=false;

        /**
         * this stands for a boolean value
         */
        public static final String WAKE_LOCK="wake_lock";
        public static final boolean WAKE_LOCK_DEFAULT=false;
        /**
         * this stands for a int value
         */
        public static final String PORT_NUMBER="port_number";
        public static final int PORT_NUMBER_DEFAULT=5656;
    }

}
