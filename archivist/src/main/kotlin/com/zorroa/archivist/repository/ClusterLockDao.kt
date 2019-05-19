package com.zorroa.archivist.repository

import com.zorroa.archivist.domain.ClusterLockExpired
import com.zorroa.archivist.domain.ClusterLockSpec
import com.zorroa.archivist.domain.LockStatus
import com.zorroa.archivist.domain.LogAction
import com.zorroa.archivist.domain.LogObject
import com.zorroa.archivist.service.MeterRegistryHolder.getTags
import com.zorroa.archivist.service.warnEvent
import com.zorroa.common.util.JdbcUtils
import io.micrometer.core.instrument.Tag
import org.springframework.dao.DataAccessException
import org.springframework.dao.DuplicateKeyException
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.net.InetAddress

interface ClusterLockDao {
    fun lock(spec: ClusterLockSpec): LockStatus
    fun unlock(name: String): Boolean
    fun isLocked(name: String): Boolean
    fun hasCombineLocks(spec: ClusterLockSpec): Boolean
    fun getExpired(): List<ClusterLockExpired>
    fun checkLock(name: String): Boolean
}

@Repository
class ClusterLockDaoImpl : AbstractDao(), ClusterLockDao {

    override fun lock(spec: ClusterLockSpec): LockStatus {
        val host = InetAddress.getLocalHost().hostAddress
        val time = System.currentTimeMillis()
        val expireTime = time + spec.timeoutUnits.toMillis(spec.timeout)

        if (spec.combineMultiple) {
            if (incrementCombineLock(spec.name)) {
                return LockStatus.Combined
            }
        }

        return try {
            jdbc.update { connection ->
                val ps = connection.prepareStatement(INSERT)
                ps.setString(1, spec.name)
                ps.setString(2, host)
                ps.setLong(3, time)
                ps.setLong(4, expireTime)
                ps.setBoolean(5, spec.combineMultiple)
                ps
            }
            meterRegistry.counter("zorrra.cluster_lock",
                    getTags(Tag.of("status", "locked"))).increment()
            LockStatus.Locked
        } catch (e: DuplicateKeyException) {
            logger.warnEvent(LogObject.CLUSTER_LOCK, LogAction.LOCK,
                    "Failure obtaining lock", mapOf("lockName" to spec.name))
            meterRegistry.counter("zorrra.cluster_lock",
                    getTags(Tag.of("status", "wait"))).increment()
            LockStatus.Wait
        }
    }

    fun incrementCombineLock(name: String): Boolean {
        try {
            jdbc.queryForObject("SELECT str_name FROM cluster_lock WHERE str_name=? FOR UPDATE NOWAIT",
                String::class.java, name)
            if (jdbc.update(INCREMENT_COMBINE_COUNT, name) == 1) {
                meterRegistry.counter("zorrra.cluster_lock",
                    getTags(Tag.of("status", "combine"))).increment()
                return true
            }
        } catch (e: DataAccessException) {
            logger.debug("Could not increment combined lock for $name")
        }
        return false
    }

    override fun unlock(name: String): Boolean {
        return jdbc.update("DELETE FROM cluster_lock WHERE str_name=?", name) == 1
    }

    override fun isLocked(name: String): Boolean {
        return jdbc.queryForObject("SELECT COUNT(1) FROM cluster_lock WHERE str_name=?", Int::class.java, name) == 1
    }

    override fun checkLock(name: String): Boolean {
        return try {
            jdbc.queryForObject(
                "SELECT str_name FROM cluster_lock WHERE str_name=? FOR UPDATE NOWAIT",
                String::class.java, name)
            true
        } catch (e: DataAccessException) {
            false
        }
    }

    override fun getExpired(): List<ClusterLockExpired> {
        return jdbc.query(GET_EXPIRED,
            RowMapper { rs, _ ->
                ClusterLockExpired(rs.getString("str_name"),
                    rs.getString("str_host"),
                    rs.getLong("time_expired"))
            }, System.currentTimeMillis())
    }

    override fun hasCombineLocks(spec: ClusterLockSpec): Boolean {
        return try {
            // Lock the row
            jdbc.queryForObject(
                "SELECT str_name FROM cluster_lock WHERE str_name=? AND bool_allow_combine='t' FOR UPDATE NOWAIT",
                String::class.java, spec.name)

            val newTimeout = System.currentTimeMillis() + spec.timeoutUnits.toMillis(spec.timeout)
            jdbc.update(HAS_COMBINED, newTimeout, spec.name) == 1
        } catch (e: DataAccessException) {
            // Lock no longer exists or is being updated by something else.
            false
        }
    }

    companion object {

        private val INSERT = JdbcUtils.insert("cluster_lock",
                "str_name",
                "str_host",
                "time_locked",
                "time_expired",
                "bool_allow_combine")

        private const val INCREMENT_COMBINE_COUNT = "UPDATE " +
                "cluster_lock " +
            "SET " +
                "int_combine_count=int_combine_count+1 " +
            "WHERE " +
                "bool_allow_combine='t' AND str_name=?"

        private const val HAS_COMBINED = "UPDATE " +
                "cluster_lock " +
            "SET " +
                "int_combine_count=0, " +
                "time_expired=? " +
            "WHERE " +
                "str_name=? " +
            "AND " +
                "int_combine_count > 0 " +
            "AND " +
                "bool_allow_combine = 't'"

        private const val GET_EXPIRED = "SELECT " +
                "str_name, " +
                "str_host, " +
                "time_expired " +
            "FROM " +
                "cluster_lock " +
            "WHERE " +
                "time_expired < ? ORDER BY str_name ASC"
    }
}