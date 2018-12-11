package com.zorroa.archivist.rest

import com.google.common.collect.ImmutableMap
import com.zorroa.archivist.domain.SettingsFilter
import com.zorroa.archivist.service.SettingsService
import com.zorroa.common.util.Json
import org.junit.Test
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors

import org.junit.Assert.*
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * Created by chambers on 7/7/17.
 */
class SettingsControllerTests : MockMvcTest() {

    @Test
    @Throws(Exception::class)
    fun testGetAllNoFilter() {
        val session = admin()

        val result = mvc.perform(get("/api/v1/settings")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk)
                .andReturn()

        val t = deserialize(result, SettingsService.ListOfSettingsType)
        assertFalse(t.isEmpty())
    }

    @Test
    @Throws(Exception::class)
    fun testGetAllFilter() {
        val session = admin()
        val filter = SettingsFilter()
        filter.count = 5
        val result = mvc.perform(get("/api/v1/settings")
                .session(session)
                .content(Json.serialize(filter))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk)
                .andReturn()

        val t = deserialize(result, SettingsService.ListOfSettingsType)
        assertEquals(5, t.size.toLong())
    }

    @Test
    @Throws(Exception::class)
    fun testSet() {
        val session = admin()

        val settings = ImmutableMap.of(
                "curator.thumbnails.drag-template", "bob")

        val result = mvc.perform(put("/api/v1/settings/")
                .session(session)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .content(Json.serialize(settings))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk)
                .andReturn()


        val map = deserialize(result, Json.GENERIC_MAP)
        assertTrue(map["success"] as Boolean)
    }
}
