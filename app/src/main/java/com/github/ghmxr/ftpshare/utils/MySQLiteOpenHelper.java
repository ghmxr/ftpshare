package com.github.ghmxr.ftpshare.utils;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.github.ghmxr.ftpshare.Constants;

public class MySQLiteOpenHelper extends SQLiteOpenHelper {


    public MySQLiteOpenHelper(Context context){
        super(context, Constants.SQLConsts.SQL_USERS_FILENAME,null,Constants.SQLConsts.SQL_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table if not exists "+Constants.SQLConsts.TABLE_NAME+" ("
        +Constants.SQLConsts.COLUMN_ID+" integer primary key autoincrement not null,"
        +Constants.SQLConsts.COLUMN_ACCOUNT_NAME+" text,"
        +Constants.SQLConsts.COLUMN_PASSWORD+" text,"
        +Constants.SQLConsts.COLUMN_PATH +" text,"
        +Constants.SQLConsts.COLUMN_WRITABLE +" integer not null default 0);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

}
