package com.zorroa.analyst.repository

import com.fasterxml.uuid.*
import com.fasterxml.uuid.impl.NameBasedGenerator
import com.fasterxml.uuid.impl.TimeBasedGenerator
import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.util.*
import java.util.concurrent.atomic.AtomicLong
import javax.sql.DataSource

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


open class AbstractJdbcDao {

    val logger : Logger = LoggerFactory.getLogger(javaClass)

    protected val uuid1 : NoArgGenerator =
            TimeBasedGenerator(EthernetAddress.fromInterface(),
                    UUIDTimer(Random(), UUIDSyncMechanism()))

    protected val uuid3 =
            Generators.nameBasedGenerator(NameBasedGenerator.NAMESPACE_URL)

    protected lateinit var jdbc: JdbcTemplate

    @Autowired
    fun setDatasource(dataSource: DataSource) {
        this.jdbc = JdbcTemplate(dataSource)
    }
}

fun sqlInsert(table: String, vararg cols: String): String {
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

fun sqlUpdate(table: String, keyCol: String, vararg cols: String): String {
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

fun sqlIn(col: String, size: Int): String {
    val sb = StringBuilder(size * 2 * 2)
    sb.append(col)
    sb.append(" IN (")
    sb.append(StringUtils.repeat("?", ",", size))
    sb.append(") ")
    return sb.toString()
}

fun sqlIn(col: String, size: Int, cast: String): String {
    val repeat = "?::$cast"
    val sb = StringBuilder(size * 2 * 2)
    sb.append(col)
    sb.append(" IN (")
    sb.append(StringUtils.repeat(repeat, ",", size))
    sb.append(") ")
    return sb.toString()
}
