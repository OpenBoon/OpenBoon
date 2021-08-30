package boonai.archivist.service

import boonai.archivist.clients.AnalystClient
import boonai.archivist.domain.Analyst
import boonai.archivist.domain.AnalystFilter
import boonai.archivist.domain.AnalystSpec
import boonai.archivist.domain.AnalystState
import boonai.archivist.domain.LockState
import boonai.archivist.domain.TaskState
import boonai.archivist.repository.AnalystDao
import boonai.archivist.repository.KPagedList
import boonai.archivist.repository.TaskDao
import boonai.archivist.security.getAnalyst
import boonai.common.service.logging.LogAction
import boonai.common.service.logging.LogObject
import boonai.common.service.logging.event
import boonai.common.service.logging.warnEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.util.UUID

interface AnalystService {
    fun upsert(spec: AnalystSpec): Analyst
    fun exists(endpoint: String): Boolean
    fun getAll(filter: AnalystFilter): KPagedList<Analyst>
    fun get(id: UUID): Analyst
    fun setLockState(analyst: Analyst, state: LockState): Boolean
    fun isLocked(endpoint: String): Boolean
    fun getUnresponsive(state: AnalystState, duration: Duration): List<Analyst>
    fun delete(analyst: Analyst): Boolean
    fun setState(analyst: Analyst, state: AnalystState): Boolean
    fun getClient(endpoint: String): AnalystClient
    fun killTask(endpoint: String, taskId: UUID, reason: String, newState: TaskState): Boolean
    fun setTaskId(analyst: Analyst, taskId: UUID?): Boolean
    fun findOne(filter: AnalystFilter): Analyst
}

@Service
@Transactional
class AnalystServicImpl @Autowired constructor(
    val analystDao: AnalystDao,
    val taskDao: TaskDao
) : AnalystService {

    override fun upsert(spec: AnalystSpec): Analyst {
        val analyst = getAnalyst()
        if (spec.taskId != null) {
            taskDao.updatePingTime(spec.taskId, analyst.endpoint)
        }

        return if (analystDao.update(spec)) {
            analystDao.get(analyst.endpoint)
        } else {
            val analyst = analystDao.create(spec)
            logger.info("Created analyst: {}", analyst.endpoint)
            analyst
        }
    }

    override fun exists(endpoint: String): Boolean {
        return analystDao.exists(endpoint)
    }

    override fun findOne(filter: AnalystFilter): Analyst {
        return analystDao.findOne(filter)
    }

    override fun get(id: UUID): Analyst {
        return analystDao.get(id)
    }

    @Transactional(readOnly = true)
    override fun getAll(filter: AnalystFilter): KPagedList<Analyst> {
        return analystDao.getAll(filter)
    }

    override fun setLockState(analyst: Analyst, state: LockState): Boolean {

        logger.event(
            LogObject.ANALYST, LogAction.UPDATE,
            mapOf(
                "analystId" to analyst.id,
                "analystLock" to analyst.lock
            )
        )
        return analystDao.setLockState(analyst, state)
    }

    @Transactional(readOnly = true)
    override fun isLocked(endpoint: String): Boolean {
        return analystDao.isInLockState(endpoint, LockState.Locked)
    }

    override fun setTaskId(analyst: Analyst, taskId: UUID?): Boolean {

        logger.event(
            LogObject.ANALYST, LogAction.UPDATE,
            mapOf(
                "analystId" to analyst.id,
                "analystTaskId" to analyst.taskId
            )
        )

        return analystDao.setTaskId(analyst.endpoint, taskId)
    }

    override fun setState(analyst: Analyst, state: AnalystState): Boolean {

        logger.event(
            LogObject.ANALYST, LogAction.UPDATE,
            mapOf(
                "analystId" to analyst.id,
                "analystState" to analyst.state
            )
        )

        return analystDao.setState(analyst, state)
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    override fun killTask(endpoint: String, taskId: UUID, reason: String, newState: TaskState): Boolean {
        return try {
            val client = AnalystClient(endpoint)
            val result = client.killTask(taskId, reason, newState)
            return if (result["status"] as Boolean) {
                logger.event(LogObject.TASK, LogAction.KILL, mapOf("reason" to reason, "taskId" to taskId))
                true
            } else {
                logger.warnEvent(
                    LogObject.TASK, LogAction.KILL, "Failed to kill task",
                    mapOf("taskId" to taskId, "analyst" to endpoint)
                )
                false
            }
        } catch (e: Exception) {
            logger.warnEvent(
                LogObject.TASK, LogAction.KILL, "Failed to kill task",
                mapOf("taskId" to taskId, "analyst" to endpoint), e
            )
            false
        }
    }

    @Transactional(readOnly = true)
    override fun getUnresponsive(state: AnalystState, duration: Duration): List<Analyst> {
        return analystDao.getUnresponsive(state, duration)
    }

    override fun delete(analyst: Analyst): Boolean {

        logger.event(
            LogObject.ANALYST, LogAction.DELETE,
            mapOf(
                "analystId" to analyst.id
            )
        )

        return analystDao.delete(analyst)
    }

    override fun getClient(endpoint: String): AnalystClient {
        return AnalystClient(endpoint)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AnalystServicImpl::class.java)
    }
}
