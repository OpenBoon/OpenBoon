package com.zorroa.archivist.repository

import com.zorroa.archivist.domain.LogAction
import com.zorroa.archivist.domain.LogObject
import com.zorroa.archivist.service.event
import com.zorroa.archivist.service.warnEvent
import com.zorroa.common.util.JdbcUtils
import org.springframework.dao.DuplicateKeyException
import org.springframework.jdbc.core.RowCallbackHandler
import org.springframework.stereotype.Repository
import java.net.InetAddress
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit

interface ClusterLockDao {
    fun clearExpired() : Int
    fun lock(name: String, duration: Long, unit: TimeUnit) : Boolean
    fun unlock(name: String) : Boolean
    fun isLocked(name: String): Boolean
}

@Repository
class ClusterLockDaoImpl : AbstractDao(), ClusterLockDao {

    override fun lock(name: String, duration: Long, unit: TimeUnit): Boolean {
        val host = InetAddress.getLocalHost().hostAddress
        val time = System.currentTimeMillis()

        logger.event(LogObject.CLUSTER_LOCK, LogAction.LOCK, mapOf("lockName" to name))

        return try {
            jdbc.update { connection ->
                val ps = connection.prepareStatement(INSERT)
                ps.setString(1, name)
                ps.setString(2, host)
                ps.setLong(3, time)
                ps.setLong(4, time + unit.toMillis(duration))
                ps
            }
            true
        } catch (e: DuplicateKeyException) {
            logger.warnEvent(LogObject.CLUSTER_LOCK, LogAction.LOCK,
                    "Failure obtaining lock", mapOf("lockName" to name))
            false
        }
    }

    override fun unlock(name: String): Boolean {
        logger.event(LogObject.CLUSTER_LOCK, LogAction.UNLOCK, mapOf("lockName" to name))
        val host = InetAddress.getLocalHost().hostAddress
        return jdbc.update("DELETE FROM cluster_lock WHERE str_name=? AND str_host=?",
                name, host) == 1
    }

    override fun isLocked(name: String): Boolean {
        return jdbc.queryForObject("SELECT COUNT(1) FROM cluster_lock WHERE str_name=?", Int::class.java, name) == 1
    }

    override fun clearExpired(): Int {
        val time = System.currentTimeMillis()
        var removed : Int = 0
        jdbc.query("SELECT str_name, time_expired FROM cluster_lock WHERE time_expired < ?", RowCallbackHandler { rs->
            val name = rs.getString("str_name")
            val date = Date.from(Instant.ofEpochMilli(rs.getLong("time_expired")))
            logger.warn("Removing expired lock '$name', expired at {}", date)
            jdbc.update("DELETE FROM cluster_lock WHERE str_name=?", name)
            removed+=1
        }, time)
        return removed
    }

    companion object {

        private val INSERT = JdbcUtils.insert("cluster_lock",
                "str_name",
                "str_host",
                "time_locked",
                "time_expired")

    }
}