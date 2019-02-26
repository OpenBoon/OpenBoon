package com.zorroa.archivist.rest

import com.fasterxml.jackson.module.kotlin.readValue
import com.zorroa.archivist.domain.AttrType
import com.zorroa.archivist.domain.Field
import com.zorroa.archivist.domain.FieldFilter
import com.zorroa.archivist.domain.FieldSpec
import com.zorroa.common.repository.KPagedList
import com.zorroa.common.util.Json
import org.junit.Test
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import kotlin.test.assertEquals
import kotlin.test.assertTrue


class FieldControllerTests : MockMvcTest() {

    @Test
    fun testCreateField() {
        val spec = FieldSpec("Media Clip Parent", "media.clip.parent", null,false)
        val session = admin()

        val req = mvc.perform(MockMvcRequestBuilders.post("/api/v1/fields")
                .session(session)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(spec)))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

        val field = Json.Mapper.readValue<Field>(req.response.contentAsString, Field::class.java)
        assertEquals(spec.name, field.name)
        assertEquals(AttrType.StringExact, field.attrType)
        assertEquals(spec.attrName, field.attrName)
    }

    @Test
    fun testDeleteField() {
        val spec = FieldSpec("Media Clip Parent", "media.clip.parent", null,false)
        val field = fieldSystemService.createField(spec)
        val session = admin()

        val req = mvc.perform(MockMvcRequestBuilders.delete("/api/v1/fields/${field.id}")
                .session(session)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(spec)))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

        val rsp = Json.Mapper.readValue<Map<String, Any>>(req.response.contentAsString)
        assertTrue(rsp["success"] as Boolean)
    }


    @Test
    fun testGet() {
        val spec = FieldSpec("Media Clip Parent", "media.clip.parent", null,false)
        val field = fieldSystemService.createField(spec)

        val session = admin()
        val req = mvc.perform(MockMvcRequestBuilders.get("/api/v1/fields/${field.id}")
                .session(session)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

        val result = Json.Mapper.readValue<Field>(req.response.contentAsString, Field::class.java)
        assertEquals(field.id, result.id)
    }

    @Test
    fun testSearch() {
        val spec = FieldSpec("Media Clip Parent", "media.clip.parent", null,false)
        val field = fieldSystemService.createField(spec)

        val filter = FieldFilter(ids=listOf(field.id))

        val session = admin()
        val req = mvc.perform(MockMvcRequestBuilders.post("/api/v1/fields/_search")
                .session(session)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(filter)))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

        val result = Json.Mapper.readValue<KPagedList<Field>>(
                req.response.contentAsString, Field.Companion.TypeRefKList)
        assertEquals(1, result.size())
        assertEquals(field.id, result[0].id)
    }
}