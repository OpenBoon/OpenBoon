package com.zorroa.archivist.repository

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.uuid.*
import com.fasterxml.uuid.impl.NameBasedGenerator
import com.fasterxml.uuid.impl.TimeBasedGenerator
import com.google.common.collect.Lists
import com.zorroa.archivist.JdbcUtils
import com.zorroa.archivist.sdk.config.ApplicationProperties
import com.zorroa.sdk.domain.PagedList
import com.zorroa.sdk.domain.Pager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.util.*
import java.util.concurrent.atomic.AtomicLong
import javax.sql.DataSource

interface GenericDao<T, in S> {

    fun getAll(): List<T>

    fun create(spec: S): T

    fun get(id: UUID): T

    fun refresh(obj: T): T

    fun getAll(paging: Pager): PagedList<T>

    fun update(id: UUID, spec: T): Boolean

    fun delete(id: UUID): Boolean

    fun count(): Long
}

interface GenericNamedDao<T, S> : GenericDao<T, S> {

    fun get(name: String): T

    fun exists(name: String): Boolean
}

/**
 * Lifted from M. Chamber's FXP project.
 *
 * Used to ensure a UUID generator does not generate colliding UUIDs.
 *
 * Apache II
 */
class UUIDSyncMechanism : TimestampSynchronizer() {

    val timer : AtomicLong = AtomicLong()

    override fun update(p0:Long) : Long {
        timer.set(p0)
        return p0 + 1
    }

    override fun deactivate() {}

    override fun initialize(): Long {
        timer.set(System.nanoTime())
        return timer.toLong()
    }
}


open class AbstractDao {

    val logger : Logger = LoggerFactory.getLogger(javaClass)

    protected val uuid1 : NoArgGenerator =
            TimeBasedGenerator(EthernetAddress.fromInterface(),
                    UUIDTimer(Random(), UUIDSyncMechanism()))

    protected val uuid3 =
            Generators.nameBasedGenerator(NameBasedGenerator.NAMESPACE_URL)

    protected lateinit var jdbc: JdbcTemplate

    protected lateinit var properties : ApplicationProperties

    private lateinit var dbVendor: String

    fun isDbVendor(vendor: String): Boolean {
        return dbVendor == vendor
    }

    @Autowired
    fun setApplicationProperties(properties: ApplicationProperties) {
        this.properties = properties
        this.dbVendor = properties.getString("archivist.datasource.primary.vendor")
    }

    @Autowired
    fun setDatasource(dataSource: DataSource) {
        this.jdbc = JdbcTemplate(dataSource)
    }
}


abstract class DaoFilter {

    @JsonIgnore
    private var built = false

    @JsonIgnore
    private var whereClause: String? = null

    @JsonIgnore
    protected var where: MutableList<String> = Lists.newArrayList()

    @JsonIgnore
    protected var values: MutableList<Any> = Lists.newArrayList()

    var sort: Map<String, String> = mutableMapOf()

    @get:JsonIgnore
    abstract val sortMap: Map<String, String>?

    abstract fun build()

    fun addToWhere(col: String) {
        this.where.add(col)
    }

    fun addToValues(vararg `val`: Any) {
        for (o in `val`) {
            values.add(o)
        }
    }

    fun addToValues(items: Collection<Any>) {
        values.addAll(items)
    }

    fun getQuery(base: String, page: Pager?): String {
        __build()
        val sb = StringBuilder(1024)
        sb.append(base)
        sb.append(" ")
        if (JdbcUtils.isValid(whereClause)) {
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

        if (page != null) {
            sb.append(" LIMIT ? OFFSET ?")
        }

        return sb.toString()
    }

    fun getCountQuery(base: String): String {
        __build()

        val sb = StringBuilder(1024)
        sb.append("SELECT COUNT(1) FROM ")
        sb.append(base.substring(base.indexOf("FROM") + 5))
        if (JdbcUtils.isValid(whereClause)) {
            if (!base.contains("WHERE")) {
                sb.append(" WHERE ")
            }
            sb.append(whereClause)
        }
        return sb.toString()
    }

    fun getValues(): Array<Any> {
        __build()
        return values.toTypedArray()
    }

    fun getValues(page: Pager): Array<Any> {
        __build()
        val result = Arrays.copyOf(values.toTypedArray(), values.size + 2)
        result[values.size] = page.size
        result[values.size + 1] = page.from
        return result
    }

    private fun __build() {
        if (!built) {
            built = true
            build()

            if (JdbcUtils.isValid(where)) {
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
