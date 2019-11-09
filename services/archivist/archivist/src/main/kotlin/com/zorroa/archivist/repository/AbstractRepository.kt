package com.zorroa.archivist.repository

import com.fasterxml.uuid.EthernetAddress
import com.fasterxml.uuid.Generators
import com.fasterxml.uuid.NoArgGenerator
import com.fasterxml.uuid.TimestampSynchronizer
import com.fasterxml.uuid.UUIDTimer
import com.fasterxml.uuid.impl.NameBasedGenerator
import com.fasterxml.uuid.impl.TimeBasedGenerator
import com.zorroa.archivist.config.ApplicationProperties
import com.zorroa.archivist.domain.Project
import io.micrometer.core.instrument.MeterRegistry
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.JdbcTemplate
import java.util.Random
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import javax.persistence.EntityManager
import javax.persistence.criteria.CriteriaBuilder
import javax.persistence.criteria.Predicate
import javax.sql.DataSource

/**
 * Lifted from M. Chamber's BBQ project.
 *
 * A utility function for catching a EmptyResultDataAccessException and rethrowing
 * the same function with a better message intended for the client.
 */
inline fun <T> throwWhenNotFound(msg: String, body: () -> T): T {

    try {
        return body()
    } catch (e: EmptyResultDataAccessException) {
        throw EmptyResultDataAccessException(msg, 1)
    } catch (e: IndexOutOfBoundsException) {
        throw EmptyResultDataAccessException(msg, 1)
    }
}

object UUIDGen {

    /**
     * Lifted from M. Chamber's BBQ project.
     *
     * Used to ensure a UUID generator does not generate colliding UUIDs.
     *
     * Apache II
     */
    private class UUIDSyncMechanism : TimestampSynchronizer() {

        val timer: AtomicLong = AtomicLong()

        override fun update(p0: Long): Long {
            timer.set(p0)
            return p0 + 1
        }

        override fun deactivate() {}

        override fun initialize(): Long {
            timer.set(System.nanoTime())
            return timer.toLong()
        }
    }

    val uuid3 =
        Generators.nameBasedGenerator(NameBasedGenerator.NAMESPACE_URL)

    val uuid1: NoArgGenerator = TimeBasedGenerator(
        EthernetAddress.fromInterface(),
        UUIDTimer(Random(), UUIDSyncMechanism())
    )
}

open class AbstractDao {

    val logger: Logger = LoggerFactory.getLogger(javaClass)

    protected val uuid1 = UUIDGen.uuid1

    protected val uuid3 = UUIDGen.uuid3

    protected lateinit var jdbc: JdbcTemplate

    protected lateinit var properties: ApplicationProperties
    
    @Autowired
    lateinit var meterRegistry: MeterRegistry

    @Autowired
    fun setApplicationProperties(properties: ApplicationProperties) {
        this.properties = properties
    }

    @Autowired
    fun setDatasource(dataSource: DataSource) {
        this.jdbc = JdbcTemplate(dataSource)
    }
}

@ApiModel("Long Range Filter", description = "Filters on a range using Longs.")
class LongRangeFilter(

    @ApiModelProperty("Values must be greater than this.")
    val greaterThan: Long?,

    @ApiModelProperty("Values must be less than this.")
    val lessThan: Long?,

    @ApiModelProperty("If true values matching the bounds will be included.")
    val inclusive: Boolean = true

) {
    /**
     * Return values needed to satisfy SQL query as list.
     */
    fun getFilterValues(): Iterable<Long> {
        val res = mutableListOf<Long>()
        if (greaterThan != null) {
            res.add(greaterThan)
        }
        if (lessThan != null) {
            res.add(lessThan)
        }
        return res
    }
}
