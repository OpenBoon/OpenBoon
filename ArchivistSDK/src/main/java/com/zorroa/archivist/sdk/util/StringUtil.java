package com.zorroa.archivist.sdk.util;

/**
 * Created by chambers on 11/6/15.
 */
public class StringUtil {

    public static String capitalize(String s) {
        return s.substring(0,1).toUpperCase() + s.substring(1);
    }

    public static String join(String[] array, String delimiter) {
        StringBuilder sb = new StringBuilder(512);
        for (String s: array) {
            sb.append(s);
            sb.append(",");
        }
        sb.deleteCharAt(sb.length()-1);
        return sb.toString();
    }
}
