package com.zorroa.archivist.service

import com.fasterxml.jackson.core.type.TypeReference
import com.google.common.collect.Maps
import com.zorroa.archivist.domain.*
import com.zorroa.archivist.repository.PipelineDao
import com.zorroa.archivist.repository.PluginDao
import com.zorroa.archivist.repository.ProcessorDao
import com.zorroa.archivist.sdk.config.ApplicationProperties
import com.zorroa.sdk.domain.PagedList
import com.zorroa.sdk.domain.Pager
import com.zorroa.sdk.plugins.PluginException
import com.zorroa.sdk.plugins.PluginRegistry
import com.zorroa.sdk.plugins.PluginSpec
import com.zorroa.sdk.processor.ProcessorRef
import com.zorroa.sdk.processor.SharedData
import com.zorroa.sdk.util.Json
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Path
import java.util.*
import java.util.stream.Collectors


/**
 * Created by chambers on 6/28/16.
 */
interface PluginService {
    fun getAllPlugins(): List<Plugin>

    fun getAllProcessors(): List<Processor>

    fun installPlugin(file: MultipartFile): Plugin

    fun installPlugin(zipFilePath: Path): Plugin

    fun installBundledPipelines()

    fun getAllPlugins(page: Pager): PagedList<Plugin>

    fun getPlugin(name: String): Plugin

    fun getPlugin(id: UUID): Plugin

    fun deletePlugin(plugin: Plugin): Boolean

    fun getAllProcessors(plugin: Plugin): List<Processor>

    fun getProcessor(id: UUID): Processor

    fun getProcessorRef(name: String, args: Map<String, Any>): ProcessorRef

    fun getProcessorRef(ref: ProcessorRef): ProcessorRef

    fun getProcessorRefs(pipelineId: UUID): List<ProcessorRef>?

    fun getProcessorRefs(refs: List<ProcessorRef>?): List<ProcessorRef>?

    fun getAllProcessors(filter: ProcessorFilter): List<Processor>

    fun getProcessorRef(name: String): ProcessorRef

    fun getProcessor(name: String): Processor

    fun installAndRegisterAllPlugins()
}

@Service
@Transactional
class PluginServiceImpl @Autowired constructor(
        private val properties: ApplicationProperties,
        private val pluginDao: PluginDao,
        private val processorDao: ProcessorDao,
        private val pipelineDao: PipelineDao,
        sharedData: SharedData
) : PluginService {

    private val pluginRegistry: PluginRegistry = PluginRegistry(sharedData)
    private val pluginPath: Path = sharedData.pluginPath

    init {
        logger.info("Loading plugins from: {}", pluginPath)
        if (pluginPath.toFile().mkdirs()) {
            logger.info("Plugin path did not exist: {}", pluginPath)
        }
    }

    override fun installPlugin(file: MultipartFile): Plugin {
        synchronized(pluginRegistry) {
            try {
                val pluginPath = pluginRegistry.unpackPluginPackage(file.inputStream)
                val plugin = pluginRegistry.loadPlugin(pluginPath)
                createPluginRecord(plugin)
                return pluginDao.get(plugin.name)

            } catch (e: Exception) {
                throw PluginException("Failed to install plugin, " + e.message, e)
            }

        }
    }

    override fun installPlugin(zipFilePath: Path): Plugin {
        synchronized(pluginRegistry) {
            if (zipFilePath.startsWith(pluginPath)) {
                throw PluginException("Plugin '$zipFilePath' is already installed.")
            }

            try {
                val pluginPath = pluginRegistry.unpackPluginPackage(zipFilePath)
                val plugin = pluginRegistry.loadPlugin(pluginPath)
                createPluginRecord(plugin)
                return pluginDao.get(plugin.name)
            } catch (e: Exception) {
                throw PluginException("Failed to install plugin, " + e.message, e)
            }

        }
    }

    private fun createPluginRecord(spec: PluginSpec) {
        var newOrChanged = false
        var plugin: Plugin
        try {
            plugin = pluginDao.get(spec.name)
            if (spec.md5 != plugin.md5) {
                logger.info("The plugin {} has changed, reloading.", plugin.name)
                newOrChanged = true
                pluginDao.update(plugin.id, spec)
            }
        } catch (e: EmptyResultDataAccessException) {
            newOrChanged = true
            plugin = pluginDao.create(spec)
        }

        /**
         * If the plugin is a new version or just a new plugin then we register
         * the other stuff.
         */
        if (newOrChanged) {
            registerProcessors(plugin, spec)
            registerPipelines(plugin, spec)
        }
    }

    fun registerPipelines(p: Plugin, spec: PluginSpec) {
        if (spec.pipelines == null) {
            return
        }

        for (pl in spec.pipelines) {
            val pipeline: Pipeline
            try {
                // remove the old one
                pipeline = pipelineDao.get(pl.name)
                val result = pipelineDao.delete(pipeline.id)
                if (!result) {
                    // its the standard pipeline, just ignore for now
                    continue
                }
            } catch (e: EmptyResultDataAccessException) {
                // ignore
            }

            /**
             * Update the pipeline and validate the processors.
             */
            try {
                pipelineDao.create(PipelineSpecV()
                        .setName(pl.name)
                        .setDescription(pl.description)
                        .setProcessors(pl.processors.stream().map { ref -> getProcessorRef(ref) }.collect(Collectors.toList()))
                        .setType(pl.type))
            } catch (e: EmptyResultDataAccessException) {
                logger.warn("Failed to register pipeline: {}", pl)
            }

        }
    }

    override fun installAndRegisterAllPlugins() {
        pluginRegistry.installedVersions = pluginDao.getInstalledVersions()
        pluginRegistry.installAllPlugins(
                properties.split("archivist.path.pluginSearchPath", ":"))

        pluginRegistry.loadInstalledPlugins()

        for (spec in pluginRegistry.plugins) {
            createPluginRecord(spec)
        }
    }

    override fun installBundledPipelines() {

        val path = properties.getPath("archivist.path.home").resolve("config/pipelines.json")
        val file = path.toFile()
        if (!file.exists()) {
            logger.info("No bundled pipelines, skipping")
            return
        }

        logger.info("Installing bundled pipeline: {}", path)

        try {
            val pipelines = Json.Mapper.readValue<List<Pipeline>>(path.toFile(),
                    object : TypeReference<List<Pipeline>>() {

                    })
            for (pl in pipelines) {
                if (pipelineDao.exists(pl.name)) {
                    continue
                }
                try {
                    logger.info("Installing bundled pipeline: {} {}, standard={}", pl.name, pl.isStandard)
                    pipelineDao.create(PipelineSpecV()
                            .setStandard(pl.isStandard)
                            .setName(pl.name)
                            .setDescription(pl.description)
                            .setProcessors(pl.processors.stream().map { ref -> getProcessorRef(ref) }.collect(Collectors.toList()))
                            .setType(pl.type))
                } catch (e: EmptyResultDataAccessException) {
                    logger.warn("Failed to register pipeline: {}", pl, e)
                }

            }
        } catch (e: Exception) {
            logger.warn("Unable to register bundled pipeline file", e)
        }

    }

    fun registerProcessors(plugin: Plugin, pspec: PluginSpec) {
        if (pspec.processors == null || pspec.processors.isEmpty()) {
            logger.warn("Plugin {} contains no processors", plugin)
            return
        }

        processorDao.deleteAll(plugin)

        for (spec in pspec.processors) {

            val proc: Processor
            try {
                proc = processorDao.get(spec.className)
                processorDao.delete(proc.id)
            } catch (e: EmptyResultDataAccessException) {
                // ignore
            }

            processorDao.create(plugin, spec)
        }
    }

    /*
     * -------------------------------------------------------------------------------------------
     */

    override fun getAllPlugins(page: Pager): PagedList<Plugin> {
        return pluginDao.getAll(page)
    }

    override fun getAllPlugins(): List<Plugin> {
        return pluginDao.getAll()
    }

    override fun getPlugin(name: String): Plugin {
        return pluginDao.get(name)
    }

    override fun getPlugin(id: UUID): Plugin {
        return pluginDao.get(id)
    }

    override fun deletePlugin(plugin: Plugin): Boolean {
        val deleted =  pluginDao.delete(plugin.id)
        if (deleted) {
            processorDao.deleteAll(plugin)
        }
        return deleted
    }

    override fun getAllProcessors(plugin: Plugin): List<Processor> {
        return processorDao.getAll(plugin)
    }

    override fun getProcessor(id: UUID): Processor {
        return processorDao.get(id)
    }

    override fun getProcessorRef(name: String, args: Map<String, Any>): ProcessorRef {
        return processorDao.getRef(name).setArgs(args)
    }

    override fun getProcessorRef(name: String): ProcessorRef {
        return processorDao.getRef(name).setArgs(Maps.newHashMap())
    }

    override fun getProcessorRef(ref: ProcessorRef): ProcessorRef {
        return processorDao.getRef(ref.className)
                .setArgs(ref.args)
                .addToFilters(ref.filters)
    }

    override fun getProcessorRefs(pipelineId: UUID): List<ProcessorRef>? {
        return getProcessorRefs(pipelineDao.get(pipelineId).processors)
    }

    fun getProcessorRefs(refs: List<ProcessorRef>?, pr: Deque<UUID>): List<ProcessorRef>? {
        if (refs == null) {
            return null
        }
        val result = mutableListOf<ProcessorRef>()
        for (ref in refs) {

            val pipelineId = ref.pipeline
            if (pipelineId != null) {

                val pipeline = try {
                    pipelineDao.get(UUID.fromString(pipelineId))
                } catch (e:IllegalArgumentException) {
                    pipelineDao.get(pipelineId)
                }

                if (pr.contains(pipeline.id)) {
                    throw IllegalStateException("Self referencing pipeline: " + pipeline.id)
                }
                pr.add(pipeline.id)

                val procs : List<ProcessorRef>? = getProcessorRefs(pipeline.processors, pr)
                procs?.let { result.addAll(procs) }
                pr.pop()

            } else {
                val ref2 = processorDao.getRef(ref.className)
                        .setArgs(ref.args)
                        .addToFilters(ref.filters)
                        .setExecute(ref.execute)
                if (ref.fileTypes != null) {
                    if (ref2.fileTypes == null) {
                        ref2.fileTypes = ref.fileTypes
                    } else {
                        ref2.fileTypes.addAll(ref.fileTypes)
                    }
                }

                result.add(ref2)
                ref2.execute = getProcessorRefs(ref2.execute)
            }
        }
        return result
    }

    override fun getProcessorRefs(refs: List<ProcessorRef>?): List<ProcessorRef>? {
        return getProcessorRefs(refs, ArrayDeque<UUID>())
    }

    override fun getAllProcessors(
            filter: ProcessorFilter): List<Processor> {
        return processorDao.getAll(filter)
    }

    override fun getProcessor(name: String): Processor {
        return processorDao.get(name)
    }

    override fun getAllProcessors(): List<Processor> {
        return processorDao.getAll()
    }

    companion object {

        private val logger = LoggerFactory.getLogger(PluginServiceImpl::class.java)
    }
}
