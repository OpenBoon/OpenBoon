package com.zorroa.archivist.rest

import com.fasterxml.jackson.module.kotlin.readValue
import com.zorroa.archivist.MockMvcTest
import com.zorroa.archivist.domain.IndexRoute
import com.zorroa.archivist.domain.IndexRouteSpec
import com.zorroa.archivist.domain.IndexRouteState
import com.zorroa.zmlp.util.Json
import org.hamcrest.CoreMatchers
import org.junit.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import kotlin.test.assertEquals

class IndexRoutingControllerTests : MockMvcTest() {

    @Value("\${archivist.es.url}")
    lateinit var esUrl: String

    override fun requiresElasticSearch(): Boolean {
        return true
    }

    val testSpec = IndexRouteSpec(
        "test", 1, state = IndexRouteState.BUILDING
    )

    @Test
    fun testCreate() {
        val rsp = mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/index-routes")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(testSpec))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val result = Json.Mapper.readValue<IndexRoute>(rsp.response.contentAsString)

        assertEquals(esUrl, result.clusterUrl)
        assertEquals("test", result.mapping)
        assertEquals(1, result.majorVer)
        assertEquals(0, result.replicas)
        assertEquals(2, result.shards)
    }

    @Test
    fun testGet() {
        val route = indexRoutingService.createIndexRoute(testSpec)
        val rsp = mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/index-routes/${route.id}")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val result = Json.Mapper.readValue<IndexRoute>(rsp.response.contentAsString)

        assertEquals(esUrl, result.clusterUrl)
        assertEquals("test", result.mapping)
        assertEquals(1, result.majorVer)
        assertEquals(0, result.replicas)
        assertEquals(2, result.shards)
    }

    @Test
    fun testGetAttrs() {
        val route = indexRoutingService.createIndexRoute(testSpec)
        mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/index-routes/${route.id}/_attrs")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.health", CoreMatchers.equalTo("green")))
            .andExpect(jsonPath("$.status", CoreMatchers.equalTo("open")))
            .andReturn()
    }
}