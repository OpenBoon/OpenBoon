package com.zorroa.archivist.service

import com.zorroa.archivist.config.ApplicationProperties
import com.zorroa.archivist.domain.*
import com.zorroa.archivist.repository.ExportFileDao
import com.zorroa.archivist.repository.IndexDao
import com.zorroa.archivist.search.AssetFilter
import com.zorroa.archivist.search.AssetSearch
import com.zorroa.archivist.security.getUser
import com.zorroa.common.domain.*
import com.zorroa.common.repository.KPagedList
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.lang.IllegalArgumentException
import java.util.*
import kotlin.math.max

interface ExportService {

    fun getAll(page: Pager): KPagedList<Job>
    fun create(spec: ExportSpec, resolve:Boolean=true) : Job
    fun createExportFile(job: JobId, spec: ExportFileSpec) : ExportFile
    fun getAllExportFiles(job: JobId) :  List<ExportFile>
    fun getExportFile(id: UUID) : ExportFile
}

@Service
@Transactional
class ExportServiceImpl @Autowired constructor(
        private val properties: ApplicationProperties,
        private val indexDao: IndexDao,
        private val exportFileDao: ExportFileDao,
        private val fileStorageService: FileStorageService,
        private val jobService: JobService,
        private val pipelineService: PipelineService
        ) : ExportService {

    @Autowired
    lateinit var searchService : SearchService

    override fun createExportFile(job: JobId, spec: ExportFileSpec) : ExportFile {
        val st = fileStorageService.get(spec.storageId).getServableFile()
        if (!st.exists()) {
            throw ArchivistWriteException("export file '${spec.storageId} does not exist")
        }
        return exportFileDao.create(job, st, spec)
    }

    override fun getAllExportFiles(job: JobId) : List<ExportFile> {
        return exportFileDao.getAll(job)
    }

    override fun getExportFile(id: UUID) : ExportFile {
        return exportFileDao.get(id)
    }

    override fun getAll(page: Pager): KPagedList<Job> {
        val filter = JobFilter(type=PipelineType.Export)
        return jobService.getAll(filter)
    }

    private inner class ExportParams(var search: AssetSearch)

    /**
     * TODO: Rethink this method, might not be needed. Audit-log is probably better.
     */
    private fun resolveExportSearch(search: AssetSearch, exportId: UUID): ExportParams {
        search.fields = arrayOf("source")

        val params = ExportParams(AssetSearch())
        val ids = mutableListOf<String>()
        val maxAssets = properties.getInt("archivist.export.maxAssetCount").toLong()
        for (asset in searchService.scanAndScroll(search, maxAssets, clamp=true)) {
            ids.add(asset.id)
        }

        if (ids.isEmpty()) {
            throw ArchivistWriteException("Unable to start export, search returns no assets")
        }

        indexDao.appendLink("export", exportId.toString(), ids)
        params.search = AssetSearch().setFilter(
                AssetFilter().addToLinks("export", exportId.toString()))
        return params
    }

    override fun create(spec: ExportSpec, resolve:Boolean): Job {
        val user = getUser()
        spec.name = spec.name ?: "Export By ${user.username}"

        val env = mutableMapOf<String, String>()
        env.putAll(spec.env)

        val jspec = JobSpec(spec.name!!, script=buildZpsSciript(spec), env=env)
        return jobService.create(jspec, PipelineType.Export)
    }

    private fun buildZpsSciript(spec: ExportSpec) : ZpsScript {
        /**
         * Now start to build the script for the task.
         */

        val maxAssets = properties.getInt("archivist.export.maxAssetCount").toLong()
        if (searchService.count(spec.search) > maxAssets) {
            throw IllegalArgumentException("Cannot export more than $maxAssets assets at a time")
        }

        val generate =  mutableListOf<ProcessorRef>()
        generate.add(ProcessorRef(
                "zplugins.asset.generators.AssetSearchGenerator",
                mapOf<String, Any>(
                        "search" to spec.search
                )
        ))

        return ZpsScript(spec.name,
                type = PipelineType.Export,
                generate=generate ,
                execute=spec.processors,
                over=null)
    }
}

