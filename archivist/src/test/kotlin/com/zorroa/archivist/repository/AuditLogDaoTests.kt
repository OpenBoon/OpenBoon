package com.zorroa.archivist.repository

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.*
import com.zorroa.archivist.security.getUserId
import com.zorroa.common.util.Json
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.*
import kotlin.test.assertEquals

class AuditLogDaoTests : AbstractTest() {

    @Autowired
    lateinit var auditLogDao : AuditLogDao

    @Test
    fun testCreateNoField() {
        val spec = AuditLogEntrySpec(
                UUID.fromString("D585D35C-AF3D-4AEB-A78F-42C61C1077CB"),
                AuditLogType.Created,
                message="The asset was created")
        val entry = auditLogDao.create(spec)
        assertEquals(spec.assetId, entry.assetId)
        assertEquals(spec.type, entry.type)
        assertEquals(spec.message, entry.message)
        assertEquals(spec.attrName, entry.attrName)
        assertEquals(spec.value, entry.value)
    }

    @Test
    fun testCreateFieldChange() {
        val spec = AuditLogEntrySpec(
                UUID.fromString("D585D35C-AF3D-4AEB-A78F-42C61C1077CB"),
                AuditLogType.Changed,
                attrName="irm.documentType",
                value="cat")
        val entry = auditLogDao.create(spec)
        assertEquals(spec.attrName, entry.attrName)
        assertEquals(Json.serializeToString("cat"), entry.value)
    }

    @Test
    fun testCreateFieldEdit() {
        val fspec = FieldSpec("Notes", null, AttrType.StringContent, true)
        val field = fieldSystemService.createField(fspec)

        val spec = AuditLogEntrySpec(
                UUID.fromString("D585D35C-AF3D-4AEB-A78F-42C61C1077CB"),
                AuditLogType.Changed,
                fieldId=field.id,
                attrName=field.attrName,
                value="cat")
        val entry = auditLogDao.create(spec)
        assertEquals(spec.attrName, entry.attrName)


        val filter = AuditLogFilter(fieldIds=listOf(field.id))
        assertEquals(1, auditLogDao.getAll(filter).page.totalCount)
    }

    @Test
    fun testBatchCreate() {
        val specs = mutableListOf<AuditLogEntrySpec>()
        for (i in 1..10) {
            specs.add(AuditLogEntrySpec(
                    UUID.fromString("D585D35C-AF3D-4AEB-A78F-42C61C1077CB"),
                    AuditLogType.Changed,
                    attrName="irm.documentType",
                    value="cat"))
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
                    attrName="irm.documentType",
                    value="cat"))
        }
        val filter = AuditLogFilter(assetIds=listOf(UUID.fromString(assetId)))

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
                    attrName="irm.documentType",
                    value="cat"))
        }
        val filter = AuditLogFilter(assetIds=listOf(UUID.fromString(assetId), UUID.randomUUID()))
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
                    attrName="irm.documentType",
                    value="cat"))
        }
        val filter = AuditLogFilter(userIds=listOf(getUserId()))
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
                    attrName="irm.documentType",
                    value="cat"))
        }
        val filter = AuditLogFilter(
                assetIds=listOf(UUID.fromString(assetId)),
                userIds=listOf(getUserId()),
                types=listOf(AuditLogType.Changed, AuditLogType.Created),
                attrNames=listOf("irm.documentType"),
                timeCreated = LongRangeFilter(0, System.currentTimeMillis()+1000))
        assertEquals(10, auditLogDao.batchCreate(specs))
        assertEquals(10, auditLogDao.getAll(filter).page.totalCount)
    }
}