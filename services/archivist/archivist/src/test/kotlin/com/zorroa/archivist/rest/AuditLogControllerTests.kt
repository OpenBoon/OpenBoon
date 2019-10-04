package com.zorroa.archivist.rest

import com.zorroa.archivist.domain.AuditLogEntry
import com.zorroa.archivist.domain.AuditLogFilter
import com.zorroa.archivist.domain.AuditLogType
import com.zorroa.archivist.domain.Document
import com.zorroa.archivist.domain.Field
import com.zorroa.archivist.domain.FieldEdit
import com.zorroa.archivist.domain.FieldEditSpec
import com.zorroa.archivist.domain.FieldSpecExpose
import com.zorroa.archivist.domain.Pager
import com.zorroa.common.repository.KPagedList
import org.assertj.core.api.Assertions
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
    fun testFieldEditAuditLog() {
        addTestAssets("set04/standard")
        val asset = indexService.getAll(Pager.first())[0]

        val field = fieldSystemService.createField(
            FieldSpecExpose("File Ext", "source.extension").apply { editable = true })

        editField(asset, field, "bob")

        val log = resultForPostContent<AuditLogEntry>(
            "/api/v1/auditlog/_findOne",
            AuditLogFilter(
                assetIds = listOf(UUID.fromString(asset.id)),
                types = listOf(AuditLogType.Changed)
            )
        )
        assertEquals("jpg", log.oldValue)
        assertEquals("bob", log.newValue)
    }

    @Test
    fun testFieldEditAuditLogSort() {
        addTestAssets("set04/standard")

        val field = fieldSystemService.createField(
            FieldSpecExpose("File Ext", "source.extension").apply { editable = true })

        editField(indexService.getAll(Pager.first())[0], field, "bob")
        editField(indexService.getAll(Pager.first())[1], field, "alice")

        val logs1 = resultForPostContent<KPagedList<AuditLogEntry>>(
            "/api/v1/auditlog/_search",
            AuditLogFilter(
                types = listOf(AuditLogType.Changed)
            ))
        Assertions.assertThat(logs1).hasSize(2)
        Assertions.assertThat(logs1.list)
            .isSortedAccordingTo(compareBy { it.newValue.toString() })

        val logs2 = resultForPostContent<KPagedList<AuditLogEntry>>(
            "/api/v1/auditlog/_search",
            AuditLogFilter(
                types = listOf(AuditLogType.Changed)
            ).apply { sort = listOf("newValue:d") })

        Assertions.assertThat(logs2.list)
            .isSortedAccordingTo(compareByDescending { it.newValue.toString() })
    }

    private fun editField(
        asset: Document,
        field: Field,
        newValue: String
    ) {
        resultForPostContent<FieldEdit>(
            "/api/v1/fieldEdits",
            FieldEditSpec(UUID.fromString(asset.id), field.id, null, newValue = newValue)
        )
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
