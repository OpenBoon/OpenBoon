package com.zorroa.archivist.service

import com.google.common.collect.ImmutableMap
import com.google.common.collect.Lists
import com.zorroa.archivist.domain.*
import com.zorroa.archivist.sdk.config.ApplicationProperties
import com.zorroa.archivist.security.getUsername
import com.zorroa.sdk.client.exception.ArchivistWriteException
import com.zorroa.sdk.domain.PagedList
import com.zorroa.sdk.domain.Pager
import com.zorroa.sdk.processor.PipelineType
import com.zorroa.sdk.processor.ProcessorRef
import com.zorroa.sdk.zps.ZpsScript
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

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
}

@Service
@Transactional
class ImportServiceImpl @Autowired constructor(
        val transactionEventManager: TransactionEventManager,
        val properties: ApplicationProperties
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
            val importPath = copyUploadedFiles(job, files)
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
        execute.add(
                ProcessorRef()
                        .setClassName("com.zorroa.core.collector.ExpandCollector")
                        .setLanguage("java"))


        val pipeline = Lists.newArrayList<ProcessorRef>()
        pipeline.addAll(pipelineService.mungePipelines(PipelineType.Import, spec.getProcessors()))

        /*
         * Append the index document collector to add stuff to the DB.
         */
        pipeline.add(
                ProcessorRef()
                        .setClassName("com.zorroa.core.collector.IndexDocumentCollector")
                        .setLanguage("java")
                        .setArgs(ImmutableMap.of<String, Any>("importId", job.jobId)))

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


        val expand = pluginService.getProcessorRef("com.zorroa.core.collector.ExpandCollector")
        val execute = Lists.newArrayList(expand)
        expand.execute = pipelineService.mungePipelines(
                PipelineType.Import, spec.getProcessors())

        /**
         * At the end we add an IndexDocumentCollector to index the results of our job.
         */
        expand.addToExecute(
                ProcessorRef()
                        .setClassName("com.zorroa.core.collector.IndexDocumentCollector")
                        .setLanguage("java")
                        .setArgs(ImmutableMap.of<String, Any>("importId", job.jobId)))

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

    @Throws(IOException::class)
    private fun copyUploadedFiles(job: Job, files: Array<MultipartFile>): Path {
        val importPath = Paths.get(job.rootPath).resolve("assets")

        for (file in files) {
            if (!importPath.resolve(file.originalFilename).toFile().exists()) {
                Files.copy(file.inputStream, importPath.resolve(file.originalFilename))
            }
        }
        return importPath
    }

    private fun determineJobName(name: String?): String {
        return name ?: String.format("import by %s", getUsername())
    }

    companion object {

        private val logger = LoggerFactory.getLogger(ImportServiceImpl::class.java)
    }
}
