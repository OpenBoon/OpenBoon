package com.zorroa.archivist.rest

import com.zorroa.archivist.MockMvcTest
import com.zorroa.archivist.domain.DataSetSpec
import com.zorroa.archivist.domain.DataSetType
import com.zorroa.archivist.service.DataSetService
import com.zorroa.zmlp.util.Json
import org.hamcrest.CoreMatchers
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext

class DataSetControllerTests : MockMvcTest() {

    @PersistenceContext
    lateinit var entityManager: EntityManager

    @Autowired
    lateinit var dataSetService: DataSetService

    val testSpec = DataSetSpec("test-1234", DataSetType.LabelDetection)

    @Test
    fun testCreate() {
        mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/data-sets")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(testSpec))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.type", CoreMatchers.equalTo(testSpec.type.name)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.name", CoreMatchers.equalTo(testSpec.name)))
            .andReturn()
    }

    @Test
    fun testGet() {
        val ds = dataSetService.create(testSpec)
        entityManager.flush()

        mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/data-sets/${ds.id}")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.type", CoreMatchers.equalTo(testSpec.type.name)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.name", CoreMatchers.equalTo(testSpec.name)))
            .andReturn()
    }

    @Test
    fun testSearch() {
        val ds = dataSetService.create(testSpec)
        val body = mapOf(
            "names" to listOf(ds.name),
            "ids" to listOf(ds.id.toString()),
            "types" to listOf(ds.type.toString())
        )

        entityManager.flush()
        val rsp = mvc.perform(
            MockMvcRequestBuilders.post("/api/v3/data-sets/_search")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(body))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.list[0].type", CoreMatchers.equalTo(testSpec.type.name)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.list[0].name", CoreMatchers.equalTo(testSpec.name)))
            .andReturn()

        println(rsp.response.contentAsString)
    }

    @Test
    fun testFindOne() {
        val ds = dataSetService.create(testSpec)
        val body = mapOf(
            "names" to listOf(ds.name),
            "ids" to listOf(ds.id.toString())
        )
        entityManager.flush()
        mvc.perform(
            MockMvcRequestBuilders.post("/api/v3/data-sets/_find_one")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(body))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.type", CoreMatchers.equalTo(testSpec.type.name)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.name", CoreMatchers.equalTo(testSpec.name)))
            .andReturn()
    }

    @Test
    fun testGetLabelCounts() {
        val ds = dataSetService.create(testSpec)

        entityManager.flush()
        mvc.perform(
            MockMvcRequestBuilders.get("/api/v3/data-sets/${ds.id}/_label_counts")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
    }
}
