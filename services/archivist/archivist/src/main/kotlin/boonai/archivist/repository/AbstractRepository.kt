package boonai.archivist.repository

import com.fasterxml.uuid.EthernetAddress
import com.fasterxml.uuid.Generators
import com.fasterxml.uuid.NoArgGenerator
import com.fasterxml.uuid.TimestampSynchronizer
import com.fasterxml.uuid.UUIDTimer
import com.fasterxml.uuid.impl.NameBasedGenerator
import com.fasterxml.uuid.impl.TimeBasedGenerator
import boonai.archivist.config.ApplicationProperties
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.JdbcTemplate
import java.util.Random
import java.util.concurrent.atomic.AtomicLong
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
    fun setApplicationProperties(properties: ApplicationProperties) {
        this.properties = properties
    }

    @Autowired
    fun setDatasource(dataSource: DataSource) {
        this.jdbc = JdbcTemplate(dataSource)
    }
}
