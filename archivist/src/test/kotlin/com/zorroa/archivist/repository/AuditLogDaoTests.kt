package com.zorroa.archivist.repository

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.AuditLogEntrySpec
import com.zorroa.archivist.domain.AuditLogType
import com.zorroa.common.util.Json
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.*
import kotlin.test.assertEquals

class AuditLogDaoTests : AbstractTest() {


    @Autowired
    lateinit var auditLogDao : AuditLogDao

    @Before
    fun init() {

    }

    @Test
    fun testCreateNoField() {
        val spec = AuditLogEntrySpec(
                UUID.fromString("D585D35C-AF3D-4AEB-A78F-42C61C1077CB"),
                AuditLogType.Created,
                "The asset was created")
        val entry = auditLogDao.create(spec)
        assertEquals(spec.assetId, entry.assetId)
        assertEquals(spec.type, entry.type)
        assertEquals(spec.message, entry.message)
        assertEquals(spec.field, entry.field)
        assertEquals(spec.value, entry.value)
    }

    @Test
    fun testCreateFieldChange() {
        val spec = AuditLogEntrySpec(
                UUID.fromString("D585D35C-AF3D-4AEB-A78F-42C61C1077CB"),
                AuditLogType.Changed,
                field="irm.documentType",
                value="cat")
        val entry = auditLogDao.create(spec)
        assertEquals(spec.field, entry.field)
        assertEquals(Json.serializeToString("cat"), entry.value)

    }


    @Test
    fun testBatchCreate() {
        val specs = mutableListOf<AuditLogEntrySpec>()
        for (i in 1..10) {
            specs.add(AuditLogEntrySpec(
                    UUID.fromString("D585D35C-AF3D-4AEB-A78F-42C61C1077CB"),
                    AuditLogType.Changed,
                    field="irm.documentType",
                    value="cat"))
        }

        assertEquals(10, auditLogDao.batchCreate(specs))
        assertEquals(10, jdbc.queryForObject("SELECT COUNT(1) FROM auditlog", Int::class.java))

    }
}