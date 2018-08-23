package com.zorroa.analyst.service

import com.fasterxml.jackson.module.kotlin.readValue
import com.zorroa.analyst.repository.PipelineDao
import com.zorroa.common.domain.*
import com.zorroa.common.util.Json
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

interface PipelineService {
    fun resolveExecute(pipelines: List<String>) : MutableList<ProcessorRef>
    fun buildDefaultProcessorList(type: PipelineType) : MutableList<ProcessorRef>
    fun create(spec: PipelineSpec) : Pipeline
    fun get(id: UUID) : Pipeline
    fun get(name: String) : Pipeline
    fun update(pipeline: Pipeline) : Boolean
    fun getDefaultPipelineNames(type: PipelineType) : List<String>
    fun resolveExecute(type: PipelineType, script: ZpsScript)
}

@Configuration
@ConfigurationProperties("analyst.pipeline")
class PipelineProperties {
    var searchPath : String? = null
    var defaultImportPipelines : String? = null
    var defaultExportPipelines : String? = null
}

/**
 * The PipelineService handles the management of pipelines.
 */
@Service
class PipelineServiceImpl : PipelineService, ApplicationListener<ContextRefreshedEvent> {

    @Autowired
    lateinit var pipelineDao : PipelineDao

    @Autowired
    lateinit var properties: PipelineProperties

    override fun create(spec: PipelineSpec) : Pipeline {
        return pipelineDao.create(spec)
    }

    override fun update(pipeline: Pipeline) : Boolean {
        return pipelineDao.update(pipeline)
    }

    override fun get(id: UUID) : Pipeline {
        return pipelineDao.get(id)
    }

    override fun get(name: String) : Pipeline {
        return pipelineDao.get(name)
    }

    override fun getDefaultPipelineNames(type: PipelineType) : List<String> {
        val names = when (type) {
            PipelineType.Import-> properties.defaultImportPipelines
            PipelineType.Export-> properties.defaultExportPipelines
            else -> throw IllegalArgumentException("There are no default $type pipelines")
        }
        logger.info(names)
        return if (names != null) {
            names.split(',').map { it.trim() }
        }
        else {
            listOf()
        }
    }

    override fun buildDefaultProcessorList(type: PipelineType) : MutableList<ProcessorRef> {
        val names = getDefaultPipelineNames(type)
        return resolveExecute(names)
    }

    override fun resolveExecute(pipelines: List<String>) : MutableList<ProcessorRef> {
        val processors = mutableListOf<ProcessorRef>()
        pipelines.forEach {
            logger.info("Pipeline: {}", it)
            val pipeline = pipelineDao.get(it)
            processors.addAll(pipeline.processors)
        }
        return processors
    }

    override fun resolveExecute(type: PipelineType, script: ZpsScript) {
        val execute = mutableListOf<ProcessorRef>()
        if (script.execute.orEmpty().isEmpty()) {
            execute.addAll(buildDefaultProcessorList(type))
        }
        else {
            script.execute?.forEach { ref ->
                if (ref.className.startsWith("pipeline:")) {
                    val name = ref.className.split(":", limit = 2)[1]
                    val pipeline = pipelineDao.get(name)
                    execute.addAll(pipeline.processors)
                } else {
                    execute.add(ref)
                }
            }
        }

        script.execute = execute
    }

    override fun onApplicationEvent(p0: ContextRefreshedEvent?) {
        properties.searchPath?.split(",")?.forEach {
            val path = Paths.get(it.trim())
            if (Files.exists(path)) {
                for (file in Files.list(path)) {
                    val spec = Json.Mapper.readValue<PipelineSpec>(file.toFile())
                    print("$spec")
                    try {
                        val pipe = pipelineDao.get(spec.name)
                        logger.info("Updating embedded pipeline: {} [{}]", spec.name, spec.type)
                        pipelineDao.update(pipe)
                    } catch (e: EmptyResultDataAccessException) {
                        logger.info("Creating embedded pipeline: {} [{}]", spec.name, spec.type)
                        val created = pipelineDao.create(spec)
                        logger.info("Created embedded pipeline: {}", created)
                    }
                    catch (e: Exception) {
                        logger.warn("Failed to load pipeline file:", e)
                    }
                }
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineServiceImpl::class.java)
    }
}
