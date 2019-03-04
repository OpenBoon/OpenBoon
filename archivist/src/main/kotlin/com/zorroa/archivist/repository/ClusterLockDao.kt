package com.zorroa.archivist.repository

import com.zorroa.archivist.domain.ClusterLockSpec
import com.zorroa.archivist.domain.LockStatus
import com.zorroa.archivist.domain.LogAction
import com.zorroa.archivist.domain.LogObject
import com.zorroa.archivist.service.event
import com.zorroa.archivist.service.warnEvent
import com.zorroa.common.util.JdbcUtils
import org.springframework.dao.DuplicateKeyException
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.RowCallbackHandler
import org.springframework.stereotype.Repository
import java.net.InetAddress
import java.time.Instant
import java.util.*

interface ClusterLockDao {
    fun clearExpired() : Int
    fun lock(spec: ClusterLockSpec) : LockStatus
    fun unlock(name: String) : Boolean
    fun isLocked(name: String): Boolean
    fun combineLocks(spec: ClusterLockSpec) : Boolean
}

@Repository
class ClusterLockDaoImpl : AbstractDao(), ClusterLockDao {

    override fun lock(spec: ClusterLockSpec): LockStatus {
        val host = InetAddress.getLocalHost().hostAddress
        val time = System.currentTimeMillis()
        val expireTime = time + spec.timeoutUnits.toMillis(spec.timeout)

        if (spec.combineMultiple &&
                jdbc.update(INCREMENT_COMBINE_COUNT, spec.name) == 1) {
            logger.event(LogObject.CLUSTER_LOCK, LogAction.COMBINE, mapOf("lockName" to spec.name))
            return LockStatus.Combined
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
            logger.event(LogObject.CLUSTER_LOCK, LogAction.LOCK,
                    mapOf("lockName" to spec.name, "expireTime" to expireTime))
            LockStatus.Locked
        } catch (e: DuplicateKeyException) {
            logger.warnEvent(LogObject.CLUSTER_LOCK, LogAction.LOCK,
                    "Failure obtaining lock", mapOf("lockName" to spec.name))
            LockStatus.Wait
        }
    }

    override fun unlock(name: String): Boolean {
        logger.event(LogObject.CLUSTER_LOCK, LogAction.UNLOCK, mapOf("lockName" to name))
        return jdbc.update("DELETE FROM cluster_lock WHERE str_name=?", name) == 1
    }

    override fun isLocked(name: String): Boolean {
        return jdbc.queryForObject("SELECT COUNT(1) FROM cluster_lock WHERE str_name=?", Int::class.java, name) == 1
    }

    override fun clearExpired(): Int {
        val time = System.currentTimeMillis()
        var removed = 0
        jdbc.query(GET_EXPIRED, RowCallbackHandler { rs->
            val name = rs.getString("str_name")
            jdbc.update("DELETE FROM cluster_lock WHERE str_name=?", name)
            logger.event(LogObject.CLUSTER_LOCK, LogAction.EXPIRED, mapOf("lockName" to name))
            removed+=1
        }, time)
        return removed
    }

    override fun combineLocks(spec: ClusterLockSpec) : Boolean {
        try {
            // Lock the row
            jdbc.queryForObject("SELECT str_name FROM cluster_lock WHERE str_name=? FOR UPDATE",
                    String::class.java, spec.name)
        } catch (e: EmptyResultDataAccessException) {
            // Lock no longer exists.
            return false
        }

        val newTimeout = System.currentTimeMillis() + spec.timeoutUnits.toMillis(spec.timeout)
        val combine =  jdbc.update(HAS_COMBINED, newTimeout, spec.name) == 1

        if (!combine) {
            jdbc.update("UPDATE cluster_lock SET bool_allow_combine='f' WHERE str_name=?", spec.name)
        }

        return combine
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
                "time_expired " +
            "FROM " +
                "cluster_lock " +
            "WHERE " +
                "time_expired < ? ORDER BY str_name ASC"
    }
}