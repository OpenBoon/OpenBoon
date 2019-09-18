package com.zorroa.archivist.repository

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.AttrType
import com.zorroa.archivist.domain.AuditLogEntrySpec
import com.zorroa.archivist.domain.AuditLogFilter
import com.zorroa.archivist.domain.AuditLogType
import com.zorroa.archivist.domain.FieldSpecCustom
import com.zorroa.archivist.security.getUserId
import com.zorroa.common.util.Json
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuditLogDaoTests : AbstractTest() {

    @Autowired
    lateinit var auditLogDao: AuditLogDao

    @Test
    fun testCreateNoField() {
        val spec = AuditLogEntrySpec(
                UUID.fromString("D585D35C-AF3D-4AEB-A78F-42C61C1077CB"),
                AuditLogType.Created,
                message = "The asset was created")
        val entry = auditLogDao.create(spec)
        assertEquals(spec.assetId, entry.assetId)
        assertEquals(spec.type, entry.type)
        assertEquals(spec.message, entry.message)
        assertEquals(spec.attrName, entry.attrName)
        assertEquals(spec.newValue, entry.newValue)
    }

    @Test
    fun testCreateFieldChange() {
        val spec = AuditLogEntrySpec(
                UUID.fromString("D585D35C-AF3D-4AEB-A78F-42C61C1077CB"),
                AuditLogType.Changed,
                attrName = "irm.documentType",
                newValue = "cat")
        val entry = auditLogDao.create(spec)
        assertEquals(spec.attrName, entry.attrName)
        assertEquals(Json.serializeToString("cat"), entry.newValue)
    }

    @Test
    fun testCreateFieldEdit() {
        val fspec = FieldSpecCustom("Notes", "test.test_content", AttrType.StringContent)
        val field = fieldSystemService.createField(fspec)

        val spec = AuditLogEntrySpec(
                UUID.fromString("D585D35C-AF3D-4AEB-A78F-42C61C1077CB"),
                AuditLogType.Changed,
                fieldId = field.id,
                attrName = field.attrName,
                newValue = "cat")
        val entry = auditLogDao.create(spec)
        assertEquals(spec.attrName, entry.attrName)

        val filter = AuditLogFilter(fieldIds = listOf(field.id))
        assertEquals(1, auditLogDao.getAll(filter).page.totalCount)
    }

    @Test
    fun testBatchCreate() {
        val specs = mutableListOf<AuditLogEntrySpec>()
        for (i in 1..10) {
            specs.add(AuditLogEntrySpec(
                    UUID.fromString("D585D35C-AF3D-4AEB-A78F-42C61C1077CB"),
                    AuditLogType.Changed,
                    attrName = "irm.documentType",
                    newValue = "cat"))
        }

        assertEquals(10, auditLogDao.batchCreate(specs))
        assertEquals(10, jdbc.queryForObject("SELECT COUNT(1) FROM auditlog", Int::class.java))
    }

    @Test
    fun testCount() {
        val specs = mutableListOf<AuditLogEntrySpec>()
        val assetId = "D585D35C-AF3D-4AEB-A78F-42C61C1077CB"
        for (i in 1..10) {
            specs.add(AuditLogEntrySpec(
                    UUID.fromString(assetId),
                    AuditLogType.Changed,
                    attrName = "irm.documentType",
                    newValue = "cat"))
        }
        val filter = AuditLogFilter(assetIds = listOf(UUID.fromString(assetId)))

        assertEquals(10, auditLogDao.batchCreate(specs))
        assertEquals(10, auditLogDao.count(filter))
    }

    @Test
    fun testGetAllByAssetIds() {

        val specs = mutableListOf<AuditLogEntrySpec>()
        val assetId = "D585D35C-AF3D-4AEB-A78F-42C61C1077CB"
        for (i in 1..10) {
            specs.add(AuditLogEntrySpec(
                    UUID.fromString(assetId),
                    AuditLogType.Changed,
                    attrName = "irm.documentType",
                    newValue = "cat"))
        }
        val filter = AuditLogFilter(assetIds = listOf(UUID.fromString(assetId), UUID.randomUUID()))
        assertEquals(10, auditLogDao.batchCreate(specs))
        assertEquals(10, auditLogDao.getAll(filter).page.totalCount)
    }

    @Test
    fun testGetAllByUserIds() {

        val specs = mutableListOf<AuditLogEntrySpec>()
        val assetId = "D585D35C-AF3D-4AEB-A78F-42C61C1077CB"
        for (i in 1..10) {
            specs.add(AuditLogEntrySpec(
                    UUID.fromString(assetId),
                    AuditLogType.Changed,
                    attrName = "irm.documentType",
                    newValue = "cat"))
        }
        val filter = AuditLogFilter(userIds = listOf(getUserId()))
        assertEquals(10, auditLogDao.batchCreate(specs))
        assertEquals(10, auditLogDao.getAll(filter).page.totalCount)
    }

    @Test
    fun testGetAll() {

        val specs = mutableListOf<AuditLogEntrySpec>()
        val assetId = "D585D35C-AF3D-4AEB-A78F-42C61C1077CB"
        for (i in 1..10) {
            specs.add(AuditLogEntrySpec(
                    UUID.fromString(assetId),
                    AuditLogType.Changed,
                    attrName = "irm.documentType",
                    newValue = "cat"))
        }
        val filter = AuditLogFilter(
                assetIds = listOf(UUID.fromString(assetId)),
                userIds = listOf(getUserId()),
                types = listOf(AuditLogType.Changed, AuditLogType.Created),
                attrNames = listOf("irm.documentType"),
                timeCreated = LongRangeFilter(0, System.currentTimeMillis() + 1000))
        assertEquals(10, auditLogDao.batchCreate(specs))
        assertEquals(10, auditLogDao.getAll(filter).page.totalCount)
    }

    @Test
    fun testGetAllSorted() {
        val specs = mutableListOf<AuditLogEntrySpec>()
        val assetId = "D585D35C-AF3D-4AEB-A78F-42C61C1077CB"
        for (i in 1..10) {
            specs.add(
                AuditLogEntrySpec(
                    UUID.fromString(assetId),
                    AuditLogType.Changed,
                    attrName = "irm.documentType",
                    newValue = "cat"
                )
            )
        }
        auditLogDao.batchCreate(specs)
        // Just test the DB allows us to sort on each defined sortMap col
        for (field in AuditLogFilter().sortMap.keys) {
            var filter = AuditLogFilter().apply {
                sort = listOf("$field:a")
            }
            val page = auditLogDao.getAll(filter)
            assertTrue(page.size() > 0)
        }
    }

    @Test
    fun testGetAllSortedByModifiedValue() {
        val specs = mutableListOf<AuditLogEntrySpec>()
        val assetId = "D585D35C-AF3D-4AEB-A78F-42C61C1077CB"
        for (i in 1..10) {
            specs.add(
                AuditLogEntrySpec(
                    UUID.fromString(assetId),
                    AuditLogType.Changed,
                    attrName = "irm.documentType",
                    newValue = if (i % 2 == 0) {
                        "cat"
                    } else {
                        "dog"
                    }
                )
            )
        }
        auditLogDao.batchCreate(specs)

        val filter1 = AuditLogFilter().apply {
            sort = listOf("newValue:a")
        }
        assertThat(auditLogDao.getAll(filter1).list)
            .isSortedAccordingTo(compareBy { it.newValue.toString() })

        val filter2 = AuditLogFilter().apply {
            sort = listOf("newValue:d")
        }
        assertThat(auditLogDao.getAll(filter2).list)
            .isSortedAccordingTo(compareByDescending { it.newValue.toString() })
    }

    @Test
    fun testGetAllSortedByUserEmail() {
        val assetId = "D585D35C-AF3D-4AEB-A78F-42C61C1077CB"

        val spec = AuditLogEntrySpec(
            UUID.fromString(assetId),
            AuditLogType.Changed,
            attrName = "irm.documentType",
            newValue = "deed"
        )

        authenticate()
        auditLogDao.create(spec)

        authenticate("user")
        auditLogDao.create(spec)

        authenticate("librarian")
        auditLogDao.create(spec)

        assertThat(auditLogDao.getAll(AuditLogFilter())).hasSize(3)

        val filter1 = AuditLogFilter().apply {
            sort = listOf("userEmail:a")
        }
        assertThat(auditLogDao.getAll(filter1).list)
            .isSortedAccordingTo(compareBy { it.user.email })

        val filter2 = AuditLogFilter().apply {
            sort = listOf("userEmail:d")
        }
        assertThat(auditLogDao.getAll(filter2).list)
            .isSortedAccordingTo(compareByDescending { it.user.email })
    }
}
