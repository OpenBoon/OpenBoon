package boonai.archivist.service

import boonai.archivist.domain.AsyncProcess
import boonai.archivist.domain.AsyncProcessSpec
import boonai.archivist.domain.AsyncProcessState
import boonai.archivist.domain.AsyncProcessType
import boonai.archivist.repository.AsyncProcessDao
import boonai.archivist.repository.AsyncProcessJdbcDao
import boonai.archivist.repository.UUIDGen
import boonai.archivist.security.InternalThreadAuthentication
import boonai.archivist.security.getZmlpActor
import boonai.archivist.security.withAuth
import boonai.common.apikey.AuthServerClient
import boonai.common.service.logging.LogAction
import boonai.common.service.logging.LogObject
import boonai.common.service.logging.event
import com.google.common.util.concurrent.AbstractScheduledService
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Timer
import java.util.TimerTask
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * A service for executing async processes within archivist.
 */
interface AsyncProcessService {
    fun createAsyncProcess(spec: AsyncProcessSpec): AsyncProcess
    fun createAsyncProcesses(vararg spec: AsyncProcessSpec)
}

@Configuration
@ConfigurationProperties("archivist.async-process")
class AsyncProcessConfiguration {

    /**
     * Enable async processing.
     */
    var enabled: Boolean = true
}

@Service
@Transactional
class AsyncProcessServiceImpl(val asyncProcessDao: AsyncProcessDao) : AsyncProcessService {

    override fun createAsyncProcess(spec: AsyncProcessSpec): AsyncProcess {
        val time = System.currentTimeMillis()
        val actor = getZmlpActor().toString()
        val id = UUIDGen.uuid1.generate()

        val proc = AsyncProcess(
            id,
            spec.projectId,
            spec.description,
            spec.type,
            AsyncProcessState.PENDING,
            time, -1, -1, -1, actor
        )

        asyncProcessDao.save(proc)

        logger.event(
            LogObject.PROCESS, LogAction.CREATE,
            mapOf(
                "asyncProcessId" to id,
                "asyncProcessDesc" to spec.description
            )
        )

        return proc
    }

    override fun createAsyncProcesses(vararg spec: AsyncProcessSpec) {
        spec.forEach {
            createAsyncProcess(it)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AsyncProcessService::class.java)
    }
}

@Service
class AsyncProcessHandler(
    val asyncProcessDao: AsyncProcessDao,
    val asyncProcessJdbcDao: AsyncProcessJdbcDao,
    val indexRoutingService: IndexRoutingService,
    val projectService: ProjectService,
    val authServerClient: AuthServerClient,
    val config: AsyncProcessConfiguration
) : AbstractScheduledService(), ApplicationListener<ContextRefreshedEvent> {

    val running: MutableList<UUID> = mutableListOf()

    override fun onApplicationEvent(p0: ContextRefreshedEvent?) {
        if (config.enabled) {
            logger.info("Starting AsyncProcess handler")
            this.startAsync()

            logger.info("Starting AsyncProcess watchdog")
            Timer().scheduleAtFixedRate(
                object : TimerTask() {
                    override fun run() {
                        runWatchDog()
                    }
                },
                30000, 30000
            )
        }
    }

    fun handleNextAsyncProcess() {

        val proc = asyncProcessDao.findTopByStateOrderByTimeCreatedAsc(
            AsyncProcessState.PENDING
        ) ?: return

        if (!asyncProcessJdbcDao.setState(proc, AsyncProcessState.RUNNING, AsyncProcessState.PENDING)) {
            logger.warn("AsyncProcess ${proc.id} was already running")
            return
        }

        logger.event(
            LogObject.PROCESS, LogAction.START,
            mapOf(
                "asyncProcessId" to proc.id,
                "asyncProcessDesc" to proc.description,
                "projectId" to proc.projectId
            )
        )

        // Only this thread can touch the running list.
        running.add(proc.id)

        try {
            withAuth(InternalThreadAuthentication(proc.projectId, setOf())) {
                when (proc.type) {
                    AsyncProcessType.DELETE_PROJECT_STORAGE -> {
                        projectService.delete(proc.projectId)
                    }

                    AsyncProcessType.DELETE_PROJECT_INDEXES -> {
                        indexRoutingService.closeAndDeleteProjectIndexes(proc.projectId)
                    }

                    AsyncProcessType.DELETE_SYSTEM_STORAGE -> {
                        projectService.deleteProjectSystemStorage(proc.projectId)
                    }

                    AsyncProcessType.DELETE_API_KEY -> {
                        authServerClient.deleteProjectApiKeys(proc.projectId)
                    }
                }
            }

            asyncProcessJdbcDao.setState(proc, AsyncProcessState.SUCCESS, AsyncProcessState.RUNNING)
        } catch (e: Exception) {
            asyncProcessJdbcDao.setState(proc, AsyncProcessState.ERROR, AsyncProcessState.RUNNING)
            logger.warn("Failed to execute process: ${proc.id} / ${proc.description}", e)
        } finally {
            logger.event(
                LogObject.PROCESS, LogAction.STOP,
                mapOf(
                    "asyncProcessId" to proc.id,
                    "asyncProcessDesc" to proc.description,
                    "projectId" to proc.projectId
                )
            )

            running.remove(proc.id)
        }
    }

    fun runWatchDog() {
        for (id in running) {
            if (asyncProcessDao.updateRefreshTime(System.currentTimeMillis(), id) != 1) {
                logger.warn("The AsyncProc $id was in the running list but not in the DB")
            }
        }

        // go back 5 min from now.
        val expireTime = System.currentTimeMillis() - (60000 * 5)
        for (proc in asyncProcessDao.findByStateAndTimeRefreshLessThan(AsyncProcessState.RUNNING, expireTime)) {
            logger.warn("Setting AsyncProc ${proc.id} / ${proc.description} back to pending")
            asyncProcessJdbcDao.setState(proc, AsyncProcessState.PENDING)
        }
    }

    override fun runOneIteration() {
        handleNextAsyncProcess()
    }

    override fun scheduler(): Scheduler {
        return Scheduler.newFixedDelaySchedule(Random.nextLong(30, 60), 30, TimeUnit.SECONDS)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AsyncProcessHandler::class.java)
    }
}
