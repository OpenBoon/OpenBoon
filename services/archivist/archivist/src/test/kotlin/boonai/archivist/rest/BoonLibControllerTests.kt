package boonai.archivist.rest

import boonai.archivist.MockMvcTest
import boonai.archivist.domain.AssetSpec
import boonai.archivist.domain.AssetState
import boonai.archivist.domain.BatchCreateAssetsRequest
import boonai.archivist.domain.BoonLibEntity
import boonai.archivist.domain.BoonLibEntityType
import boonai.archivist.domain.BoonLibSpec
import boonai.archivist.domain.LicenseType
import boonai.archivist.domain.ProjectFileLocator
import boonai.archivist.domain.ProjectStorageCategory
import boonai.archivist.domain.ProjectStorageEntity
import boonai.archivist.domain.ProjectStorageSpec
import boonai.archivist.domain.ProjectToBoonLibCopyRequest
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

class BoonLibControllerTests : MockMvcTest() {

    @Autowired
    lateinit var boonLibService: BoonLibService

    @Autowired
    lateinit var datasetService: DatasetService

    @Autowired
    lateinit var boonLibStorageService: BoonLibStorageService

    @Autowired
    lateinit var projectStorageService: ProjectStorageService

    val spec = BoonLibSpec(
        "Test",
        BoonLibEntity.Dataset,
        BoonLibEntityType.Classification,
        LicenseType.CC0,
        "A test lib"
    )

    @Test
    fun testCreate() {
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
    fun testGet() {
        val lib = boonLibService.createBoonLib(spec)

        mvc.perform(
            MockMvcRequestBuilders.get("/api/v3/boonlibs/${lib.id}")
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
    fun testImport() {
        val lib = boonLibService.createBoonLib(spec)

        val req = BatchCreateAssetsRequest(
            assets = listOf(AssetSpec("gs://cats/large-brown-cat.jpg")),
            state = AssetState.Analyzed
        )

        assetService.batchCreate(req)
        val asset = getSample(1)[0]

        boonLibStorageService.store(
            "boonlib/${lib.id}/abc123/asset.json", 320,
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
        val lib = boonLibService.createBoonLib(spec)
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

        val lib = boonLibService.createBoonLib(spec)
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
}
