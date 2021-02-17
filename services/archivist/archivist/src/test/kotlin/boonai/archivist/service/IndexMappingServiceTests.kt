package boonai.archivist.service

import boonai.archivist.AbstractTest
import boonai.archivist.domain.Asset
import boonai.archivist.domain.Field
import boonai.archivist.repository.FieldDao
import boonai.archivist.repository.IndexRouteDao
import boonai.common.service.security.getProjectId
import boonai.common.service.security.getZmlpActor
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext
import kotlin.test.assertEquals

class IndexMappingServiceTests : AbstractTest() {

    @Autowired
    lateinit var indexMappingService: IndexMappingService

    @Autowired
    lateinit var fieldDao: FieldDao

    @Autowired
    lateinit var indexRouteDao: IndexRouteDao

    @PersistenceContext
    lateinit var entityManager: EntityManager

    override fun requiresElasticSearch(): Boolean {
        return true
    }

    @Test
    fun testAddAllFieldsToIndex() {
        makeTestField("test-field-1", "keyword")
        makeTestField("test-field-2", "text")

        indexMappingService.addAllFieldsToIndex(indexRouteDao.getProjectRoute())
        val mapping = indexRoutingService.getProjectRestClient().getMapping()
        val doc = Asset("1", mapping.toMutableMap())
        val idx = mapping.keys.toList()[0]

        val prefix = "$idx.mappings.properties.custom.properties"
        assertEquals("keyword", doc.getAttr("$prefix.test-field-1.type") ?: "")
        assertEquals("text", doc.getAttr("$prefix.test-field-2.type") ?: "")
    }

    @Test
    fun testFieldToIndex() {
        val field = makeTestField("test-field-1", "integer")
        indexMappingService.addFieldToIndex(field)

        val client = indexRoutingService.getProjectRestClient()
        val mapping = client.getMapping()
        val doc = Asset("1", mapping.toMutableMap())
        val idx = mapping.keys.toList()[0]

        val prefix = "$idx.mappings.properties.custom.properties"
        assertEquals("integer", doc.getAttr("$prefix.test-field-1.type") ?: "")
    }

    @Test
    fun testFieldToIndexWithRoute() {
        val field = makeTestField("test-field-1", "integer")
        indexMappingService.addFieldToIndex(field, indexRouteDao.getProjectRoute())

        val client = indexRoutingService.getProjectRestClient()
        val mapping = client.getMapping()
        val doc = Asset("1", mapping.toMutableMap())
        val idx = mapping.keys.toList()[0]

        val prefix = "$idx.mappings.properties.custom.properties"
        assertEquals("integer", doc.getAttr("$prefix.test-field-1.type") ?: "")
    }

    fun makeTestField(name: String, type: String): Field {
        val actor = getZmlpActor().toString()
        val time = System.currentTimeMillis()
        val field = Field(
            UUID.randomUUID(),
            getProjectId(),
            name,
            type,
            time, time,
            actor, actor
        )
        fieldDao.saveAndFlush(field)

        return field
    }
}
