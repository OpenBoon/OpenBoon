package com.zorroa.irm.studio.service

import com.fasterxml.jackson.module.kotlin.readValue
import com.zorroa.irm.studio.Json
import com.zorroa.irm.studio.domain.Pipeline
import com.zorroa.irm.studio.domain.PipelineSpec
import com.zorroa.irm.studio.repository.PipelineDao
import com.zorroa.studio.sdk.ProcessorRef
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.stereotype.Service
import java.util.*
import javax.annotation.PostConstruct

interface PipelineService {
    fun buildProcessorList(pipelines: List<String>) : MutableList<ProcessorRef>
    fun buildDefaultProcessorList() : MutableList<ProcessorRef>
    fun create(spec: PipelineSpec) : Pipeline
    fun get(id: UUID) : Pipeline
    fun get(name: String) : Pipeline
    fun getDefaultPipelineList() : List<String>
    fun update(pipeline: Pipeline) : Boolean
}

@Configuration
@ConfigurationProperties("zorroa.pipeline")
class ZorroaPipelineConfiguration {

    var defaultPipelines : String? = null
}

/**
 * The PipelineService handles the management of pipelines.
 */
@Service
class PipelineServiceImpl : PipelineService, ApplicationListener<ContextRefreshedEvent> {

    @Autowired
    lateinit var pipelineDao : PipelineDao

    @Autowired
    lateinit var settings: ZorroaPipelineConfiguration

    val defaultPipelines = mutableListOf<String>()

    @PostConstruct
    fun setup() {
        settings.defaultPipelines
                ?.split(",")
                ?.forEach {
                    defaultPipelines.add(it.trim())
                }
        logger.info("default Pipelines: {}", defaultPipelines)
    }

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

    override fun getDefaultPipelineList(): List<String> {
        return defaultPipelines
    }

    override fun buildDefaultProcessorList() : MutableList<ProcessorRef> {
        return buildProcessorList(defaultPipelines)
    }

    override fun buildProcessorList(pipelines: List<String>) : MutableList<ProcessorRef> {
        val processors = mutableListOf<ProcessorRef>()
        pipelines.forEach {
            logger.info("Pipeline: {}", it)
            val pipeline = pipelineDao.get(it)
            processors.addAll(pipeline.processors)
        }
        return processors
    }

    override fun onApplicationEvent(p0: ContextRefreshedEvent?) {
        val cl = this.javaClass.classLoader
        val resolver = PathMatchingResourcePatternResolver(cl)
        val resources = resolver.getResources("classpath:/pipelines/*.json")

        for (resource in resources) {
            val spec = Json.Mapper.readValue<PipelineSpec>(resource.inputStream)
            print("$spec")
            try {
                val pipe = pipelineDao.get(spec.name)
                logger.info("Updating embedded pipeline: {}", resource.filename)
                pipelineDao.update(pipe)
            }
            catch (e: EmptyResultDataAccessException) {
                logger.info("Creating embedded pipeline: {}", resource.filename)
                val created = pipelineDao.create(spec)
                logger.info("Created embedded pipeline: {}", created)
            }
        }

        logger.info("Default pipelines are: {}", getDefaultPipelineList())

    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineServiceImpl::class.java)
    }
}
