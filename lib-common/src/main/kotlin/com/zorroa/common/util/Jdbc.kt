package com.zorroa.common.util

import org.apache.commons.lang3.StringUtils
import java.util.regex.Pattern

object JdbcUtils {

    private val UUID_REGEX = Pattern.compile("[0-9a-f\\-]{36}",
            Pattern.CASE_INSENSITIVE)

    fun isUUID(value: String): Boolean {
        return UUID_REGEX.matcher(value).matches()
    }

    fun insert(table: String, vararg cols: String): String {
        val sb = StringBuilder(1024)
        sb.append("INSERT INTO ")
        sb.append(table)
        sb.append("(")
        sb.append(StringUtils.join(cols, ","))
        sb.append(") VALUES (")
        sb.append(StringUtils.repeat("?", ",", cols.size))
        sb.append(")")
        return sb.toString()
    }

    fun update(table: String, keyCol: String, vararg cols: String): String {
        val sb = StringBuilder(1024)
        sb.append("UPDATE ")
        sb.append(table)
        sb.append(" SET ")
        for (col in cols) {
            sb.append(col)
            if (col.contains("=")) {
                sb.append(",")
            } else {
                sb.append("=?,")
            }
        }
        sb.deleteCharAt(sb.length - 1)
        sb.append(" WHERE ")
        sb.append(keyCol)
        sb.append("=?")
        return sb.toString()
    }

    fun inClause(col: String, size: Int): String {
        val sb = StringBuilder(size * 2 * 2)
        sb.append(col)
        sb.append(" IN (")
        sb.append(StringUtils.repeat("?", ",", size))
        sb.append(") ")
        return sb.toString()
    }

    fun inClause(col: String, size: Int, cast: String): String {
        val repeat = "?::$cast"
        val sb = StringBuilder(size * 2 * 2)
        sb.append(col)
        sb.append(" IN (")
        sb.append(StringUtils.repeat(repeat, ",", size))
        sb.append(") ")
        return sb.toString()
    }

}
