package com.zorroa.archivist.service

import com.zorroa.archivist.domain.DataSource
import com.zorroa.archivist.domain.DataSourceCredentials
import com.zorroa.archivist.domain.DataSourceSpec
import com.zorroa.archivist.domain.Job
import com.zorroa.archivist.domain.JobSpec
import com.zorroa.archivist.domain.LogAction
import com.zorroa.archivist.domain.LogObject
import com.zorroa.archivist.domain.ProcessorRef
import com.zorroa.archivist.domain.ZpsScript
import com.zorroa.archivist.repository.DataSourceDao
import com.zorroa.archivist.repository.DataSourceJdbcDao
import com.zorroa.archivist.repository.UUIDGen
import com.zorroa.archivist.security.getProjectId
import com.zorroa.archivist.security.getZmlpActor
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.encrypt.Encryptors
import org.springframework.security.crypto.keygen.KeyGenerators
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

interface DataSourceService {

    /**
     * Create a new [DataSource]
     */
    fun create(spec: DataSourceSpec): DataSource

    /**
     * Get a [DataSource] by its unique ID.
     */
    fun get(id: UUID): DataSource

    /**
     * Returns the decrypted dataset credentials.  This method should only be
     * called from the JobRunner Key.
     *
     * @param id The UUID of the dataset.
     * @return The credentials blob.
     */
    fun getCredentials(id: UUID): DataSourceCredentials

    /**
     * Update the credentials blob, can be null.  Return true
     * if the value was updated.
     */
    fun updateCredentials(id: UUID, blob: String?) : Boolean

    /**
     * Create an Analysis job to process the [DataSource]
     */
    fun createAnalysisJob(dataSource: DataSource): Job
}

@Service
@Transactional
class DataSourceServiceImpl(
    val dataSourceDao: DataSourceDao,
    val dataSourceJdbcDao: DataSourceJdbcDao,
    val projectService: ProjectService,
    val jobService: JobService
) : DataSourceService {

    override fun create(spec: DataSourceSpec): DataSource {

        val time = System.currentTimeMillis()
        val actor = getZmlpActor()
        val id = UUIDGen.uuid1.generate()
        val result = dataSourceDao.saveAndFlush(
            DataSource(
                id,
                getProjectId(),
                spec.name,
                spec.uri,
                spec.fileTypes,
                spec.analysis,
                time,
                time,
                actor.name,
                actor.name
            )
        )
        updateCredentials(id, spec.credentials)

        logger.event(
            LogObject.DATASOURCE, LogAction.CREATE,
            mapOf(
                "newDataSourceId" to result.id,
                "newDataSourceName" to result.name
            )
        )
        return result
    }

    override fun createAnalysisJob(dataSource: DataSource): Job {
        val name = "Analyze DataSource '${dataSource.name}'"

        // TODO: check the uri type and make correct generator

        val gen = ProcessorRef(
            "zmlp_core.core.generators.GcsBucketGenerator",
            "zmlp-plugins-core",
            args = mapOf("uri" to dataSource.uri)
        )

        val script = ZpsScript("GcsBucketGenerator ${dataSource.uri}", listOf(gen), null, null)
        script.setSettting("fileTypes", dataSource.fileTypes)
        script.setSettting("batchSize", 10)

        val spec = JobSpec(name, script, dataSourceId = dataSource.id)
        return jobService.create(spec)
    }

    @Transactional(readOnly = true)
    override fun get(id: UUID): DataSource {
        return dataSourceDao.getOne(id)
    }

    @Transactional(readOnly = true)
    override fun getCredentials(id: UUID): DataSourceCredentials {
        val creds = dataSourceJdbcDao.getCredentials(id)
        return DataSourceCredentials(
            blob = Encryptors.text(
                projectService.getCryptoKey(), creds.salt
            ).decrypt(creds.blob)
        )
    }

    override fun updateCredentials(id: UUID, blob: String?) : Boolean {
        val salt = KeyGenerators.string().generateKey()
        return dataSourceJdbcDao.updateCredentials(
            id, encryptCredentials(blob, salt), salt
        )
    }

    fun encryptCredentials(creds: String?, salt: String): String? {
        return if (creds == null) {
            creds
        } else {
            Encryptors.text(projectService.getCryptoKey(), salt).encrypt(creds)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DataSourceServiceImpl::class.java)
    }
}