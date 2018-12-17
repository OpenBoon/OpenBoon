package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.security.AnalystAuthentication
import com.zorroa.common.domain.AnalystSpec
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import java.util.*
import kotlin.test.assertEquals

class AnalystServiceTests : AbstractTest() {

    @Autowired
    internal lateinit var analystService: AnalystService

    @Test
    fun testUpsert() {
        SecurityContextHolder.getContext().authentication = AnalystAuthentication("https://127.0.0.1:5000")
        val spec1 = AnalystSpec(
                1024,
                648,
                1024,
                0.5f,
                null)
        analystService.upsert(spec1)

        val spec2 = AnalystSpec(
                1024,
                1024,
                1024,
                1.0f,
                UUID.fromString("EF3B1E5A-31B5-4AEB-8C4E-7DA50F2AC592"))
        val a2 = analystService.upsert(spec2)
        assertEquals(spec2.totalRamMb, a2.totalRamMb)
        assertEquals(spec2.freeRamMb, a2.freeRamMb)
        assertEquals(spec2.load, a2.load)
    }
}