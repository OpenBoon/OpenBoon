package com.zorroa.common.repository

import com.fasterxml.jackson.annotation.JsonIgnore
import org.slf4j.LoggerFactory
import java.util.*

abstract class DaoFilter {

    @JsonIgnore
    private var built = false

    @JsonIgnore
    private var whereClause: String? = null

    @JsonIgnore
    protected var where: MutableList<String> = mutableListOf()

    @JsonIgnore
    protected var values: MutableList<Any> = mutableListOf()

    var page : KPage = KPage()

    var sort: Map<String, String> = mutableMapOf()

    constructor()

    constructor(page: KPage) {
        this.page = page
    }

    @get:JsonIgnore
    abstract val sortMap: Map<String, String>?

    abstract fun build()

    fun addToWhere(col: String) {
        this.where.add(col)
    }

    fun addToValues(vararg value: Any) {
        for (o in value) {
            values.add(o)
        }
    }

    fun addToValues(items: Collection<Any>) {
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

        if (sortMap != null && !sort.isEmpty()) {
            val order = StringBuilder(64)
            for ((key, value) in sort) {
                val col = sortMap!![key]
                if (col != null) {
                    order.append(col + " " + if (value.startsWith("a")) "asc " else "desc ")
                    order.append(",")
                }
            }
            if (order.isNotEmpty()) {
                order.deleteCharAt(order.length - 1)
                sb.append(" ORDER BY ")
                sb.append(order)
            }
        }

        if (!forCount) {
            sb.append(" LIMIT ? OFFSET ?")
        }

        val s =  sb.toString()
        logger.info(s)
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
        return if (forCount) {
            values.toTypedArray()
        }
        else {
            val result = Arrays.copyOf(values.toTypedArray(), values.size + 2)
            result[values.size] = page.size
            result[values.size + 1] = page.from
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
    fun forceSort(sort: Map<String, String>): DaoFilter {
        this.sort = sort
        return this
    }

    companion object {

        protected val logger = LoggerFactory.getLogger(DaoFilter::class.java)
    }
}
