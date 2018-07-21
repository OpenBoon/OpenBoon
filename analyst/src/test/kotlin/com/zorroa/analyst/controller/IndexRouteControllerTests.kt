package com.zorroa.analyst.controller

import com.zorroa.analyst.AbstractMvcTest
import com.zorroa.common.domain.IndexRoute
import com.zorroa.common.util.Json
import org.junit.Test
import org.springframework.http.MediaType
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.*
import kotlin.test.assertNotNull
import kotlin.test.assertNull


@WebAppConfiguration
class IndexRouteControllerTests : AbstractMvcTest() {

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
