package com.zorroa.archivist.service

import com.zorroa.archivist.config.ApplicationProperties
import com.zorroa.archivist.domain.*
import com.zorroa.archivist.repository.ExportDao
import com.zorroa.archivist.repository.ExportFileDao
import com.zorroa.archivist.repository.IndexDao
import com.zorroa.archivist.security.getUser
import com.zorroa.common.clients.AnalystClient
import com.zorroa.common.domain.*
import com.zorroa.common.repository.KPage
import com.zorroa.common.repository.KPagedList
import com.zorroa.common.search.AssetFilter
import com.zorroa.common.search.AssetSearch
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.InputStream
import java.util.*

interface ExportService {

    fun getAll(page: KPage, filter: ExportFilter): KPagedList<Export>
    fun get(id: UUID) : Export
    fun create(spec: ExportSpec) : Export
    fun createExportFile(export: Export, spec: ExportFileSpec) : ExportFile
    fun getAllExportFiles(export: Export) :  List<ExportFile>
    fun getExportFile(id: UUID) : ExportFile
    fun getExportFileStream(exportFile: ExportFile) : InputStream

}

@Service
@Transactional
class ExportServiceImpl @Autowired constructor(
        private val properties: ApplicationProperties,
        private val indexDao: IndexDao,
        private val exportDao: ExportDao,
        private val exportFileDao: ExportFileDao,
        private val txm : TransactionEventManager,
        private val analystClient: AnalystClient
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

    override fun getExportFileStream(exportFile: ExportFile) : InputStream {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getAll(page: KPage, filter: ExportFilter): KPagedList<Export> {
        return exportDao.getAll(page, filter)
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
        env.putAll(mapOf("ZORROA_USER_ID" to user.id.toString(),
                "ZORROA_ORG_ID" to user.organizationId.toString(),
                "ZORROA_EXPORT_ID" to export.id.toString()))

        val jspec = JobSpec(spec.name!!,
                PipelineType.EXPORT,
                getUser().organizationId,
                buildZpsSciript(export, spec),
                env=env)

        val job = analystClient.createJob(jspec)
        exportDao.setAnalystJobId(export, job)

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
                        "exportName" to export.name)))

        script.execute?.addAll(spec.processors)

        /**
         * Replace the search the user supplied with our own search so we ensure
         * we get the exact assets during the export and new data
         * added that might match their search change the export.
         */
        val params = resolveExportSearch(spec.search, export.id)
        script.generate?.add(ProcessorRef(
                "com.zorroa.core.generator.AssetSearchGenerator",
                mapOf<String, Any>("search" to params.search)))

        return script
    }
}

