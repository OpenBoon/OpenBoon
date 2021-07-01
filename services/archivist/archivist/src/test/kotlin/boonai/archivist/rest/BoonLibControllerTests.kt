package boonai.archivist.rest

import boonai.archivist.MockMvcTest
import boonai.archivist.domain.AssetSpec
import boonai.archivist.domain.AssetState
import boonai.archivist.domain.BatchCreateAssetsRequest
import boonai.archivist.domain.BoonLib
import boonai.archivist.domain.BoonLibEntity
import boonai.archivist.domain.BoonLibFilter
import boonai.archivist.domain.BoonLibSpec
import boonai.archivist.domain.BoonLibState
import boonai.archivist.domain.BoonLibUpdateSpec
import boonai.archivist.domain.DatasetSpec
import boonai.archivist.domain.DatasetType
import boonai.archivist.domain.ProjectFileLocator
import boonai.archivist.domain.ProjectStorageCategory
import boonai.archivist.domain.ProjectStorageEntity
import boonai.archivist.domain.ProjectStorageSpec
import boonai.archivist.domain.ProjectToBoonLibCopyRequest
import boonai.archivist.repository.BoonLibJdbcDao
import boonai.archivist.service.BoonLibService
import boonai.archivist.service.DatasetService
import boonai.archivist.storage.BoonLibStorageService
import boonai.archivist.storage.ProjectStorageService
import boonai.common.util.Json
import org.hamcrest.CoreMatchers
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.io.ByteArrayInputStream
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext

class BoonLibControllerTests : MockMvcTest() {

    @Autowired
    lateinit var boonLibService: BoonLibService

    @Autowired
    lateinit var datasetService: DatasetService

    @Autowired
    lateinit var boonLibStorageService: BoonLibStorageService

    @Autowired
    lateinit var projectStorageService: ProjectStorageService

    @Autowired
    lateinit var boonLibJdbcDao: BoonLibJdbcDao

    @PersistenceContext
    lateinit var entityManager: EntityManager

    @Test
    fun testCreate() {
        val spec = makeBoonLibSpec("Test")
        mvc.perform(
            MockMvcRequestBuilders.post("/api/v3/boonlibs")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(spec))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.description", CoreMatchers.equalTo(spec.description)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.name", CoreMatchers.equalTo(spec.name)))
            .andReturn()
    }

    @Test
    fun testUpdate() {
        val lib = makeBoonLib("Test")
        val spec = BoonLibUpdateSpec(name = "Updated Name", description = "Updated Description")

        mvc.perform(
            MockMvcRequestBuilders.put("/api/v3/boonlibs/${lib.id}")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(spec))
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.description", CoreMatchers.equalTo(spec.description)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.name", CoreMatchers.equalTo(spec.name)))
            .andReturn()
    }

    @Test
    fun testGet() {
        val lib = makeBoonLib("Test")

        mvc.perform(
            MockMvcRequestBuilders.get("/api/v3/boonlibs/${lib.id}")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.description", CoreMatchers.equalTo("foo")))
            .andExpect(MockMvcResultMatchers.jsonPath("$.name", CoreMatchers.equalTo("Test")))
            .andReturn()
    }

    @Test
    fun testImport() {
        val lib = makeBoonLib("Test")

        val req = BatchCreateAssetsRequest(
            assets = listOf(AssetSpec("gs://cats/large-brown-cat.jpg")),
            state = AssetState.Analyzed
        )

        assetService.batchCreate(req)
        val asset = getSample(1)[0]
        asset.setAttr("labels", listOf(mapOf("label" to "dog")))

        boonLibStorageService.store(
            "boonlib/${lib.id}/abc123/asset.json", 347,
            ByteArrayInputStream(Json.serializeToString(asset).toByteArray())
        )

        mvc.perform(
            MockMvcRequestBuilders.post("/api/v3/boonlibs/${lib.id}/_import")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.count", CoreMatchers.equalTo(1)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.tookMillis", CoreMatchers.anything()))
            .andReturn()
    }

    @Test
    fun testUploadFile() {
        job()
        val lib = makeBoonLib("Test")
        // /api/v3/boonlibs/_upload/{libId}/{itemId}/{name}
        val someJson = mapOf("testing" to "123")
        mvc.perform(
            MockMvcRequestBuilders.post("/api/v3/boonlibs/_upload/${lib.id}/foo/bar.json")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(someJson))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.success", CoreMatchers.equalTo(true)))
            .andReturn()
    }

    @Test
    fun testCopyFiles() {

        val loc = ProjectFileLocator(ProjectStorageEntity.ASSETS, "1234", ProjectStorageCategory.SOURCE, "bob.txt")
        val pspec = ProjectStorageSpec(loc, mapOf("cats" to 100), "test".toByteArray())
        val result = projectStorageService.store(pspec)

        val lib = makeBoonLib("Test")
        val req = ProjectToBoonLibCopyRequest(mapOf(result.id to "boonlib/${lib.id}/test/bob.txt"))

        job()

        mvc.perform(
            MockMvcRequestBuilders.post("/api/v3/boonlibs/_copy_from_project")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(req))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.success", CoreMatchers.equalTo(true)))
            .andReturn()
    }

    @Test
    fun testFindOne() {
        val lib = makeBoonLib("Test")
        boonLibJdbcDao.updateBoonLibState(lib.id, BoonLibState.READY)

        val filter = BoonLibFilter(ids = listOf(lib.id))

        mvc.perform(
            MockMvcRequestBuilders.post("/api/v3/boonlibs/_findOne")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(filter))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.description", CoreMatchers.equalTo("foo")))
            .andExpect(MockMvcResultMatchers.jsonPath("$.name", CoreMatchers.equalTo("Test")))
            .andReturn()
    }

    @Test
    fun testSearch() {
        val lib = makeBoonLib("Test")
        boonLibJdbcDao.updateBoonLibState(lib.id, BoonLibState.READY)

        val filter = BoonLibFilter(ids = listOf(lib.id))

        mvc.perform(
            MockMvcRequestBuilders.post("/api/v3/boonlibs/_search")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(filter))
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.list[0].description", CoreMatchers.equalTo("foo")))
            .andExpect(MockMvcResultMatchers.jsonPath("$.list[0].name", CoreMatchers.equalTo("Test")))
            .andExpect(MockMvcResultMatchers.jsonPath("$.list[0].id", CoreMatchers.equalTo(lib.id.toString())))
            .andReturn()
    }

    fun makeBoonLib(name: String): BoonLib {
        val spec = makeBoonLibSpec(name)
        val item = boonLibService.createBoonLib(spec)
        entityManager.flush()
        return item
    }

    fun makeBoonLibSpec(name: String): BoonLibSpec {
        val ds = datasetService.createDataset(DatasetSpec(name, DatasetType.Classification, "foo"))
        val spec = BoonLibSpec(
            name,
            "foo",
            BoonLibEntity.Dataset,
            ds.id
        )
        return spec
    }
}
