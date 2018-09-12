package com.zorroa.archivist.service

import com.zorroa.archivist.config.ApplicationProperties
import com.zorroa.archivist.domain.*
import com.zorroa.archivist.repository.ExportFileDao
import com.zorroa.archivist.repository.IndexDao
import com.zorroa.archivist.search.AssetFilter
import com.zorroa.archivist.search.AssetSearch
import com.zorroa.archivist.security.getUser
import com.zorroa.common.domain.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

interface ExportService {

    fun getAll(page: Pager): PagedList<Job>
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
        private val txm : TransactionEventManager,
        private val jobService: JobService,
        private val storageRouter: StorageRouter
        ) : ExportService {

    @Autowired
    lateinit var searchService : SearchService

    override fun createExportFile(job: JobId, spec: ExportFileSpec) : ExportFile {
        return exportFileDao.create(job, spec)
    }

    override fun getAllExportFiles(job: JobId) : List<ExportFile> {
        return exportFileDao.getAll(job)
    }

    override fun getExportFile(id: UUID) : ExportFile {
        return exportFileDao.get(id)
    }

    override fun getAll(page: Pager): PagedList<Job> {
        val filter = JobFilter(type=PipelineType.Export)
        return jobService.getAll(page, filter)
    }

    private inner class ExportParams(var search: AssetSearch)

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

        val jspec = JobSpec(spec.name!!,
                PipelineType.Export,
                listOf(),
                env=env)
        val job =  jobService.create(jspec)
        jobService.createTask(job, TaskSpec("export file generator", buildZpsSciript(job, spec, resolve)))
        return job
    }

    private fun buildZpsSciript(job: Job, spec: ExportSpec, resolve:Boolean) : ZpsScript {

        /**
         * Now start to build the script for the task.
         */
        val script = ZpsScript(spec.name!!, inline=true)
        script.globals?.putAll(mapOf(
                "exportArgs" to mapOf(
                        "exportId" to job.id,
                        "exportName" to job.name,
                        "exportRoot" to properties.getString("archivist.export.export-root"))))

        //TODO: This should be coming from the default pipeline. Need to sort this out.
        script.execute?.add(ProcessorRef("zplugins.irm.processors.CDVAssetProcessor"))

        script.execute?.addAll(spec.processors)
        script.execute?.add(ProcessorRef("zplugins.export.processors.GcsExportUploader",
                mapOf<String, Any>("gcs-bucket" to properties.getString("archivist.export.gcs-bucket"))))
        script.execute?.add(ProcessorRef("zplugins.export.processors.ExportedFileRegister"))

        /**
         * Replace the search the user supplied with our own search so we ensure
         * we get the exact assets during the export and new data
         * added that might match their search change the export.
         */
        if (resolve) {
            val params = resolveExportSearch(spec.search, job.id)
            script.generate?.add(ProcessorRef(
                    "zplugins.asset.generators.AssetSearchGenerator",
                    mapOf<String, Any>(
                            "search" to params.search
                    )
            ))
        }
        else {
            script.generate?.add(ProcessorRef(
                    "zplugins.asset.generators.AssetSearchGenerator",
                    mapOf<String, Any>(
                            "search" to spec.search
                    )
            ))
        }

        return script
    }
}

