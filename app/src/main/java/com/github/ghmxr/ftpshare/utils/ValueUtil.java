package com.github.ghmxr.ftpshare.utils;

import java.io.File;
import java.util.Locale;

public class ValueUtil {
    /**
     * judge if it is a child path of parent
     */
    public static boolean isChildPathOfCertainPath(File child,File parent){
        try {
            if(child.getAbsolutePath().trim().toLowerCase(Locale.getDefault()).equals(parent.getAbsolutePath().trim().toLowerCase(Locale.getDefault()))) return true;
            else{
                while((child=child.getParentFile())!=null){
                    if(child.getAbsolutePath().trim().toLowerCase(Locale.getDefault()).equals(parent.getAbsolutePath().trim().toLowerCase(Locale.getDefault()))) return true;
                }
            }
        }catch (Exception e){e.printStackTrace();}
        return false;
    }
}
