package com.zorroa.archivist.rest

import com.zorroa.archivist.domain.AuditLogEntry
import com.zorroa.archivist.domain.AuditLogFilter
import com.zorroa.archivist.domain.AuditLogType
import com.zorroa.archivist.domain.Pager
import com.zorroa.common.repository.KPagedList
import org.junit.Before
import org.junit.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuditLogControllerTests : MockMvcTest() {

    @Before
    fun init() {
        addTestAssets("set04/standard")
    }

    override fun requiresElasticSearch(): Boolean {
        return true
    }

    @Test
    @Throws(Exception::class)
    fun testSearch() {
        val logs = resultForPostContent<KPagedList<AuditLogEntry>>(
            "/api/v1/auditlog/_search",
            AuditLogFilter(types = listOf(AuditLogType.Created)))
        assertTrue(logs.size() > 0)
    }

    @Test
    fun testFindOne() {
        val asset = indexService.getAll(Pager.first(1)).list[0]
        val auditLogEntry = resultForPostContent<AuditLogEntry>(
            "/api/v1/auditlog/_findOne",
            AuditLogFilter(assetIds = listOf(UUID.fromString(asset.id))))
        assertEquals(asset.id, auditLogEntry.assetId.toString())
    }

    @Test
    fun testFindOneFailsWhenMultipleFound() {
        assertClientErrorForPostContent(
            "/api/v1/auditlog/_findOne",
            AuditLogFilter())
    }

    @Test
    fun testFindOneFailsOnNotFound() {
        assertClientErrorForPostContent(
            "/api/v1/auditlog/_findOne",
            AuditLogFilter(assetIds = listOf(UUID.randomUUID())))
    }
}
