package com.zorroa.archivist.service

import com.zorroa.archivist.config.ApplicationProperties
import com.zorroa.archivist.domain.*
import com.zorroa.archivist.repository.ExportDao
import com.zorroa.archivist.repository.ExportFileDao
import com.zorroa.archivist.repository.IndexDao
import com.zorroa.archivist.search.AssetFilter
import com.zorroa.archivist.search.AssetSearch
import com.zorroa.archivist.security.getOrgId
import com.zorroa.archivist.security.getUser
import com.zorroa.common.domain.*
import com.zorroa.common.repository.KPage
import com.zorroa.common.repository.KPagedList
import com.zorroa.common.util.Json
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

interface ExportService {

    fun getAll(page: KPage, filter: ExportFilter): KPagedList<Export>
    fun get(id: UUID) : Export
    fun create(spec: ExportSpec) : Export
    fun createExportFile(export: Export, spec: ExportFileSpec) : ExportFile
    fun getAllExportFiles(export: Export) :  List<ExportFile>
    fun getExportFile(id: UUID) : ExportFile
    fun setState(id:UUID, state: JobState) : Boolean
}

@Service
@Transactional
class ExportServiceImpl @Autowired constructor(
        private val properties: ApplicationProperties,
        private val indexDao: IndexDao,
        private val exportDao: ExportDao,
        private val exportFileDao: ExportFileDao,
        private val txm : TransactionEventManager,
        private val jobService: JobService,
        private val storageRouter: StorageRouter
        ) : ExportService {

    @Autowired
    lateinit var searchService : SearchService

    override fun get(id: UUID): Export {
       return exportDao.get(id)
    }

    override fun createExportFile(export: Export, spec: ExportFileSpec) : ExportFile {
        return exportFileDao.create(export, spec)
    }

    override fun getAllExportFiles(export: Export) : List<ExportFile> {
        return exportFileDao.getAll(export)
    }

    override fun getExportFile(id: UUID) : ExportFile {
        return exportFileDao.get(id)
    }

    override fun getAll(page: KPage, filter: ExportFilter): KPagedList<Export> {
        return exportDao.getAll(page, filter)
    }

    override fun setState(id:UUID, state: JobState) : Boolean {
        return exportDao.setState(id, state)
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

    override fun create(spec: ExportSpec): Export {
        val user = getUser()
        spec.name = spec.name ?: "Export By ${user.username}"
        val export = exportDao.create(spec)

        val env = mutableMapOf<String, String>()
        env.putAll(spec.env)
        env.putAll(mapOf("ZORROA_EXPORT_ID" to export.id.toString()))

        val jspec = JobSpec(spec.name!!,
                PipelineType.Export,
                listOf(buildZpsSciript(export, spec)),
                env=env)

        val job = jobService.create(jspec)
        return export
    }

    private fun buildZpsSciript(export: Export, spec: ExportSpec) : ZpsScript {

        /**
         * Now start to build the script for the task.
         */
        val script = ZpsScript(export.name, inline=true)
        script.globals?.putAll(mapOf(
                "exportArgs" to mapOf(
                        "exportId" to export.id,
                        "exportName" to export.name,
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
        val params = resolveExportSearch(spec.search, export.id)
        script.generate?.add(ProcessorRef(
                "zplugins.asset.generators.AssetSearchGenerator",
                mapOf<String, Any>(
                        "search" to params.search
                )
        ))

        return script
    }
}

