package com.zorroa.archivist.service

import com.zorroa.archivist.domain.DataSource
import com.zorroa.archivist.domain.DataSourceSpec
import com.zorroa.archivist.domain.DataSourceUpdate
import com.zorroa.archivist.domain.Job
import com.zorroa.archivist.domain.JobFilter
import com.zorroa.archivist.domain.JobSpec
import com.zorroa.archivist.domain.JobState
import com.zorroa.archivist.domain.ProcessorRef
import com.zorroa.archivist.domain.StandardContainers
import com.zorroa.archivist.domain.ZpsScript
import com.zorroa.archivist.repository.DataSourceDao
import com.zorroa.archivist.repository.DataSourceJdbcDao
import com.zorroa.archivist.repository.UUIDGen
import com.zorroa.archivist.security.getProjectId
import com.zorroa.archivist.security.getZmlpActor
import com.zorroa.archivist.util.isUUID
import com.zorroa.zmlp.service.logging.LogAction
import com.zorroa.zmlp.service.logging.LogObject
import com.zorroa.zmlp.service.logging.event
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

interface DataSourceService {

    /**
     * Create a new [DataSource]
     */
    fun create(spec: DataSourceSpec): DataSource

    /**
     * Update an existing [DataSource]
     */
    fun update(id: UUID, update: DataSourceUpdate): DataSource

    /**
     * Delete an existing [DataSource]
     */
    fun delete(id: UUID)

    /**
     * Get a [DataSource] by its unique ID.
     */
    fun get(id: UUID): DataSource

    /**
     * Create an Analysis job to process the [DataSource]
     */
    fun createAnalysisJob(dataSource: DataSource): Job

    /**
     * Set available credentials blobs for this job.
     */
    fun setCredentials(id: UUID, names: Set<String>)
}

@Service
@Transactional
class DataSourceServiceImpl(
    val dataSourceDao: DataSourceDao,
    val dataSourceJdbcDao: DataSourceJdbcDao,
    val credentialsService: CredentialsService,
    val txEvent: TransactionEventManager
) : DataSourceService {

    @Autowired
    lateinit var jobService: JobService

    @Autowired
    lateinit var pipelineService: PipelineService

    @Autowired
    lateinit var pipelineModService: PipelineModService

    @Autowired
    lateinit var pipelineResolverService: PipelineResolverService

    override fun create(spec: DataSourceSpec): DataSource {

        val time = System.currentTimeMillis()
        val actor = getZmlpActor()
        val id = UUIDGen.uuid1.generate()

        val mods = pipelineModService.getByNames(spec.modules ?: setOf())

        val result = dataSourceDao.saveAndFlush(
            DataSource(
                id,
                getProjectId(),
                spec.name,
                spec.uri,
                spec.fileTypes,
                listOf(),
                mods.map { it.id },
                time,
                time,
                actor.name,
                actor.name
            )
        )

        val creds = credentialsService.getAll(spec.credentials)
        dataSourceJdbcDao.setCredentials(id, creds)

        logger.event(
            LogObject.DATASOURCE, LogAction.CREATE,
            mapOf(
                "newDataSourceId" to result.id,
                "newDataSourceName" to result.name
            )
        )
        return result
    }

    override fun update(id: UUID, update: DataSourceUpdate): DataSource {
        logger.event(LogObject.DATASOURCE, LogAction.UPDATE, mapOf("dataSourceId" to id))

        val mods = pipelineModService.getByNames(update.modules ?: listOf())
        val ds = get(id)

        val updated = DataSource(ds.id,
            ds.projectId,
            update.name,
            update.uri,
            update.fileTypes,
            listOf(),
            mods.map { it.id },
            ds.timeCreated,
            System.currentTimeMillis(),
            ds.actorCreated,
            getZmlpActor().toString())

        update.credentials?.let {
            setCredentials(id, it)
        }
        return dataSourceDao.saveAndFlush(updated)
    }

    override fun delete(id: UUID) {
        logger.event(LogObject.DATASOURCE, LogAction.DELETE, mapOf("dataSourceId" to id))
        val ds = get(id)

        // Grab all the jobs before the DS is delted.
        val jobs = jobService.getAll(
            JobFilter(
                states = listOf(JobState.InProgress, JobState.Failure),
                datasourceIds = listOf(ds.id)).apply { page.disabled = true }
        )

        logger.info("Canceling ${jobs.size()} DataSource ${ds.id} jobs")

        // If this thing commits we async cancel all the jobs for the
        // DS which involves killins all the running tasks.
        txEvent.afterCommit(sync = false) {
            jobs.forEach { jobService.cancelJob(it) }
        }
        dataSourceDao.delete(ds)
    }

    override fun createAnalysisJob(dataSource: DataSource): Job {
        val name = "Analyze DataSource '${dataSource.name}'"

        // TODO: check the uri type and make correct generator

        val gen = ProcessorRef(
            "zmlp_core.core.generators.GcsBucketGenerator",
            StandardContainers.CORE,
            args = mapOf("uri" to dataSource.uri)
        )

        val mods = pipelineModService.getByIds(dataSource.modules)
        val script = ZpsScript("GcsBucketGenerator ${dataSource.uri}", listOf(gen), null,
            pipelineResolverService.resolveModular(mods))

        script.setSettting("fileTypes", dataSource.fileTypes)
        script.setSettting("batchSize", 20)

        val spec = JobSpec(name, script,
            dataSourceId = dataSource.id,
            credentials = dataSource.credentials.map { it.toString() }.toSet())
        return jobService.create(spec)
    }

    @Transactional(readOnly = true)
    override fun get(id: UUID): DataSource {
        return dataSourceDao.getOneByProjectIdAndId(getProjectId(), id)
    }

    override fun setCredentials(id: UUID, names: Set<String>) {
        val creds = names.map {
            if (it.isUUID()) {
                credentialsService.get(UUID.fromString(it))
            } else {
                credentialsService.get(it)
            }
        }
        dataSourceJdbcDao.setCredentials(id, creds)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DataSourceServiceImpl::class.java)
    }
}
