package boonai.archivist.rest

import boonai.archivist.MockMvcTest
import boonai.archivist.domain.DataSourceDelete
import boonai.archivist.domain.DataSourceFilter
import boonai.archivist.domain.DataSourceSpec
import boonai.archivist.domain.DataSourceUpdate
import boonai.archivist.service.CredentialsService
import boonai.archivist.service.DataSourceService
import boonai.common.util.Json
import org.hamcrest.CoreMatchers
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext

class DataSourceControllerTests : MockMvcTest() {

    val testSpec = DataSourceSpec(
        "Testing 123",
        "gs://foo-bar"
    )

    @Autowired
    lateinit var dataSourceService: DataSourceService

    @Autowired
    lateinit var credentialsService: CredentialsService

    @PersistenceContext
    lateinit var entityManager: EntityManager

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
        val update = DataSourceUpdate("spock", "gs://foo/bar", listOf("images"), setOf(), setOf())
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
        val delete = DataSourceDelete(deleteAssets = true)
        mvc.perform(
            MockMvcRequestBuilders.delete("/api/v1/data-sources/${ds.id}")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(delete))
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
        val filter = DataSourceFilter(ids = listOf(ds.id))

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
        val filter = DataSourceFilter(ids = listOf(ds.id))

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
}
