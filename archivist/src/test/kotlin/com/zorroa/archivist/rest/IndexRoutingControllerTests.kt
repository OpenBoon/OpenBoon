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
import org.junit.Before
import org.junit.Test
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpSession
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IndexRoutingControllerTests : MockMvcTest() {

    internal lateinit var session: MockHttpSession

    override fun requiresElasticSearch(): Boolean {
        return true
    }

    @Before
    fun init() {
        session = admin()
    }

    @Test
    fun testCreate() {
        val spec = IndexRouteSpec(
            "http://localhost:9200",
            "testing123",
            "test",
            1,
            false
        )

        val rsp = mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/index-routes")
                .session(session)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
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
        assertEquals(false, result.defaultPool)
        assertEquals(false, result.useRouteKey)
        assertEquals(2, result.replicas)
        assertEquals(5, result.shards)
    }

    @Test
    fun testGet() {
        val spec = IndexRouteSpec(
            "http://localhost:9200",
            "testing123",
            "test",
            1,
            false
        )

        val route = indexRoutingService.createIndexRoute(spec)

        val rsp = mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/index-routes/${route.id}")
                .session(session)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val result = Json.Mapper.readValue<IndexRoute>(rsp.response.contentAsString)

        assertEquals("http://localhost:9200", result.clusterUrl)
        assertEquals("testing123", result.indexName)
        assertEquals("test", result.mapping)
        assertEquals(1, result.mappingMajorVer)
        assertEquals(false, result.defaultPool)
        assertEquals(false, result.useRouteKey)
        assertEquals(2, result.replicas)
        assertEquals(5, result.shards)
    }

    @Test
    fun testGetMappings() {

        val rsp = mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/index-routes/_mappings")
                .session(session)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
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
            1,
            false
        )

        val route = indexRoutingService.createIndexRoute(spec)
        val mspec = IndexMigrationSpec(route.id, true)

        val rsp = mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/index-routes/_migrate")
                .session(session)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
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