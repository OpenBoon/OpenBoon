package com.zorroa.common.util

import com.zorroa.archivist.repository.LongRangeFilter
import org.apache.commons.lang3.StringUtils
import java.util.regex.Pattern

object JdbcUtils {

    private val UUID_REGEX = Pattern.compile("[0-9a-f\\-]{36}",
            Pattern.CASE_INSENSITIVE)

    fun isUUID(value: String): Boolean {
        return UUID_REGEX.matcher(value).matches()
    }

    /**
     * Create and return an insert query.  Supports the postgres cast
     * operator (::) for on column names.   For example:
     *
     * insert("foo", "pk_foo::uuid") would return "INSERT INTO foo (pk_foo) VALUES (?::uuid)"
     */
    fun insert(table: String, vararg cols: String): String {
        val sb = StringBuilder(1024)
        sb.append("INSERT INTO ")
        sb.append(table)
        sb.append("(")
        for(col in  cols) {
            if ("::" in col) {
                sb.append(col.split("::").first())
            }
            else {
                sb.append(col)
            }
            sb.append(",")
        }
        sb.deleteCharAt(sb.lastIndex)
        sb.append(") VALUES (")
        for(col in  cols) {
            if ("::" in col) {
                sb.append("?::"+col.split("::").last())
            }
            else {
                sb.append("?")
            }
            sb.append(",")
        }
        sb.deleteCharAt(sb.lastIndex)
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
        if (size == 0) { return "" }
        val sb = StringBuilder(size * 2 * 2)
        sb.append(col)
        sb.append(" IN (")
        sb.append(StringUtils.repeat("?", ",", size))
        sb.append(") ")
        return sb.toString()
    }

    fun inClause(col: String, size: Int, cast: String): String {
        if (size == 0) { return "" }
        val repeat = "?::$cast"
        val sb = StringBuilder(size * 2 * 2)
        sb.append(col)
        sb.append(" IN (")
        sb.append(StringUtils.repeat(repeat, ",", size))
        sb.append(") ")
        return sb.toString()
    }

    /**
     * Create a WHERE clause fragment using a LongRangeFilter
     *
     * @param col The column name.
     * @param rng A LongRangeFilter
     */
    fun rangeClause(col: String, rng: LongRangeFilter): String {
        val sb = StringBuilder(32)
        val eq = if(rng.inclusive) { "="} else ""

        if (rng.greaterThan != null) {
            sb.append(" $col>$eq? ")
            if (rng.lessThan != null) {
                sb.append( "AND ")
            }
        }

        if (rng.lessThan != null) {
            sb.append(" $col<$eq? ")
        }
        return sb.toString()
    }

}
