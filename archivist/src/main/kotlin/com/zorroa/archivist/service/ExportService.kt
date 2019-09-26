package com.zorroa.archivist.service

import com.zorroa.archivist.config.ApplicationProperties
import com.zorroa.archivist.domain.ExportFile
import com.zorroa.archivist.domain.ExportFileSpec
import com.zorroa.archivist.domain.ExportSpec
import com.zorroa.archivist.domain.Pager
import com.zorroa.archivist.domain.PipelineType
import com.zorroa.archivist.domain.ProcessorRef
import com.zorroa.archivist.domain.ZpsScript
import com.zorroa.archivist.repository.ExportFileDao
import com.zorroa.archivist.security.getUser
import com.zorroa.common.domain.ArchivistWriteException
import com.zorroa.common.domain.Job
import com.zorroa.common.domain.JobFilter
import com.zorroa.common.domain.JobId
import com.zorroa.common.domain.JobSpec
import com.zorroa.common.repository.KPagedList
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

interface ExportService {

    fun getAll(page: Pager): KPagedList<Job>
    fun create(spec: ExportSpec, resolve: Boolean = true): Job
    fun createExportFile(job: JobId, spec: ExportFileSpec): ExportFile
    fun getAllExportFiles(job: JobId): List<ExportFile>
    fun getExportFile(id: UUID): ExportFile
}

@Service
@Transactional
class ExportServiceImpl @Autowired constructor(
    private val properties: ApplicationProperties,
    private val exportFileDao: ExportFileDao,
    private val fileStorageService: FileStorageService,
    private val jobService: JobService
) : ExportService {

    @Autowired
    lateinit var searchService: SearchService

    override fun createExportFile(job: JobId, spec: ExportFileSpec): ExportFile {
        val st = fileStorageService.get(spec.storageId).getServableFile()
        if (!st.exists()) {
            throw ArchivistWriteException("export file '${spec.storageId} does not exist")
        }
        return exportFileDao.create(job, st, spec)
    }

    override fun getAllExportFiles(job: JobId): List<ExportFile> {
        return exportFileDao.getAll(job)
    }

    override fun getExportFile(id: UUID): ExportFile {
        return exportFileDao.get(id)
    }

    override fun getAll(page: Pager): KPagedList<Job> {
        val filter = JobFilter(type = PipelineType.Export)
        return jobService.getAll(filter)
    }

    override fun create(spec: ExportSpec, resolve: Boolean): Job {
        val user = getUser()
        spec.name = spec.name ?: "Export By ${user.username}"

        val env = mutableMapOf<String, String>()
        env.putAll(spec.env)

        val jspec = JobSpec(spec.name!!, script = buildZpsSciript(spec), env = env)
        return jobService.create(jspec, PipelineType.Export)
    }

    private fun buildZpsSciript(spec: ExportSpec): ZpsScript {
        /**
         * Now start to build the script for the task.
         */
        val maxAssets = properties.getInt("archivist.export.maxAssetCount").toLong()
        val generate = mutableListOf<ProcessorRef>()

        generate.add(
            ProcessorRef(
                "zplugins.core.generators.AssetSearchGenerator", mapOf(
                    "search" to spec.search,
                    "max_assets" to maxAssets,
                    "page_size" to 50,
                    "scroll" to false
                )
            )
        )

        return ZpsScript(spec.name,
                type = PipelineType.Export,
                generate = generate,
                execute = spec.processors,
                over = null)
    }
}
