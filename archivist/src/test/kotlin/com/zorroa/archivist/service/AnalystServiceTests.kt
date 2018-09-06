package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.common.domain.AnalystSpec
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.*
import kotlin.test.assertEquals

class AnalystServiceTests : AbstractTest() {

    @Autowired
    internal lateinit var analystService: AnalystService

    @Test
    fun testUpsert() {
        val spec1 = AnalystSpec(
                "http://localhost:1234",
                null,
                1024,
                648,
                0.5f)
        analystService.upsert(spec1)

        val spec2 = AnalystSpec(
                "http://localhost:1234",
                UUID.fromString("EF3B1E5A-31B5-4AEB-8C4E-7DA50F2AC592"),
                1024,
                1024,
                1.0f)
        val a2 = analystService.upsert(spec2)
        assertEquals(spec2.totalRamMb, a2.totalRamMb)
        assertEquals(spec2.freeRamMb, a2.freeRamMb)
        assertEquals(spec2.load, a2.load)
    }
}