package com.zorroa.archivist.rest

import com.fasterxml.jackson.core.type.TypeReference
import com.zorroa.archivist.domain.Permission
import com.zorroa.archivist.domain.PermissionSpec
import com.zorroa.common.util.Json
import org.junit.Test
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors

import org.junit.Assert.assertEquals
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * Created by chambers on 10/28/15.
 */
class PermissionContollerTests : MockMvcTest() {

    @Test
    @Throws(Exception::class)
    fun testCreate() {
        val b = PermissionSpec("project", "sw")
        b.description = "Star Wars crew members"

        val session = admin()
        val result = mvc.perform(post("/api/v1/permissions")
                .session(session)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .content(Json.serialize(b))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk)
                .andReturn()

        val p = Json.deserialize(result.response.contentAsByteArray, Permission::class.java)
        assertEquals(b.description, p.description)
        assertEquals(b.name, p.name)
    }

    @Test
    @Throws(Exception::class)
    fun testGetAll() {

        val session = admin()
        val result = mvc.perform(get("/api/v1/permissions")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk)
                .andReturn()

        val perms1 = Json.Mapper.readValue<List<Permission>>(result.response.contentAsByteArray,
                object : TypeReference<List<Permission>>() {

                })
        val perms2 = permissionService.getPermissions()
        assertEquals(perms1, perms2)
    }

    @Test
    @Throws(Exception::class)
    fun testGet() {

        val b = PermissionSpec("project", "sw")
        b.name = "project::sw"
        b.description = "Star Wars crew members"
        val perm = permissionService.createPermission(b)

        val session = admin()
        val result = mvc.perform(get("/api/v1/permissions/" + perm.id)
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk)
                .andReturn()

        val perm1 = Json.deserialize(result.response.contentAsByteArray, Permission::class.java)
        assertEquals(perm, perm1)
    }

    @Test
    @Throws(Exception::class)
    fun testFind() {

        val b = PermissionSpec("project", "sw")
        b.description = "Star Wars crew members"
        val perm = permissionService.createPermission(b)

        val session = admin()
        val result = mvc.perform(post("/api/v1/permissions/_findOne")
                .session(session)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .content(Json.serialize(mapOf("authorities" to listOf("project::sw"))))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk)
                .andReturn()

        val perm1 = Json.deserialize(result.response.contentAsByteArray, Permission::class.java)
        assertEquals(perm, perm1)
    }

    @Test
    @Throws(Exception::class)
    fun testFoo() {

        val b = PermissionSpec("project", "sw")
        b.description = "Star Wars crew members"
        val perm = permissionService.createPermission(b)

        val session = admin()
        val result = mvc.perform(get("/api/v1/permissions/" + perm.id)
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk)
                .andReturn()

        val perm1 = Json.deserialize(result.response.contentAsByteArray, Permission::class.java)
        assertEquals(perm, perm1)
    }
}
