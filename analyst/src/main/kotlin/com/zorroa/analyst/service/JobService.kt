package com.zorroa.analyst.service

import com.zorroa.analyst.domain.LockSpec
import com.zorroa.analyst.repository.JobDao
import com.zorroa.analyst.repository.LockDao
import com.zorroa.common.domain.*
import com.zorroa.common.repository.KPagedList
import com.zorroa.common.util.Json
import com.zorroa.common.util.getPublicUrl
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

interface JobService {

    /**
     * Create and return the initial job entry in the Waiting state.
     */
    fun create(spec: JobSpec) : Job

    /**
     * Get a job by its unique ID.
     */
    fun get(id: UUID) : Job

    /**
     * Get a job by its unique name.
     */
    fun get(name: String) : Job

    /**
     * Stops a job and sets the final state.
     */
    fun stop(job: Job, finalState: JobState) : Boolean

    /**
     * Sets the job state to Running.  Also locks all assets
     * that the job references, if lockAssets is true.
     */
    fun start(job: Job)

    /**
     * Get the next N waiting jobs.
     */
    fun getWaiting(limit: Int) : List<Job>

    /**
     * Get all the running jobs.
     */
    fun getRunning() : List<Job>

    /**
     * Set a new job state.  Optionally provide a required oldState
     */
    fun setState(job: Job, newState: JobState, oldState: JobState?=null) : Boolean

    /**
     * Get all jobs that match the given filter.
     */
    fun getAll(filter: JobFilter) : KPagedList<Job>

    /**
     * Clear all asset locks on a job.
     */
    fun clearLocks(job: Job)
}

/**
 * A non-transactional service for launching jobs.
 */
interface JobRegistryService {

    /**
     * Launch the given job spec.
     *
     * The job is first launched into the SETUP state to ensure it's not
     * dispatched.  Then a pipeline is built and modified to include
     * any extra processors needed for import/export functionality.
     *
     * Once the zps script is written to job storage, the job is set
     * to the WAITING stat.
     *
     */
    fun launchJob(spec: JobSpec) : Job
}

/**
 * A non-transactional entry point for launching jobs
 */
@Component
class JobRegistryServiceImpl @Autowired constructor(
        private val jobService: JobService,
        private val storageService: JobStorageService,
        private val pipelineService: PipelineService) : JobRegistryService {

    override fun launchJob(spec: JobSpec) : Job {
        val job = jobService.create(spec)
        try {
            buildPipeline(spec, job)

            // The scheduler will sign this when its needed.
            storageService.storeBlob(
                    job.getScriptPath(),
                    "application/json",
                    Json.serialize(spec.script))

            jobService.setState(job, JobState.WAITING, null)
        } catch (e: Exception) {
            jobService.setState(job, JobState.FAIL, null)
        }
        return job
    }

    fun buildPipeline(spec: JobSpec, job: Job) {

        pipelineService.resolveExecute(spec.script)

        when(job.type) {
            PipelineType.IMPORT->handleImportJob(spec, job)
            PipelineType.EXPORT->handleExportJob(spec, job)
            PipelineType.BATCH->handleBatchJob(spec, job)
        }
    }

    fun handleImportJob(spec: JobSpec, job: Job) {
        val selfUrl = getPublicUrl()
        val endpoint = "$selfUrl/api/v1/jobs/${job.id}/_finish"

        spec.script.execute?.add(ProcessorRef("zplugins.metadata.metadata.PostMetadataToRestApi",
                args=mapOf("endpoint" to endpoint, "phase" to "teardown")))
        spec.script.execute?.add(ProcessorRef("zplugins.core.collector.IndexDocumentCollector"))
    }

    fun handleExportJob(spec: JobSpec, job: Job) {
        // TODO: Add processors to register exported files with Archivist
        val selfUrl = getPublicUrl()
        val endpoint = "$selfUrl/api/v1/jobs/${job.id}/_finish"
        spec.script.execute?.add(ProcessorRef("zplugins.metadata.metadata.PostMetadataToRestApi",
                args=mapOf("endpoint" to endpoint, "phase" to "teardown")))
    }

    fun handleBatchJob(spec: JobSpec, job: Job) { }

    companion object {
        private val logger = LoggerFactory.getLogger(JobRegistryServiceImpl::class.java)
    }
}

@Transactional
@Service
class JobServiceImpl @Autowired constructor(
        val jobDao: JobDao,
        val lockDao: LockDao): JobService {

    @Autowired
    lateinit var storageService : JobStorageService

    override fun get(id: UUID) : Job {
        return jobDao.get(id)
    }

    override fun get(name: String) : Job {
        return jobDao.get(name)
    }

    override fun create(spec: JobSpec) : Job {
        val job =  jobDao.create(spec)
        if (job.lockAssets) {
            val assets = mutableListOf<UUID>()
            spec.script.over?.forEach {
                assets.add(UUID.fromString(it.id))
            }
            jobDao.mapAssetsToJob(job, assets)
        }
        return job
    }

    override fun setState(job: Job, newState: JobState, oldState: JobState?) : Boolean {
        val result = jobDao.setState(job, newState, oldState)
        if (result) {
            logger.info("SUCCESS JOB State Change: {} {}->{}",
                    job.name, oldState?.name, newState.name)
        }
        else {
            logger.warn("FAILED JOB State Change: {} {}->{}",
                    job.name, oldState?.name, newState.name)
        }
        return result
    }

    override fun start(job: Job) {
        /**
         * If the job requires we lock the assets, then lock the assets.
         */
        if (job.lockAssets) {
            val script = Json.Mapper.readValue(
                    storageService.getInputStream(job.getScriptPath()), ZpsScript::class.java)
            script?.over?.forEach {
                lockDao.create(LockSpec(UUID.fromString(it.id), job.id))
            }
        }

        // throwing here rolls back any locks
        if (!setState(job, JobState.RUNNING, JobState.WAITING)) {
            throw IllegalStateException("Job ${job.id} was not in the waiting state")
        }
    }

    override fun stop(job: Job, finalState: JobState) : Boolean {
        val result = setState(job, finalState, JobState.RUNNING)
        if (result) {
            lockDao.deleteByJob(job.id)
        }
        return result
    }

    override fun getWaiting(limit: Int) : List<Job> {
        return jobDao.getWaiting(limit)
    }

    override fun getRunning() : List<Job> {
        return jobDao.getRunning()
    }

    override fun getAll(filter: JobFilter) : KPagedList<Job> {
        return jobDao.getAll(filter)
    }

    override fun clearLocks(job: Job) {
        lockDao.deleteByJob(job.id)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(JobServiceImpl::class.java)
    }
}
