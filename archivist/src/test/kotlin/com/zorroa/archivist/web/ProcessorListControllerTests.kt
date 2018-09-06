package com.zorroa.archivist.web

import com.zorroa.common.util.Json
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mock.web.MockHttpSession
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import kotlin.test.assertEquals

@WebAppConfiguration
class ProcessorListControllerTests : MockMvcTest() {

    @Autowired
    internal lateinit var session: MockHttpSession

    @Before
    @Throws(Exception::class)
    fun init() {
        session = admin()
    }

    @Test
    fun testGetDefaultExport() {
        val result = mvc.perform(MockMvcRequestBuilders.get("/api/v1/processor-lists/defaults/export")
                .session(session)
                .content(""))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn()
        val processorList = Json.Mapper.readValue(result.response.contentAsString, Map::class.java)
        assertEquals(processorList["name"], "default-export")
        val processors = processorList["processors"] as ArrayList<*>
        assertEquals(processors.size, 1)
    }
}