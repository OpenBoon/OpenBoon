package com.zorroa.archivist.web

import com.fasterxml.jackson.core.type.TypeReference
import com.zorroa.archivist.domain.Organization
import com.zorroa.archivist.domain.OrganizationSpec
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

    @Autowired
    internal lateinit var session: MockHttpSession

    @Before
    @Throws(Exception::class)
    fun init() {
        session = admin()
    }

    @Test
    @Throws(Exception::class)
    fun testCreate() {
        val result = mvc.perform(MockMvcRequestBuilders.post("/api/v1/organizations")
                .session(session)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(organizationSpec)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn()
        val (id, name) = Json.Mapper.readValue<Organization>(result.response.contentAsString,
                object : TypeReference<Organization>() {

                })
        assertEquals(organizationName, name)
    }
}
