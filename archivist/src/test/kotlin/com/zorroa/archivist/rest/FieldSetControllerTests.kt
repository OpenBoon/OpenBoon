package com.zorroa.archivist.rest

import com.fasterxml.jackson.module.kotlin.readValue
import com.zorroa.archivist.domain.FieldSet
import com.zorroa.archivist.domain.FieldSetFilter
import com.zorroa.archivist.domain.FieldSetSpec
import com.zorroa.common.repository.KPagedList
import com.zorroa.common.util.Json
import org.junit.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FieldSetControllerTests : MockMvcTest() {

    @Test
    fun testCreate() {
        val spec = FieldSetSpec("Potatos")
        resultForPostContent<FieldSet>(
            "/api/v1/fieldSets",
            spec)
        val allFieldSets = fieldSystemService.getAllFieldSets()
        assertEquals(1, allFieldSets.count { it.name == spec.name })
    }

    @Test
    fun testGet() {
        setupEmbeddedFieldSets()
        val allFieldSets = fieldSystemService.getAllFieldSets()

        val result = mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/fieldSets/{id}", allFieldSets[0].id)
                .session(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
        val fieldSet = Json.Mapper.readValue<FieldSet>(result.response.contentAsString)
        assertEquals(allFieldSets[0].id, fieldSet.id)
    }

    @Test
    fun testSearch() {
        setupEmbeddedFieldSets()
        val fieldSets = resultForPostContent<KPagedList<FieldSet>>(
            "/api/v1/fieldSets/_search",
            FieldSetFilter())
        assertTrue(fieldSets.size() > 0)
    }

    @Test
    fun testFindOne() {
        setupEmbeddedFieldSets()
        val allFieldSets = fieldSystemService.getAllFieldSets()
        val fieldSet = resultForPostContent<FieldSet>(
            "/api/v1/fieldSets/_findOne",
            FieldSetFilter(names = listOf(allFieldSets[0].name)))
        assertEquals(allFieldSets[0].id, fieldSet.id)
    }
}
