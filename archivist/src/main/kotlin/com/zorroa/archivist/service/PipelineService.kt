package com.zorroa.archivist.service

import com.fasterxml.jackson.module.kotlin.readValue
import com.zorroa.archivist.config.ApplicationProperties
import com.zorroa.archivist.domain.*
import com.zorroa.archivist.repository.PipelineDao
import com.zorroa.common.util.Json
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

interface PipelineService {
    fun resolve(pipelines: List<String>) : MutableList<ProcessorRef>
    fun resolveDefault(type: PipelineType) : MutableList<ProcessorRef>
    fun create(spec: PipelineSpec) : Pipeline
    fun get(id: UUID) : Pipeline
    fun get(name: String) : Pipeline
    fun getAll(): List<Pipeline>
    fun getAll(type: PipelineType): List<Pipeline>
    fun getAll(page: Pager): PagedList<Pipeline>
    fun update(pipeline: Pipeline) : Boolean
    fun delete(id: UUID): Boolean
    fun getDefaultPipelineNames(type: PipelineType) : List<String>
    fun resolve(type: PipelineType, refs: List<ProcessorRef>?) : MutableList<ProcessorRef>
}

/**
 * The PipelineService handles the management of pipelines.
 */
@Service
@Transactional
class PipelineServiceImpl @Autowired constructor(
        private val pipelineDao : PipelineDao,
        private val properties: ApplicationProperties
): PipelineService, ApplicationListener<ContextRefreshedEvent> {

    override fun create(spec: PipelineSpec) : Pipeline {
        val p =  pipelineDao.create(spec)
        logger.info(Json.prettyString(p))
        return p
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

    override fun getAll(): List<Pipeline> {
        return pipelineDao.getAll()
    }

    override fun getAll(type: PipelineType): List<Pipeline> {
        return pipelineDao.getAll(type)
    }

    override fun getAll(page: Pager): PagedList<Pipeline> {
        return pipelineDao.getAll(page)
    }

    override fun delete(id: UUID): Boolean {
        return pipelineDao.delete(id)
    }

    override fun getDefaultPipelineNames(type: PipelineType) : List<String> {
        val names = when (type) {
            PipelineType.Import-> properties.getString("archivist.pipeline.default-import-pipelines")
            PipelineType.Export-> properties.getString("archivist.pipeline.default-export-pipelines")
            else -> throw IllegalArgumentException("There are no default $type pipelines")
        }
        return if (names != null) {
            names.split(',').map { it.trim() }
        }
        else {
            listOf()
        }
    }

    override fun resolveDefault(type: PipelineType) : MutableList<ProcessorRef> {
        val names = getDefaultPipelineNames(type)
        return resolve(names)
    }

    override fun resolve(pipelines: List<String>) : MutableList<ProcessorRef> {
        val processors = mutableListOf<ProcessorRef>()
        pipelines.forEach {
            val pipeline = pipelineDao.get(it)
            processors.addAll(pipeline.processors)
        }
        return processors
    }


    override fun resolve(type: PipelineType, refs: List<ProcessorRef>?) : MutableList<ProcessorRef>  {
        val result = mutableListOf<ProcessorRef>()

        refs?.forEach { ref->
            if (ref.className.startsWith("pipeline:")) {
                val name = ref.className.split(":", limit = 2)[1]

                val pl = try {
                    pipelineDao.get(UUID.fromString(name))
                } catch (e:IllegalArgumentException) {
                    pipelineDao.get(name)
                }

                if (pl.type != type) {
                    throw  throw IllegalArgumentException("Cannot have pipeline type " +
                            pl.type + " embedded in a " + type + " pipeline")
                }
                result.addAll(resolve(type, pl.processors))
            }
            else {
                result.add(ref)
                ref.execute?.let {
                    ref.execute = resolve(type, it)
                }
            }
        }

        return result
    }


    override fun onApplicationEvent(p0: ContextRefreshedEvent?) {
        val searchPath = properties.getList("archivist.pipeline.search-path")
        searchPath.forEach {
            val path = Paths.get(it.trim())
            if (Files.exists(path)) {
                for (file in Files.list(path)) {
                    val spec = Json.Mapper.readValue<PipelineSpec>(file.toFile())
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
