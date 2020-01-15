package com.zorroa.archivist.rest

import com.zorroa.archivist.MockMvcTest
import com.zorroa.archivist.domain.DataSourceCredentials
import com.zorroa.archivist.domain.DataSourceFilter
import com.zorroa.archivist.domain.DataSourceSpec
import com.zorroa.archivist.domain.DataSourceUpdate
import com.zorroa.archivist.service.DataSourceService
import com.zorroa.archivist.util.Json
import org.hamcrest.CoreMatchers
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

class DataSourceControllerTests : MockMvcTest() {

    val testSpec = DataSourceSpec(
        "Testing 123",
        "gs://foo-bar"
    )

    @Autowired
    lateinit var dataSourceService: DataSourceService

    @Test
    fun testCreate() {
        mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/data-sources")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(testSpec))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.uri", CoreMatchers.equalTo(testSpec.uri)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.name", CoreMatchers.equalTo(testSpec.name)))
            .andReturn()
    }

    @Test
    fun testUpdate() {
        val ds = dataSourceService.create(testSpec)
        val update = DataSourceUpdate("spock", "gs://foo/bar", ds.fileTypes, ds.pipelineId)
        mvc.perform(
            MockMvcRequestBuilders.put("/api/v1/data-sources/${ds.id}")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(update))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.uri", CoreMatchers.equalTo(update.uri)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.name", CoreMatchers.equalTo(update.name)))
            .andReturn()
    }

    @Test
    fun testDelete() {
        val ds = dataSourceService.create(testSpec)
        mvc.perform(
            MockMvcRequestBuilders.delete("/api/v1/data-sources/${ds.id}")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
    }
    @Test
    fun testGet() {
        val ds = dataSourceService.create(testSpec)

        mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/data-sources/${ds.id}")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.uri", CoreMatchers.equalTo(testSpec.uri)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.name", CoreMatchers.equalTo(testSpec.name)))
            .andReturn()
    }

    @Test
    fun testFindOne() {
        val ds = dataSourceService.create(testSpec)
        val filter = DataSourceFilter(ids=listOf(ds.id))

        mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/data-sources/_findOne")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(filter))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.uri", CoreMatchers.equalTo(testSpec.uri)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.name", CoreMatchers.equalTo(testSpec.name)))
            .andReturn()
    }


    @Test
    fun testFind() {
        val ds = dataSourceService.create(testSpec)
        val filter = DataSourceFilter(ids=listOf(ds.id))

        mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/data-sources/_search")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(filter))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.list[0].uri", CoreMatchers.equalTo(testSpec.uri)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.list[0].name", CoreMatchers.equalTo(testSpec.name)))
            .andReturn()
    }

    @Test
    fun testImportAssets() {
        val ds = dataSourceService.create(testSpec)

        mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/data-sources/${ds.id}/_import")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.dataSourceId", CoreMatchers.equalTo(ds.id.toString())))
            .andReturn()
    }

    @Test
    fun testUpdateCredentials() {
        val ds = dataSourceService.create(testSpec)
        val creds = DataSourceCredentials(blob="YAY")

        mvc.perform(
            MockMvcRequestBuilders.put("/api/v1/data-sources/${ds.id}/_credentials")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(creds))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.type", CoreMatchers.equalTo("DATASOURCE")))
            .andExpect(MockMvcResultMatchers.jsonPath("$.id", CoreMatchers.equalTo(ds.id.toString())))
            .andReturn()
    }

    @Test
    fun testGetCredentialsFailureAsSuperAdmin() {
        val ds = dataSourceService.create(testSpec)
        dataSourceService.updateCredentials(ds.id, "YAY")

        mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/data-sources/${ds.id}/_credentials")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().is4xxClientError)
            .andReturn()
    }

    @Test
    fun testGetCredentials() {
        val ds = dataSourceService.create(testSpec)
        dataSourceService.updateCredentials(ds.id, "YAY")

        mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/data-sources/${ds.id}/_credentials")
                .headers(job())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.blob", CoreMatchers.equalTo("YAY")))
            .andReturn()
    }
}