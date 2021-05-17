package boonai.archivist.rest

import boonai.archivist.MockMvcTest
import boonai.archivist.domain.AssetSpec
import boonai.archivist.domain.BatchCreateAssetsRequest
import boonai.archivist.domain.DataSet
import boonai.archivist.domain.DataSetSpec
import boonai.archivist.domain.DataSetType
import boonai.archivist.domain.UpdateLabelRequest
import boonai.archivist.service.DataSetService
import boonai.common.util.Json
import org.hamcrest.CoreMatchers
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import kotlin.test.assertEquals

class DataSetControllerTests : MockMvcTest() {

    @Autowired
    lateinit var dataSetService: DataSetService

    lateinit var dataSet: DataSet

    @Before
    fun init() {
        dataSet = dataSetService.createDataSet(DataSetSpec("cats", DataSetType.Classification))
    }

    @Test
    fun testCreate() {

        val mspec = DataSetSpec(
            "test",
            DataSetType.Classification
        )

        mvc.perform(
            MockMvcRequestBuilders.post("/api/v3/datasets")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(mspec))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.type", CoreMatchers.equalTo(mspec.type.name)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.name", CoreMatchers.equalTo(mspec.name)))
            .andReturn()
    }

    @Test
    fun testGet() {

        val mspec = DataSetSpec(
            "test",
            DataSetType.Classification
        )

        val ds = dataSetService.createDataSet(mspec)

        mvc.perform(
            MockMvcRequestBuilders.get("/api/v3/datasets/${ds.id}")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.type", CoreMatchers.equalTo(mspec.type.name)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.name", CoreMatchers.equalTo(mspec.name)))
            .andReturn()
    }

    @Test
    fun testDelete() {

        val mspec = DataSetSpec(
            "test",
            DataSetType.Classification
        )

        val ds = dataSetService.createDataSet(mspec)

        mvc.perform(
            MockMvcRequestBuilders.delete("/api/v3/datasets/${ds.id}")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.success", CoreMatchers.equalTo(true)))
            .andReturn()
    }

    @Test
    fun testRenameLabel() {
        val specs = dataSet(dataSet)
        assetService.batchCreate(
            BatchCreateAssetsRequest(specs)
        )

        val body = UpdateLabelRequest("ant", "horse")

        mvc.perform(
            MockMvcRequestBuilders.put("/api/v3/datasets/${dataSet.id}/labels")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(body))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                    "$.updated",
                    CoreMatchers.equalTo(1)
                )
            )
            .andReturn()
    }

    @Test
    fun testDeleteLabel() {
        val specs = dataSet(dataSet)
        assetService.batchCreate(
            BatchCreateAssetsRequest(specs)
        )

        val body = UpdateLabelRequest("ant", "")

        mvc.perform(
            MockMvcRequestBuilders.put("/api/v3/models/${dataSet.id}/labels")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(body))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                    "$.updated",
                    CoreMatchers.equalTo(1)
                )
            )
            .andReturn()

        authenticate()
        val labels = dataSetService.getLabelCounts(dataSet)
        assertEquals(null, labels["ant"])
        assertEquals(3, labels.size)
    }

    fun dataSet(ds: DataSet): List<AssetSpec> {
        return listOf(
            AssetSpec("https://i.imgur.com/12abc.jpg", label = ds.makeLabel("beaver")),
            AssetSpec("https://i.imgur.com/abc123.jpg", label = ds.makeLabel("ant")),
            AssetSpec("https://i.imgur.com/horse.jpg", label = ds.makeLabel("horse")),
            AssetSpec("https://i.imgur.com/zani.jpg", label = ds.makeLabel("dog"))
        )
    }
}
