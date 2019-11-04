package com.zorroa.archivist.rest

import com.fasterxml.jackson.module.kotlin.readValue
import com.zorroa.archivist.domain.IndexMappingVersion
import com.zorroa.archivist.domain.IndexMigrationSpec
import com.zorroa.archivist.domain.IndexRoute
import com.zorroa.archivist.domain.IndexRouteSpec
import com.zorroa.archivist.domain.PipelineType
import com.zorroa.common.domain.Job
import com.zorroa.common.domain.JobState
import com.zorroa.common.util.Json
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest
import org.elasticsearch.client.RequestOptions
import org.hamcrest.CoreMatchers
import org.junit.After
import org.junit.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IndexRoutingControllerTests : MockMvcTest() {

    override fun requiresElasticSearch(): Boolean {
        return true
    }

    @After
    fun after() {

        val route = indexRoutingService.getIndexRoute(
            UUID.fromString("00000000-0000-0000-0000-000000000000")
        )
        val rest = indexRoutingService.getClusterRestClient(route)

        // Clear out test indexes.  Could be more in future.
        listOf("testing123").forEach {
            try {
                val reqDel = DeleteIndexRequest(it)
                rest.client.indices().delete(reqDel, RequestOptions.DEFAULT)
            } catch (e: Exception) {
                logger.warn("Failed to delete '$it' index, this is usually ok.")
            }
        }
    }

    @Test
    fun testCreate() {
        val spec = IndexRouteSpec(
            "http://localhost:9200",
            "testing123",
            "test",
            1
        )

        val rsp = mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/index-routes")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(spec))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val result = Json.Mapper.readValue<IndexRoute>(rsp.response.contentAsString)

        assertEquals("http://localhost:9200", result.clusterUrl)
        assertEquals("testing123", result.indexName)
        assertEquals("test", result.mapping)
        assertEquals(1, result.mappingMajorVer)
        assertEquals(2, result.replicas)
        assertEquals(5, result.shards)
    }

    @Test
    fun testGet() {
        val spec = IndexRouteSpec(
            "http://localhost:9200",
            "testing123",
            "test",
            1
        )

        val route = indexRoutingService.createIndexRoute(spec)

        val rsp = mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/index-routes/${route.id}")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val result = Json.Mapper.readValue<IndexRoute>(rsp.response.contentAsString)

        assertEquals("http://localhost:9200", result.clusterUrl)
        assertEquals("testing123", result.indexName)
        assertEquals("test", result.mapping)
        assertEquals(1, result.mappingMajorVer)
        assertEquals(2, result.replicas)
        assertEquals(5, result.shards)
    }

    @Test
    fun testGetState() {
        val spec = IndexRouteSpec(
            "http://localhost:9200",
            "testing123",
            "test",
            1
        )

        val route = indexRoutingService.createIndexRoute(spec)
        mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/index-routes/${route.id}/_state")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.health", CoreMatchers.equalTo("yellow")))
            .andExpect(jsonPath("$.status", CoreMatchers.equalTo("open")))
            .andReturn()
    }

    @Test
    fun testOpenIndex() {
        val spec = IndexRouteSpec(
            "http://localhost:9200",
            "testing123",
            "test",
            1
        )

        val route = indexRoutingService.createIndexRoute(spec)
        indexRoutingService.closeIndex(route)

        mvc.perform(
            MockMvcRequestBuilders.put("/api/v1/index-routes/${route.id}/_open")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.object.closed", CoreMatchers.equalTo(false)))
            .andReturn()
    }

    @Test
    fun testCloseIndex() {
        val spec = IndexRouteSpec(
            "http://localhost:9200",
            "testing123",
            "test",
            1
        )

        val route = indexRoutingService.createIndexRoute(spec)
        mvc.perform(
            MockMvcRequestBuilders.put("/api/v1/index-routes/${route.id}/_close")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.object.closed", CoreMatchers.equalTo(true)))
            .andReturn()
    }

    @Test
    fun testDeleteIndex() {
        val spec = IndexRouteSpec(
            "http://localhost:9200",
            "testing123",
            "test",
            1
        )

        val route = indexRoutingService.createIndexRoute(spec)
        indexRoutingService.closeIndex(route)

        mvc.perform(
            MockMvcRequestBuilders.delete("/api/v1/index-routes/${route.id}")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
    }

    @Test
    fun testGetMappings() {

        val rsp = mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/index-routes/_mappings")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val result = Json.Mapper.readValue<List<IndexMappingVersion>>(rsp.response.contentAsString)
        val mapping = result[0]
        assertEquals("test", mapping.mapping)
        assertEquals(1, mapping.mappingMajorVer)
    }

    @Test
    fun testCreateMigration() {

        val spec = IndexRouteSpec(
            "http://localhost:9200",
            "testing123",
            "test",
            1
        )

        val route = indexRoutingService.createIndexRoute(spec)
        val mspec = IndexMigrationSpec(route.id, true)

        val rsp = mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/index-routes/_migrate")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(mspec))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val result = Json.Mapper.readValue<Job>(rsp.response.contentAsString)
        assertEquals(PipelineType.Batch, result.type)
        assertTrue(result.name.startsWith("migration"))
        assertEquals(JobState.Active, result.state)
    }
}