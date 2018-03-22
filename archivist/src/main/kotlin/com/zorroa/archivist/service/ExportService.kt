package com.zorroa.archivist.service

import com.google.common.collect.Lists
import com.zorroa.archivist.domain.*
import com.zorroa.archivist.repository.AssetDao
import com.zorroa.archivist.repository.ExportDao
import com.zorroa.archivist.repository.JobDao
import com.zorroa.archivist.sdk.config.ApplicationProperties
import com.zorroa.archivist.security.getUsername
import com.zorroa.sdk.client.exception.ArchivistWriteException
import com.zorroa.sdk.domain.PagedList
import com.zorroa.sdk.domain.Pager
import com.zorroa.sdk.processor.ExportArgs
import com.zorroa.sdk.processor.PipelineType
import com.zorroa.sdk.processor.ProcessorRef
import com.zorroa.sdk.search.AssetFilter
import com.zorroa.sdk.search.AssetSearch
import com.zorroa.sdk.zps.ZpsScript
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

/**
 * Created by chambers on 11/1/15.
 */
interface ExportService {

    fun createExportFile(job: Job, spec: ExportFileSpec): ExportFile

    fun getExportFile(fileId: UUID): ExportFile

    fun getAllExportFiles(job: Job): List<ExportFile>

    fun create(spec: ExportSpec): Job

    fun getAll(page: Pager): PagedList<Job>
}

@Service
@Transactional
class ExportServiceImpl @Autowired constructor(
        val jobService: JobService,
        val jobDao: JobDao,
        val exportDao: ExportDao,
        val pipelineService: PipelineService,
        val pluginService: PluginService,
        val searchService: SearchService,
        val assetDao: AssetDao,
        val properties: ApplicationProperties,
        val transactionEventManager: TransactionEventManager,
        val logService: EventLogService
) : ExportService {

    @Value("\${archivist.export.priority}")
    internal var taskPriority: Int = 0

    /**
     * A temporary place to stuff parameters detected when the search
     * is generated.
     */
    private inner class ExportParams(var search: AssetSearch) {

        internal var pages = false

        internal var frames = false
    }

    /*
     * Perform the export search to tag all the assets being exported.  We then
     * replace the the search being executed with a search for the assets
     * we specifically tagged.
     */
    private fun resolveExportSearch(search: AssetSearch, exportId: UUID): ExportParams {
        search.fields = arrayOf("source")

        val params = ExportParams(AssetSearch())
        val ids = Lists.newArrayListWithCapacity<String>(64)
        for (asset in searchService.scanAndScroll(search,
                properties.getInt("archivist.export.maxAssetCount"))) {
            ids.add(asset.id)

            // Temp stuff
            if (asset.attrExists("source.clip.page")) {
                params.pages = true
            }
            if (asset.attrExists("source.clip.frame")) {
                params.frames = true
            }
        }

        if (ids.isEmpty()) {
            throw ArchivistWriteException("Unable to start export, search returns no assets")
        }

        assetDao.appendLink("export", exportId.toString(), ids)
        params.search = AssetSearch().setFilter(
                AssetFilter().addToLinks("export", exportId.toString()))
        return params
    }

    override fun createExportFile(job: Job, spec: ExportFileSpec): ExportFile {
        return exportDao.createExportFile(job, spec)
    }

    override fun getExportFile(fileId: UUID): ExportFile {
        return exportDao.getExportFile(fileId)
    }

    override fun getAllExportFiles(job: Job): List<ExportFile> {
        return exportDao.getAllExportFiles(job)
    }

    override fun create(spec: ExportSpec): Job {

        val jspec = JobSpec()
        jspec.type = PipelineType.Export

        if (spec.name == null) {
            jspec.name = String.format("export_by_%s", getUsername())
        } else {
            jspec.name = spec.name
        }

        jobDao.nextId(jspec)
        val jobRoot = jobService.resolveJobRoot(jspec)

        val job = jobService.launch(jspec)

        /**
         * Now start to build the script for the task.
         */
        val script = ZpsScript()
        script.putToGlobals("exportArgs", ExportArgs()
                .setExportId(jspec.id)
                .setExportName(jspec.name)
                .setExportRoot(jobRoot.resolve("exported").toString()))

        /**
         * This entire job runs in a single frame.  If this is eventually set False
         * to do parallel processing, the whole pipeline has to be reworked.
         */
        script.isInline = true

        /**
         * Arrays for the primary and per-asset pipeline.
         */
        val generate = Lists.newArrayList<ProcessorRef>()
        val execute = pipelineService.mungePipelines(PipelineType.Export, spec.processors)

        if (spec.compress) {
            execute.add(pluginService.getProcessorRef("com.zorroa.core.exporter.ZipExportPackager",
                    mapOf<String,Any>("fileName" to jspec.name)))
        }

        script.generate = generate
        script.execute = execute

        /**
         * Replace the search the user supplied with our own search so we ensure
         * we get the exact assets during the export and new data
         * added that might match their search change the export.
         */
        val params = resolveExportSearch(spec.search, job.jobId)
        generate.add(pluginService.getProcessorRef(
                "com.zorroa.core.generator.AssetSearchGenerator",
                mapOf<String, Any>("search" to params.search)))

        /**
         * Add the collector which registers ouputs with the server.
         */
        execute.add(pluginService.getProcessorRef("com.zorroa.core.collector.ExportCollector"))

        jobService.createTask(TaskSpec()
                .setJobId(job.jobId)
                .setName("Setup and Generation")
                .setOrder(taskPriority)
                .setScript(script))

        /**
         * Log the create export with the search for the given assets.  When someone
         * downloads the export, that actually logs it as an exported asset.
         */
        transactionEventManager.afterCommit(true, {
            logService.logAsync(UserLogSpec.build(LogAction.Create,
                    "export", job.jobId))
        })

        return job
    }

    override fun getAll(page: Pager): PagedList<Job> {
        return jobService.getAll(page, JobFilter().setType(PipelineType.Export))
    }

    companion object {

        private val logger = LoggerFactory.getLogger(ExportServiceImpl::class.java)
    }
}
