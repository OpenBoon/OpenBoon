package com.zorroa.archivist.web

import com.fasterxml.jackson.core.type.TypeReference
import com.zorroa.archivist.domain.AuditLogFilter
import com.zorroa.archivist.domain.AuditLogType
import com.zorroa.archivist.repository.AnalystDao
import com.zorroa.common.domain.Analyst
import com.zorroa.common.domain.AnalystFilter
import com.zorroa.common.domain.AnalystSpec
import com.zorroa.common.domain.AnalystState
import com.zorroa.common.repository.KPagedList
import com.zorroa.common.util.Json
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import kotlin.test.assertEquals

class AnalystControllerTests : MockMvcTest() {

    @Autowired
    lateinit var analystDao: AnalystDao

    lateinit var analyst: Analyst
    lateinit var spec: AnalystSpec

    @Before
    fun init() {
        authenticateAsAnalyst()
        spec = AnalystSpec(
                1024,
                648,
                1024,
                0.5f,
                null)
        analyst = analystDao.create(spec)
    }

    @Test
    @Throws(Exception::class)
    fun testSearch() {
        // All filter options tested in DAO.
        val session = admin()
        val filter = AnalystFilter(states=listOf(AnalystState.Up))

        val rsp = mvc.perform(MockMvcRequestBuilders.post("/api/v1/analysts/_search")
                .session(session)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .content(Json.serialize(filter))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

        val list = Json.Mapper.readValue<KPagedList<Analyst>>(rsp.response.contentAsString,
                object : TypeReference<KPagedList<Analyst>>() {})
        assertEquals(1, list.size())
    }

    @Test
    fun testGet() {
        val session = admin()

        val rsp = mvc.perform(MockMvcRequestBuilders.get("/api/v1/analysts/${analyst.id}")
                .session(session)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

        val analyst2 = Json.Mapper.readValue<Analyst>(rsp.response.contentAsString, Analyst::class.java)
        assertEquals(analyst.endpoint, analyst2.endpoint)
        assertEquals(analyst.id, analyst2.id)
        assertEquals(analyst.state, analyst2.state)
    }
}