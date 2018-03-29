package com.zorroa.archivist.service

import com.google.common.collect.Lists
import com.zorroa.archivist.domain.*
import com.zorroa.archivist.security.getUsername
import com.zorroa.sdk.client.exception.ArchivistWriteException
import com.zorroa.sdk.domain.PagedList
import com.zorroa.sdk.domain.Pager
import com.zorroa.sdk.processor.PipelineType
import com.zorroa.sdk.processor.ProcessorRef
import com.zorroa.sdk.processor.SharedData
import com.zorroa.sdk.zps.ZpsScript
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

/**
 * Created by chambers on 7/11/16.
 */
interface ImportService {

    fun getAll(page: Pager): PagedList<Job>

    fun create(spec: UploadImportSpec, files: Array<MultipartFile>): Job

    /**
     * Create a import job with the given import spec.
     *
     * @param spec
     * @return
     */
    fun create(spec: ImportSpec): Job

    fun createFileUploadPath(job: Job): Path
}

@Service
@Transactional
class ImportServiceImpl @Autowired constructor(
        private val transactionEventManager: TransactionEventManager,
        private val sharedData: SharedData
        ) : ImportService {

    @Autowired
    private lateinit var jobService: JobService

    @Autowired
    private lateinit var pipelineService: PipelineService

    @Autowired
    private lateinit var pluginService: PluginService

    @Autowired
    private lateinit var logService: EventLogService

    @Value("\${archivist.import.priority}")
    internal var taskPriority: Int = 0

    override fun getAll(page: Pager): PagedList<Job> {
        return jobService.getAll(page, JobFilter().setType(PipelineType.Import))
    }

    override fun create(spec: UploadImportSpec, files: Array<MultipartFile>): Job {

        val jspec = JobSpec()
        jspec.type = PipelineType.Import
        jspec.name = determineJobName(spec.name)

        val job = jobService.launch(jspec)

        // Setup generator
        val generators = Lists.newArrayList<ProcessorRef>()
        try {
            val importPath = createFileUploadPath(job)
            copyUploadedFiles(importPath, files)
            generators.add(ProcessorRef()
                    .setClassName("com.zorroa.core.generator.FileSystemGenerator")
                    .setLanguage("java")
                    .setArg("path", importPath.toString()))

        } catch (e: IOException) {
            logger.warn("Failed to copy uploaded files:", e)
            throw ArchivistWriteException("Failed to copy uploaded files, unexpected :" + e.message)
        }

        // Setup execute
        val execute = mutableListOf<ProcessorRef>()

        /*
         * The first node is an expand collector which allows us to execute in parallel.
         */
        execute.add(pluginService.getProcessorRef("com.zorroa.core.collector.ExpandCollector"))


        val pipeline = Lists.newArrayList<ProcessorRef>()
        pipeline.addAll(pipelineService.mungePipelines(PipelineType.Import, spec.getProcessors()))

        /*
         * Append the index document collector to add stuff to the DB.
         */
        pipeline.add(pluginService.getProcessorRef("com.zorroa.core.collector.IndexCollector",
                mapOf("importId" to job.jobId)))

        /*
         * Set the pipeline as the sub execute to the expand node.
         */
        execute[0].execute = pipeline

        /*
         * Now build the script.
         */
        val script = ZpsScript()
        script.generate = generators
        script.execute = execute

        jobService.createTask(TaskSpec().setScript(script)
                .setJobId(job.jobId)
                .setOrder(taskPriority)
                .setName("Generation via " + generators[0].className))

        transactionEventManager.afterCommit(true,
                { logService.logAsync(UserLogSpec.build(LogAction.Create, "import", job.jobId)) })

        return job
    }

    override fun create(spec: ImportSpec): Job {
        val jspec = JobSpec()
        jspec.type = PipelineType.Import
        jspec.name = determineJobName(spec.name)

        /**
         * Create the job.
         */
        val job = jobService.launch(jspec)


        val expand = pluginService.getProcessorRef("com.zorroa.core.collector.ExpandCollector",
                mapOf("batchSize" to spec.batchSize))

        val execute = Lists.newArrayList(expand)
        expand.execute = pipelineService.mungePipelines(
                PipelineType.Import, spec.processors)

        /**
         * At the end we add an IndexCollector to index the results of our job.
         */
        expand.addToExecute(pluginService.getProcessorRef("com.zorroa.core.collector.IndexCollector",
                mapOf("importId" to job.jobId)))

        /**
         * Now attach the pipeline to each generator, be sure to validate each processor
         * since they are coming from the user.
         */
        val generators = Lists.newArrayList<ProcessorRef>()
        if (spec.generators != null) {
            for (m in spec.generators) {
                val gen = pluginService.getProcessorRef(m)
                generators.add(gen)
            }
        }

        /**
         * The execute property holds the current processors to be executed.
         */
        val script = ZpsScript()
        script.generate = generators
        script.execute = execute

        jobService.createTask(TaskSpec().setScript(script)
                .setJobId(job.jobId)
                .setOrder(taskPriority)
                .setName("Frame Generator"))

        transactionEventManager.afterCommit(true,
                { logService.logAsync(UserLogSpec.build(LogAction.Create, "import", job.jobId)) })

        return job
    }

    override fun createFileUploadPath(job: Job): Path {
        val basePath = sharedData.resolve("uploads")
        val time = DateTime()
        val formatter = DateTimeFormat.forPattern("YYYY/MM")
        val importPath = basePath
                .resolve(formatter.print(time))
                .resolve(getUsername())
                .resolve(job.id.toString())
                .toAbsolutePath()
        importPath.toFile().mkdirs()
        return importPath
    }

    @Throws(IOException::class)
    private fun copyUploadedFiles(path: Path, files: Array<MultipartFile>) {
        for (file in files) {
            Files.copy(file.inputStream, path.resolve(file.originalFilename))
        }
    }

    private fun determineJobName(name: String?): String {
        return name ?: String.format("import by %s", getUsername())
    }

    companion object {

        private val logger = LoggerFactory.getLogger(ImportServiceImpl::class.java)
    }
}
