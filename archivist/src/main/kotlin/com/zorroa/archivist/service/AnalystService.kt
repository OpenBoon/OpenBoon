package com.zorroa.archivist.service

import com.fasterxml.jackson.core.type.TypeReference
import com.zorroa.archivist.domain.ClusterLockSpec
import com.zorroa.archivist.domain.LogAction
import com.zorroa.archivist.domain.LogObject
import com.zorroa.archivist.domain.ProcessorSpec
import com.zorroa.archivist.repository.AnalystDao
import com.zorroa.archivist.security.getAnalystEndpoint
import com.zorroa.common.clients.RestClient
import com.zorroa.common.domain.*
import com.zorroa.common.repository.KPage
import com.zorroa.common.repository.KPagedList
import com.zorroa.common.util.Json
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

interface AnalystService {
    fun upsert(spec: AnalystSpec) : Analyst
    fun exists(endpoint: String) : Boolean
    fun getAll(filter: AnalystFilter) : KPagedList<Analyst>
    fun get(id: UUID) : Analyst
    fun setLockState(analyst: Analyst, state: LockState) : Boolean
    fun isLocked(endpoint: String) : Boolean
    fun getUnresponsive(state: AnalystState, duration:Duration) : List<Analyst>
    fun delete(analyst: Analyst) : Boolean
    fun setState(analyst: Analyst, state: AnalystState) : Boolean
    fun doProcessorScan() : List<ProcessorSpec>
    fun getClient(endpoint: String) : RestClient
    fun killTask(endpoint: String, taskId: UUID, reason: String, newState: TaskState) : Boolean
    fun setTaskId(analyst: Analyst, taskId: UUID?) : Boolean
}

@Service
@Transactional
class AnalystServicImpl @Autowired constructor(
        val analystDao: AnalystDao,
        val txm: TransactionEventManager,
        val clusterLockExecutor: ClusterLockExecutor): AnalystService {

    @Autowired
    lateinit var processorService: ProcessorService

    /**
     * If the firstPing is true, then do a processor scan once
     * the transaction has committed.
     */
    val firstPing = AtomicBoolean(true)

    override fun upsert(spec: AnalystSpec) : Analyst {
        // First ping of any analyst we'll do a scan.
        if (firstPing.compareAndSet(true, false)) {
            txm.afterCommit(false) {
                doProcessorScan()
            }
        }

        return if (analystDao.update(spec)) {
            val endpoint = getAnalystEndpoint()
            analystDao.get(endpoint)
        }
        else {
            val analyst = analystDao.create(spec)
            logger.info("Created analyst: {}", analyst.endpoint)
            analyst
        }
    }

    override fun exists(endpoint: String) : Boolean {
        return analystDao.exists(endpoint)
    }

    override fun get(id: UUID) : Analyst {
        return analystDao.get(id)
    }

    @Transactional(readOnly = true)
    override fun getAll(filter: AnalystFilter) : KPagedList<Analyst> {
        return analystDao.getAll(filter)
    }

    override fun setLockState(analyst: Analyst, state: LockState) : Boolean {
        return analystDao.setLockState(analyst, state)
    }

    @Transactional(readOnly = true)
    override fun isLocked(endpoint: String) : Boolean {
        return analystDao.isInLockState(endpoint, LockState.Locked)
    }

    override fun setTaskId(analyst: Analyst, taskId: UUID?) : Boolean {
        return analystDao.setTaskId(analyst.endpoint, taskId)
    }

    override fun setState(analyst: Analyst, state: AnalystState) : Boolean {
        return analystDao.setState(analyst, state)
    }

    override fun killTask(endpoint: String, taskId: UUID, reason: String, newState: TaskState) : Boolean {
        return try {
            val client = RestClient(endpoint)
            val result = client.delete("/kill/$taskId",
                    mapOf("reason" to reason, "newState" to newState.name), Json.GENERIC_MAP)

            return if (result["status"] as Boolean) {
               logger.event(LogObject.TASK, LogAction.KILL,
                        mapOf("reason" to reason, "taskId" to taskId))
                true
            } else {
                logger.warnEvent(LogObject.TASK, LogAction.KILL, "Failed to kill task",
                        mapOf("taskId" to taskId, "analyst" to endpoint))
                false
            }

        } catch (e: Exception) {
            logger.warnEvent(LogObject.TASK, LogAction.KILL, "Failed to kill task",
                    mapOf("taskId" to taskId, "analyst" to endpoint), e)
            false
        }
    }

    @Transactional(readOnly = true)
    override fun getUnresponsive(state: AnalystState, duration: Duration): List<Analyst> {
        return analystDao.getUnresponsive(state, duration)
    }

    override fun delete(analyst: Analyst): Boolean {
        return analystDao.delete(analyst)
    }

    override fun doProcessorScan(): List<ProcessorSpec> {
        /**
         * Take a soft lock and hold for the full timeout to stop over-scanning.
         */
        val lock = ClusterLockSpec.softLock("processor-scan").apply {
            holdTillTimeout = true
            timeout = 1
            timeoutUnits = TimeUnit.MINUTES
        }

        val result = clusterLockExecutor.inline(lock) {
            val filter = AnalystFilter(states = listOf(AnalystState.Up))
            filter.apply {
                this.page = KPage(0, 5)
                this.sort = listOf("timePing:d", "load:a")
            }

            val analysts = analystDao.getAll(filter)
            var processors = mutableListOf<ProcessorSpec>()
            for (analyst in analysts) {
                logger.event(LogObject.ANALYST, LogAction.SCAN,
                        mapOf("analystEndpoint" to analyst.endpoint))
                val client = getClient(analyst.endpoint)
                try {
                    val procs = client.get("/processors",
                            object : TypeReference<List<ProcessorSpec>>() {})
                    if (procs.isNotEmpty()) {
                        processorService.replaceAll(procs)
                        processors.addAll(procs)
                        break
                    }
                } catch (e: Exception) {
                    logger.warn("Failed to communicate with Analyst '${analyst.endpoint}", e)
                }
            }
            processors
        }

        return result ?: listOf()
    }

    override fun getClient(endpoint: String): RestClient {
        return RestClient(endpoint)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AnalystServicImpl::class.java)
    }
}