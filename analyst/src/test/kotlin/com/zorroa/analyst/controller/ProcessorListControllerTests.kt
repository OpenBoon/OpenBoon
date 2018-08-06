package com.zorroa.analyst.controller

import com.fasterxml.jackson.module.kotlin.readValue
import com.zorroa.analyst.AbstractMvcTest
import com.zorroa.common.domain.ProcessorRef
import com.zorroa.common.util.Json
import org.junit.Test
import org.springframework.http.MediaType
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@WebAppConfiguration
class ProcessorListControllerTests : AbstractMvcTest() {

    @Test
    fun testGetDefaultImport() {
        val req = mockMvc.perform(MockMvcRequestBuilders.get(
                "/api/v1/processor-lists/defaults/import")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{}"))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

        val kpl : Map<String, Any> = Json.Mapper.readValue(req.response.contentAsByteArray)
        assertTrue(kpl.containsKey("processors"))
        assertTrue(kpl.containsKey("type"))
        assertEquals("import", kpl["type"])

        val processors : List<ProcessorRef> = Json.Mapper.convertValue(kpl["processors"],Json.LIST_OF_PREFS)
        assertEquals("zplugins.core.document.PySetAttributes", processors[0].className)
    }

    @Test
    fun testGetDefaultExport() {
        val req = mockMvc.perform(MockMvcRequestBuilders.get(
                "/api/v1/processor-lists/defaults/export")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{}"))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

        val kpl : Map<String, Any> = Json.Mapper.readValue(req.response.contentAsByteArray)
        assertTrue(kpl.containsKey("processors"))
        assertTrue(kpl.containsKey("type"))
        assertEquals("export", kpl["type"])

        val processors : List<ProcessorRef> = Json.Mapper.convertValue(kpl["processors"],Json.LIST_OF_PREFS)
        assertEquals("zplugins.core.document.PySetAttributes", processors[0].className)
    }
}
