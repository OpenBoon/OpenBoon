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

    /**
     * Create and return an update query.  Supports the postgres cast
     * operator (::) for on column names.   For example:
     *
     * update("foo", "pk_foo", "json::jsonb") would return "UPDATE foo SET json=?::jsonb WHERE pk_foo=?"
     */
    fun update(table: String, keyCol: String, vararg cols: String): String {
        val sb = StringBuilder(1024)
        sb.append("UPDATE $table SET ")
        for (col in cols) {
            val cast = col.contains("::")
            val parts = col.split("::")

            sb.append(parts[0])
            when {
                col.contains("=") -> sb.append(",")
                cast -> sb.append("=?::${parts[1]},")
                else -> sb.append("=?,")
            }
        }
        sb.deleteCharAt(sb.length - 1)
        sb.append(" WHERE $keyCol =?")
        return sb.toString()
    }

    fun inClause(col: String, size: Int): String {
        return when {
            size <=0 -> ""
            size == 1 -> "$col = ?"
            else -> {
                val sb = StringBuilder(128)
                sb.append("$col IN (")
                sb.append(StringUtils.repeat("?", ",", size))
                sb.append(") ")
                sb.toString()
            }
        }
    }

    fun inClause(col: String, size: Int, cast: String): String {
        val repeat = "?::$cast"
        return when {
            size<=0 -> ""
            size== 1-> "$col = $repeat"
            else -> {
                val sb = StringBuilder(128)
                sb.append("$col IN (")
                sb.append(StringUtils.repeat(repeat, ",", size))
                sb.append(") ")
                return sb.toString()
            }
        }
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
