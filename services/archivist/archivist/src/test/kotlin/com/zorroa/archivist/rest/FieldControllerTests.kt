package com.zorroa.archivist.rest

import com.zorroa.archivist.MockMvcTest
import com.zorroa.archivist.domain.FieldSpec
import com.zorroa.archivist.service.FieldService
import com.zorroa.zmlp.util.Json
import org.hamcrest.CoreMatchers
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

class FieldControllerTests : MockMvcTest() {

    @Autowired
    lateinit var fieldService: FieldService

    @Test
    fun testMapping() {
        mvc.perform(
            MockMvcRequestBuilders.get("/api/v3/fields/_mapping")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
    }

    @Test
    fun testCreate() {
        val testSpec = FieldSpec("pet_name", "keyword")
        mvc.perform(
            MockMvcRequestBuilders.post("/api/v3/custom-fields")
                .headers(admin())
                .content(Json.serialize(testSpec))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.attrName", CoreMatchers.equalTo("custom.pet_name")))
            .andExpect(MockMvcResultMatchers.jsonPath("$.name", CoreMatchers.equalTo("pet_name")))
            .andReturn()
    }

    @Test
    fun testGet() {
        val field = fieldService.createField(FieldSpec("name", "keyword"))
        mvc.perform(
            MockMvcRequestBuilders.get("/api/v3/custom-fields/${field.id}")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.name", CoreMatchers.equalTo("name")))
            .andExpect(MockMvcResultMatchers.jsonPath("$.attrName", CoreMatchers.equalTo("custom.name")))
            .andExpect(MockMvcResultMatchers.jsonPath("$.id", CoreMatchers.equalTo(field.id.toString())))
            .andExpect(MockMvcResultMatchers.jsonPath("$.timeCreated", CoreMatchers.anything()))
            .andExpect(MockMvcResultMatchers.jsonPath("$.timeModified", CoreMatchers.anything()))
            .andReturn()
    }
}
