package com.zorroa.irm.studio.controller

import com.zorroa.common.domain.IndexRoute
import com.zorroa.common.util.Json
import com.zorroa.irm.studio.AbstractTest
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.util.*
import kotlin.test.assertNotNull
import kotlin.test.assertNull


@WebAppConfiguration
class IndexRouteControllerTests : AbstractTest() {

    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var wac: WebApplicationContext

    @Before
    fun setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(wac).build()
    }

    @Test
    fun testGetEsHome() {
        val id = UUID.randomUUID()
        val result = mockMvc.perform(get("/api/v1/index-routes/${id}")
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk)
                .andReturn()

        val home = Json.deserialize(result.response.contentAsByteArray, IndexRoute::class.java)
        assertNotNull(home.clusterUrl)
        assertNotNull(home.indexName)
        assertNull(home.routingKey)
    }

}
