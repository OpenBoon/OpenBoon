package com.zorroa.archivist.service

import com.google.common.collect.ImmutableList
import com.google.common.collect.Lists
import com.google.common.collect.Maps
import com.zorroa.archivist.domain.AnalyzeSpec
import com.zorroa.common.cluster.client.ClusterConnectionException
import com.zorroa.common.cluster.client.WorkerNodeClient
import com.zorroa.common.cluster.thrift.TaskErrorT
import com.zorroa.common.cluster.thrift.TaskStartT
import com.zorroa.common.config.NetworkEnvironment
import com.zorroa.sdk.client.exception.ArchivistException
import com.zorroa.sdk.filesystem.ObjectFileSystem
import com.zorroa.sdk.processor.PipelineType
import com.zorroa.sdk.processor.ProcessorRef
import com.zorroa.sdk.processor.SharedData
import com.zorroa.sdk.util.FileUtils
import com.zorroa.sdk.util.Json
import com.zorroa.sdk.zps.ZpsScript
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import java.nio.file.Files

interface AnalyzeService {

    @Throws(IOException::class)
    fun analyze(spec: AnalyzeSpec, file: Array<MultipartFile>?): Any
}

@Component
open class AnalyzeServiceImpl
    @Autowired constructor (
        private val analystService: AnalystService,
        private val sharedData: SharedData,
        private val ofs: ObjectFileSystem,
        private val assetService: AssetService,
        private val pipelineService: PipelineService,
        private val pluginService: PluginService,
        private val network: NetworkEnvironment
    ): AnalyzeService {

    @Throws(IOException::class)
    override fun analyze(spec: AnalyzeSpec, file: Array<MultipartFile>?): Any {

        val script = ZpsScript()
        script.isInline = true
        script.isStrict = true

        val pipeline = Lists.newArrayList<ProcessorRef>()
        pipeline.addAll(pipelineService.mungePipelines(PipelineType.Import, spec.processors))
        script.execute = pipeline

        var lang: String
        try {
            lang = script.execute[0].language
        } catch (ignore: IndexOutOfBoundsException) {
            // an empty pipeline, just ignore it
            lang = "java"
        }

        if (lang == "python") {
            script.execute.add(pluginService.getProcessorRef("zorroa_py_core.document.PyReturnResponse"))
        } else {
            script.execute.add(pluginService.getProcessorRef("com.zorroa.core.processor.ReturnResponse"))
        }

        if (files != null) {
            if (lang == "python") {
                script.generate = ImmutableList.of(ProcessorRef()
                        .setClassName("zorroa_py_core.generators.PyFileGenerator")
                        .setLanguage("python")
                        .setArg("paths", copyUploadedFiles(files)))

            } else {
                script.generate = ImmutableList.of(ProcessorRef()
                        .setClassName("com.zorroa.core.generator.FileListGenerator")
                        .setLanguage("java")
                        .setArg("paths", copyUploadedFiles(files)))
            }
        } else if (spec.asset != null) {
            val asset = assetService.get(spec.asset)
            script.over = ImmutableList.of(asset)
        } else {
            throw ArchivistException("No file or asset specified")
        }

        val analysts = analystService.getActive()
        if (analysts.isEmpty()) {
            throw ArchivistException("Unable to find a suitable analyst.")
        }

        for (analyst in analysts) {
            try {
                val client = WorkerNodeClient(analyst.url)
                // Never retry so we don't accidentally run the same command.
                client.maxRetries = 0
                client.connectTimeout = 1000
                // Wait up to 120 seconds for result.
                client.socketTimeout = 120 * 1000

                val resultT = client.executeTask(TaskStartT()
                        .setArgMap(Json.serialize(spec.args))
                        .setEnv(mapOf())
                        .setMasterHost(network.clusterAddr)
                        .setName("execute")
                        .setOrder(-1000)
                        .setSharedDir(sharedData.root.toString())
                        .setScript(Json.serialize(script)))

                return if (resultT.getResult() != null) {
                    mapOf<String, Any>(
                            "list" to Json.deserialize(resultT.getResult(), Json.LIST_OF_OBJECTS),
                            "errors" to makeRestFriendly(resultT.getErrors()))
                } else {
                    mapOf<String, Any>("errors" to makeRestFriendly(resultT.getErrors()))
                }

            } catch (e: ClusterConnectionException) {
                logger.warn("Unable to connect to analyst: {}", e.message)
            }

        }

        throw ArchivistException("All analysts timed out.")
    }

    @Throws(IOException::class)
    private fun copyUploadedFiles(files: Array<MultipartFile>): List<String> {
        val result = Lists.newArrayListWithCapacity<String>(files.size)
        for (file in files) {
            val ofile = ofs.prepare("tmp", file.originalFilename + file.size,
                    FileUtils.extension(file.originalFilename))
            val path = ofile.file.toPath()

            if (!ofile.exists()) {
                Files.copy(file.inputStream, path)
            }
            result.add(path.toString())
        }
        return result
    }

    private fun makeRestFriendly(errors: List<TaskErrorT>): List<Map<String, Any>> {
        val result = Lists.newArrayListWithCapacity<Map<String, Any>>(errors.size)
        for (error in errors) {
            val entry = Maps.newHashMap<String, Any>()
            entry.put("message", error.getMessage())
            entry.put("service", error.getOriginService())
            entry.put("skipped", error.isSkipped)
            entry.put("path", error.getPath())
            entry.put("assetId", error.getId())
            entry.put("processor", error.getProcessor())
            entry.put("phase", error.getPhase())
            entry.put("timestamp", error.getTimestamp())

            val stackTrace = Lists.newArrayList<Map<String, Any>>()
            entry.put("stackTrace", stackTrace)

            for (e in error.getStack()) {
                val stack = Maps.newHashMap<String, Any>()
                stack.put("className", e.getClassName())
                stack.put("file", e.getFile())
                stack.put("lineNumber", e.getLineNumber())
                stack.put("method", e.getMethod())
                stackTrace.add(stack)
            }
            result.add(entry)
        }
        return result
    }

    companion object {

        private val logger = LoggerFactory.getLogger(ImportServiceImpl::class.java)
    }

}
