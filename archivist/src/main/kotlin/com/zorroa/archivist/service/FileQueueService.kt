package com.zorroa.archivist.service

import com.zorroa.archivist.config.ApplicationProperties
import com.zorroa.archivist.domain.*
import com.zorroa.archivist.repository.FileQueueDao
import com.zorroa.archivist.sdk.security.UserRegistryService
import com.zorroa.archivist.security.InternalAuthentication
import com.zorroa.archivist.security.resetAuthentication
import com.zorroa.common.domain.JobSpec
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.timerTask

/**
 * The FileQueueService handles accepting file processing requests via various
 * pub/sub entry points.
 */
interface FileQueueService {
    fun create(spec: QueuedFileSpec): QueuedFile
    fun processQueue() : Int
}

// Not transactional
@Service
class FileQueueServiceImpl @Autowired constructor(
        private val fileQueueDao: FileQueueDao,
        private val properties: ApplicationProperties): FileQueueService, ApplicationListener<ContextRefreshedEvent> {

    @Autowired
    lateinit var jobService: JobService

    @Autowired
    lateinit var pipelineService: PipelineService

    @Autowired
    lateinit var userRegistryService: UserRegistryService

    val timer = Timer()

    val isProcessing = AtomicBoolean(false)

    override fun onApplicationEvent(p0: ContextRefreshedEvent) {
        setupTimer()
    }

    override fun create(spec: QueuedFileSpec) : QueuedFile {
        return fileQueueDao.create(spec)
    }

    override fun processQueue() : Int {

        // TODO: get lock on queue.  With multiple archivists we need a cluster wide lock

        val files = fileQueueDao.getAll(1000)
        if (files.isEmpty()) {
            return 0
        }

        // Bail out of another thread is here.
        if (!isProcessing.compareAndSet(false, true)) {
            logger.info("Queued files are already being batched")
            return 0
        }

        var total = 0
        try {
            val batchSize = properties.getInt("archivist.file-queue.batch-size")

            // Jobs have to be grouped by these guys.  The queued file list is already sorted
            var orgId: UUID = files[0].organizationId
            var pipelineId: UUID = files[0].pipelineId

            val batch = mutableListOf<QueuedFile>()
            for (qf in files) {

                // If the org or pipeline changes or we hit the batch size, then launch the batch.
                if ((qf.organizationId != orgId || qf.pipelineId != pipelineId) || batch.size >= batchSize) {

                    // Maybe try block around this.
                    logger.info("Launching batch of {} files org.id='{}'", batch.size, orgId)

                    // Launch the job
                    total+=makeJob(orgId, pipelineId, batch)

                    // delete batch from DB
                    batch.clear()

                    // Reset these values
                    orgId = qf.organizationId
                    pipelineId = qf.pipelineId

                } else {
                    batch.add(qf)
                }
            }

            if (batch.isNotEmpty()) {
                logger.info("Launching final batch of {} files to org.id='{}'", batch.size, orgId)
                total+=makeJob(orgId, pipelineId, batch)

            }

        } catch (e: Exception) {
            logger.error("Failed to launch batch of queued files org.id='{}': ", e)
        }
        finally {
            isProcessing.set(false)
        }

        return total
    }

    private fun makeJob(orgId: UUID, pipelineId: UUID, batch: List<QueuedFile>) : Int {

        // Reset auth to
        val currentAuth = resetAuthentication(InternalAuthentication(
                userRegistryService.getUser(getOrgBatchUserName(orgId))))

        try {
            val docs = batch.map {
                val doc = Document(it.assetId.toString(), it.metadata)
                doc.setAttr("source.path", it.path)
                doc
            }

            val size = batch.size
            val name = "$size queue files $orgId / $pipelineId"
            val script = ZpsScript("generate $name",
                    inline = false,
                    type = PipelineType.Import,
                    generate = null,
                    over = docs,
                    execute = pipelineService.resolve(pipelineId))

            val spec = JobSpec(name, script)
            jobService.create(spec)
            fileQueueDao.delete(batch)
            return batch.size
        } finally {
            resetAuthentication(currentAuth)
        }
    }

    private fun setupTimer() {

        // Disable the timer if there is no pubsub system.
        if (properties.getString("archivist.pubsub.type") in setOf("local", "disabled")) {
            return
        }

        val delay = properties.getInt("archivist.file-queue.startup-delay-seconds") * 1000L
        val interval = properties.getInt("archivist.file-queue.check-interval-seconds") * 1000L

        logger.info("Initializing queued file timer: delay: {}, period {}",
                delay, interval)

        timer.scheduleAtFixedRate(timerTask {
            try {
                processQueue()
            }
            catch (e: Exception) {
                logger.error("Failed to launch batch of queued files: ", e)
            }
        }, delay, interval)
    }


    companion object {

        private val logger = LoggerFactory.getLogger(FileQueueServiceImpl::class.java)
    }
}