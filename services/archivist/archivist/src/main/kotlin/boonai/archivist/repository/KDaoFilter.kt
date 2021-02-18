package boonai.archivist.repository

import com.fasterxml.jackson.annotation.JsonIgnore
import org.slf4j.LoggerFactory
import java.util.Arrays

abstract class KDaoFilter {

    @JsonIgnore
    private var built = false

    @JsonIgnore
    private var whereClause: String? = null

    @JsonIgnore
    protected var where: MutableList<String> = mutableListOf()

    @JsonIgnore
    protected var values: MutableList<Any> = mutableListOf()

    var page: KPage = KPage()

    var sort: List<String>? = null

    var sortRaw: List<String>? = null

    constructor()

    constructor(page: KPage) {
        this.page = page
    }

    abstract val sortMap: Map<String, String>

    abstract fun build()

    fun addToWhere(col: String) {
        if (col.isBlank()) { return }
        this.where.add(col)
    }

    fun addToValues(vararg value: Any) {
        for (o in value) {
            values.add(o)
        }
    }

    fun addToValues(items: Iterable<Any>) {
        values.addAll(items)
    }

    fun getQuery(base: String, forCount: Boolean = false): String {
        __build()
        val sb = StringBuilder(1024)

        sb.append(base)
        sb.append(" ")
        if (!whereClause.isNullOrEmpty()) {
            if (!base.contains("WHERE")) {
                sb.append(" WHERE ")
            }
            sb.append(whereClause)
        }

        if (!forCount && (sort != null || sortRaw != null)) {
            val order = StringBuilder(128)

            sortRaw?.forEach {
                order.append(it)
                order.append(",")
                println(order)
            }

            sort?.forEach { e ->
                val (key, dir) = e.split(":", limit = 2)
                val col = sortMap[key]
                if (col != null) {
                    order.append(col + " " + if (dir.startsWith("a", ignoreCase = true)) "asc " else "desc ")
                    order.append(",")
                } else {
                    throw IllegalArgumentException("Invalid sort column: '$col'")
                }
            }

            if (order.isNotEmpty()) {
                order.deleteCharAt(order.length - 1)
                sb.append(" ORDER BY ")
                sb.append(order)
            }
        }

        if (!forCount && !page.disabled) {
            sb.append(" LIMIT ? OFFSET ?")
        }

        val s = sb.toString()
        if (logger.isDebugEnabled) {
            logger.debug(s)
        }
        return s
    }

    fun getCountQuery(base: String): String {
        __build()

        val sb = StringBuilder(1024)
        sb.append(base)
        if (!whereClause.isNullOrEmpty()) {
            if (!base.contains("WHERE")) {
                sb.append(" WHERE ")
            }
            sb.append(whereClause)
        }
        return sb.toString()
    }

    fun getValues(forCount: Boolean = false): Array<Any> {
        __build()
        return if (forCount || page.disabled) {
            values.toTypedArray()
        } else {
            val result = Arrays.copyOf(values.toTypedArray(), values.size + 2)
            result[values.size] = page?.size
            result[values.size + 1] = page?.from
            result
        }
    }

    private fun __build() {
        if (!built) {
            built = true
            build()

            if (where.isNotEmpty()) {
                whereClause = where.joinToString(" AND ")
            }
            where.clear()
        }
    }

    @JsonIgnore
    fun forceSort(sort: List<String>): KDaoFilter {
        this.sort = sort
        return this
    }

    companion object {

        protected val logger = LoggerFactory.getLogger(KDaoFilter::class.java)
    }
}
