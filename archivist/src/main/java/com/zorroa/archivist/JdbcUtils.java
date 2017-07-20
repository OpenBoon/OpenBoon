package com.zorroa.archivist;
/*
*    Copyright 2014 Matthew Chambers
*
*    Copied from the Plow Render Farm
*
*    Licensed under the Apache License, Version 2.0 (the "License");
*    you may not use this file except in compliance with the License.
*    You may obtain a copy of the License at
*
*        http://www.apache.org/licenses/LICENSE-2.0
*
*    Unless required by applicable law or agreed to in writing, software
*    distributed under the License is distributed on an "AS IS" BASIS,
*    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*    See the License for the specific language governing permissions and
*    limitations under the License.
*/


import org.apache.commons.lang3.StringUtils;

import java.util.Collection;

public class JdbcUtils {

    public static boolean isValid(Collection<?> collection) {
        return collection == null ? false : !collection.isEmpty();
    }

    public static boolean isValid(String str) {
        return str == null ? false : !str.isEmpty();
    }

    public static boolean isValid(Object obj) {
        return obj == null ? false : true;
    }

    public static String insert(String table, String ... cols) {
        final StringBuilder sb = new StringBuilder(1024);
        sb.append("INSERT INTO ");
        sb.append(table);
        sb.append("(");
        sb.append(StringUtils.join(cols, ","));
        sb.append(") VALUES (");
        sb.append(StringUtils.repeat("?",",", cols.length));
        sb.append(")");
        return sb.toString();
    }

    public static String update(String table, String keyCol, String ... cols) {
        final StringBuilder sb = new StringBuilder(1024);
        sb.append("UPDATE ");
        sb.append(table);
        sb.append(" SET ");
        for (String col: cols) {
            sb.append(col);
            if (col.contains("=")) {
                sb.append(",");
            }
            else {
                sb.append("=?,");
            }
        }
        sb.deleteCharAt(sb.length()-1);
        sb.append(" WHERE ");
        sb.append(keyCol);
        sb.append("=?");
        return sb.toString();
    }

    public static String in(String col, int size) {
        StringBuilder sb = new StringBuilder(size * 2 * 2);
        sb.append(col);
        sb.append(" IN (");
        sb.append(StringUtils.repeat("?",",", size));
        sb.append(") ");
        return sb.toString();
    }

    public static String in(String col, int size, String cast) {
        final String repeat = "?::" + cast;
        StringBuilder sb = new StringBuilder(size * 2 * 2);
        sb.append(col);
        sb.append(" IN (");
        sb.append(StringUtils.repeat(repeat,",", size));
        sb.append(") ");
        return sb.toString();
    }
}
