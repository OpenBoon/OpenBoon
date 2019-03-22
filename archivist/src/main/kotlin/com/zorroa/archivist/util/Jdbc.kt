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
     * Create and return an insert query.  Supports the postgres cast operator (::)
     * for on column names.
     *
     * For example:
     * insert("foo", "pk_foo::uuid") would return "INSERT INTO foo (pk_foo) VALUES (?::uuid)"
     *
     * Supports calling a single function on a bind variable using the @ operator.
     *
     * Example:
     * insert("table", words@to_tsvector") would return "INSERT INTO table (words) VALUES (to_tsvector(?));
     *
     */
    fun insert(table: String, vararg cols: String): String {
        val sb = StringBuilder(1024)
        sb.append("INSERT INTO ")
        sb.append(table)
        sb.append("(")
        for(col in cols) {
            when {
                "::" in col -> sb.append(col.split("::").first())
                "@" in col -> sb.append(col.split("@").first())
                else -> sb.append(col)
            }
            sb.append(",")
        }
        sb.deleteCharAt(sb.lastIndex)
        sb.append(") VALUES (")
        for(col in  cols) {
            when {
                "::" in col -> sb.append("?::" + col.split("::").last())
                "@" in col -> sb.append(col.split("@").last() + "(?)")
                else -> sb.append("?")
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
     * Constuct an array overlap clause
     *
     * @param col: The column name
     * @param type: The column type
     * @param size: The number of elements the array to be compared.
     *
     * @return: An Postgres array overlap clause.
     *
     */
    fun arrayOverlapClause(col: String, type: String, size: Int): String {
        return when {
            size<=0 -> ""
            else -> {
                val sb = StringBuilder(128)
                sb.append("$col && ARRAY[")
                sb.append(StringUtils.repeat("?", ",", size))
                sb.append("]::$type[]")
                return sb.toString()
            }
        }
    }

    /**
     * Analyze a string and return a vector of words as a space delimited string.
     * Handles splitting words by periods, camel case.  TsWordVectors are used
     * for Postgres full text indexing.
     *
     * @param original The original text
     * @return The vectorized version.
     */
    fun getTsWordVector(original: String): String {
        return original.split(Regex("[_\\.-]+")).flatMap {
            it.split(Regex("(?<=[a-z])(?=[A-Z])"))
        }.joinToString(" ").toLowerCase()
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
