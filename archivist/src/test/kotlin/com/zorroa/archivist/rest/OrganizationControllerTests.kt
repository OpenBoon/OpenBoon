package com.zorroa.archivist.rest

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.readValue
import com.zorroa.archivist.domain.Organization
import com.zorroa.archivist.domain.OrganizationFilter
import com.zorroa.archivist.domain.OrganizationSpec
import com.zorroa.archivist.domain.OrganizationUpdateSpec
import com.zorroa.archivist.service.OrganizationService
import com.zorroa.common.repository.KPagedList
import com.zorroa.common.util.Json
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpSession
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import kotlin.test.assertEquals

class OrganizationControllerTests: MockMvcTest() {

    private val organizationName = "Wendys"
    private val organizationSpec = OrganizationSpec(organizationName)

    internal lateinit var session: MockHttpSession

    @Before
    fun init() {
        session = admin()
    }

    @Test
    fun testCreate() {
        val result = mvc.perform(MockMvcRequestBuilders.post("/api/v1/organizations")
                .session(session)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(organizationSpec)))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()
        val org = Json.Mapper.readValue<Organization>(result.response.contentAsString, Organization::class.java)
        assertEquals(organizationName, org.name)
    }

    @Test
    fun testGet() {
        val org = organizationService.create(organizationSpec)
        val rsp = mvc.perform(MockMvcRequestBuilders.get("/api/v1/organizations/${org.id}")
                .session(session)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()
        val result = Json.Mapper.readValue<Organization>(rsp.response.contentAsString, Organization::class.java)
        assertEquals(organizationName, result.name)
    }

    @Test
    fun testSearch() {
        organizationService.create(organizationSpec)
        val filter = OrganizationFilter(names=listOf(organizationName))

        val rsp = mvc.perform(MockMvcRequestBuilders.post("/api/v1/organizations/_search")
                .session(session)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(filter)))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()
        val result = Json.Mapper.readValue<KPagedList<Organization>>(rsp.response.contentAsString)
        assertEquals(organizationName, result[0].name)
    }

    @Test
    fun testFindOne() {
        organizationService.create(organizationSpec)
        val filter = OrganizationFilter(names=listOf(organizationName))

        val rsp = mvc.perform(MockMvcRequestBuilders.post("/api/v1/organizations/_findOne")
                .session(session)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(filter)))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()
        val result = Json.Mapper.readValue<Organization>(rsp.response.contentAsString)
        assertEquals(organizationName, result.name)
    }

    @Test
    fun testUpdate() {
        val org = organizationService.create(organizationSpec)
        val update = OrganizationUpdateSpec(name="bob_dole")

        val rsp = mvc.perform(MockMvcRequestBuilders.put("/api/v1/organizations/${org.id}")
                .session(session)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(update)))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()
        val result = Json.Mapper.readValue<Map<String,Any>>(rsp.response.contentAsString)
        assertEquals(true, result["success"] as Boolean)
    }
}
