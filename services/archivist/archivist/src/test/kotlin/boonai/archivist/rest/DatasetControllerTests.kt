package boonai.archivist.rest

import boonai.archivist.MockMvcTest
import boonai.archivist.domain.AssetSpec
import boonai.archivist.domain.AssetState
import boonai.archivist.domain.BatchCreateAssetsRequest
import boonai.archivist.domain.Dataset
import boonai.archivist.domain.DatasetSpec
import boonai.archivist.domain.DatasetType
import boonai.archivist.domain.DatasetUpdate
import boonai.archivist.domain.UpdateLabelRequest
import boonai.archivist.repository.DatasetDao
import boonai.archivist.service.DatasetService
import boonai.common.util.Json
import org.hamcrest.CoreMatchers
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import kotlin.test.assertEquals

class DatasetControllerTests : MockMvcTest() {

    @Autowired
    lateinit var datasetService: DatasetService

    @Autowired
    lateinit var datasetDao: DatasetDao

    lateinit var dataset: Dataset

    @Before
    fun init() {
        dataset = datasetService.createDataset(DatasetSpec("cats", DatasetType.Classification))
    }

    @Test
    fun testCreate() {

        val mspec = DatasetSpec(
            "test",
            DatasetType.Classification
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

        val mspec = DatasetSpec(
            "test",
            DatasetType.Classification
        )

        val ds = datasetService.createDataset(mspec)

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

        val mspec = DatasetSpec(
            "test",
            DatasetType.Classification
        )

        val ds = datasetService.createDataset(mspec)

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
    fun testUpdate() {
        val mspec = DatasetSpec(
            "test",
            DatasetType.Classification
        )
        val ds = datasetService.createDataset(mspec)
        val update = DatasetUpdate(name = "updated name", description = "updated description")

        mvc.perform(
            MockMvcRequestBuilders.put("/api/v3/datasets/${ds.id}")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(update))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val updated = datasetDao.findById(ds.id).get()
        assertEquals("updated name", updated.name)
        assertEquals("updated description", updated.description)
    }

    @Test
    fun testRenameLabel() {
        val specs = makeDataSet(dataset)
        assetService.batchCreate(
            BatchCreateAssetsRequest(specs)
        )

        val body = UpdateLabelRequest("ant", "horse")

        mvc.perform(
            MockMvcRequestBuilders.put("/api/v3/datasets/${dataset.id}/labels")
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
        val specs = makeDataSet(dataset)
        assetService.batchCreate(
            BatchCreateAssetsRequest(specs, state = AssetState.Analyzed)
        )

        val body = UpdateLabelRequest("ant", "")

        mvc.perform(
            MockMvcRequestBuilders.put("/api/v3/datasets/${dataset.id}/labels")
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
        val labels = datasetService.getLabelCounts(dataset)
        assertEquals(null, labels["ant"])
        assertEquals(3, labels.size)
    }

    @Test
    fun testFindOne() {
        val filter =
            """
            {
                "names": ["${dataset.name}"],
                "ids": ["${dataset.id}"]
            }
            """
        mvc.perform(
            MockMvcRequestBuilders.post("/api/v3/datasets/_find_one")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(filter)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.type", CoreMatchers.equalTo(dataset.type.name)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.name", CoreMatchers.equalTo(dataset.name)))
            .andReturn()
    }

    @Test
    fun testSearch() {

        val filter =
            """
            {
                "names": ["${dataset.name}"],
                "ids": ["${dataset.id}"]
            }
            """
        mvc.perform(
            MockMvcRequestBuilders.post("/api/v3/datasets/_search")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(filter)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.list[0].type", CoreMatchers.equalTo(dataset.type.name)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.list[0].name", CoreMatchers.equalTo(dataset.name)))
            .andReturn()
    }

    @Test
    fun testGetTypes() {
        mvc.perform(
            MockMvcRequestBuilders.get("/api/v3/datasets/_types")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                    "$[0].name",
                    CoreMatchers.equalTo("Classification")
                )
            )
            .andReturn()
    }

    @Test
    fun testGetLabelCountsV4() {

        val specs = makeDataSet(dataset)
        assetService.batchCreate(
            BatchCreateAssetsRequest(specs, state = AssetState.Analyzed)
        )

        val rsp = mvc.perform(
            MockMvcRequestBuilders.get("/api/v4/datasets/${dataset.id}/_label_counts")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                    "$.dog.TRAIN",
                    CoreMatchers.equalTo(1)
                )
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                    "$.dog.TEST",
                    CoreMatchers.equalTo(0)
                )
            )
            .andReturn()

        logger.info(rsp.response.contentAsString)
    }

    fun makeDataSet(ds: Dataset): List<AssetSpec> {
        return listOf(
            AssetSpec("https://i.imgur.com/12abc.jpg", label = ds.makeLabel("beaver")),
            AssetSpec("https://i.imgur.com/abc123.jpg", label = ds.makeLabel("ant")),
            AssetSpec("https://i.imgur.com/horse.jpg", label = ds.makeLabel("horse")),
            AssetSpec("https://i.imgur.com/zani.jpg", label = ds.makeLabel("dog"))
        )
    }
}
