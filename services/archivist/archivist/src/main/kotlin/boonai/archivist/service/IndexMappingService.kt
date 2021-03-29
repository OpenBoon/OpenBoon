package boonai.archivist.service

import boonai.archivist.clients.EsRestClient
import boonai.archivist.domain.Field
import boonai.archivist.domain.IndexRoute
import boonai.archivist.repository.FieldDao
import boonai.common.service.security.getProjectId
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Handles modification to the ES index mappping.
 */
interface IndexMappingService {
    fun addAllFieldsToIndex(index: IndexRoute)
    fun addFieldToIndex(field: Field, index: IndexRoute)
    fun addFieldToIndex(field: Field)
}

@Service
@Transactional
class IndexMappingServiceImpl(
    val indexRoutingService: IndexRoutingService,
    val fieldDao: FieldDao
) : IndexMappingService {

    override fun addFieldToIndex(field: Field, index: IndexRoute) {
        val client = indexRoutingService.getClusterRestClient(index)
        addFieldToIndex(field.getPath(), field.type, client)
    }

    override fun addFieldToIndex(field: Field) {
        val client = indexRoutingService.getProjectRestClient()
        addFieldToIndex(field.getPath(), field.type, client)
    }

    override fun addAllFieldsToIndex(index: IndexRoute) {
        val client = indexRoutingService.getClusterRestClient(index)
        for (field in fieldDao.getAllByProjectId(getProjectId())) {
            logger.info("Adding field ${field.getPath()} to index ${client.route.indexUrl}")
            addFieldToIndex(field.getPath(), field.type, client)
        }
        client.refresh()
    }

    fun addFieldToIndex(name: String, type: String, client: EsRestClient? = null) {
        val rest = client ?: indexRoutingService.getProjectRestClient()
        try {
            logger.info("Adding field '{}' of type '{}' to index {}", name, type, rest.route.indexUrl)
            val frag = createMappingFragment(name, type)
            rest.updateMapping(frag)
        } catch (e: Exception) {
            logger.warn("Failed to create attr: $name with type $type", e)
            throw e
        }
    }

    /**
     * Create the necessary JSON body for adding a field to the mapping.
     */
    fun createMappingFragment(name: String, type: String): Map<String, Any> {
        val fieldDef = getFieldDefinition(type)
        val root = mutableMapOf<String, Any>("properties" to mutableMapOf<String, Any>())
        var current = root
        name.split('.').forEach {
            val next = mutableMapOf<String, Any>()
            val props = mutableMapOf<String, Map<String, Any>>(it to next)
            current["properties"] = props
            current = next
        }
        fieldDef.forEach { (k, v) ->
            current[k] = v
        }
        return root
    }

    /**
     * Get a field definition.
     */
    fun getFieldDefinition(type: String): Map<String, Any> {
        return if (type == "fulltext_keyword") {
            mapOf(
                "type" to "keyword",
                "fields" to mapOf(
                    "fulltext" to
                        mapOf(
                            "type" to "text",
                            "analyzer" to "default"
                        )
                )
            )
        } else {
            mapOf("type" to type)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(IndexMappingServiceImpl::class.java)
    }
}
