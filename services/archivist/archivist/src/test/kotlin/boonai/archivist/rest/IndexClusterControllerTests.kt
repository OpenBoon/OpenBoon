package boonai.archivist.rest

import boonai.archivist.MockMvcTest
import boonai.archivist.domain.IndexClusterSpec
import boonai.common.util.Json
import org.hamcrest.CoreMatchers
import org.junit.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

class IndexClusterControllerTests : MockMvcTest() {

    val testSpec = IndexClusterSpec("http://es2:9200", false)

    @Test
    fun testCreate() {
        mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/index-clusters")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(testSpec))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                    "$.url",
                    CoreMatchers.equalTo("http://es2:9200")
                )
            )
            .andReturn()
    }

    @Test
    fun testGet() {
        val cluster = indexClusterService.createDefaultCluster()
        mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/index-clusters/${cluster.id}")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                    "$.url",
                    CoreMatchers.equalTo(cluster.url)
                )
            )
            .andReturn()
    }

    @Test
    fun testGetAll() {
        val cluster = indexClusterService.createDefaultCluster()
        mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/index-clusters/_search")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                    "$.list[0].url",
                    CoreMatchers.equalTo(cluster.url)
                )
            )
            .andReturn()
    }
}
