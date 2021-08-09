package boonai.archivist.storage

import boonai.archivist.AbstractTest
import boonai.archivist.domain.AssetSpec
import boonai.archivist.domain.AssetState
import boonai.archivist.domain.BatchCreateAssetsRequest
import boonai.archivist.domain.BoonLibSpec
import boonai.archivist.domain.BoonLibEntity
import boonai.archivist.domain.DatasetSpec
import boonai.archivist.domain.DatasetType
import boonai.archivist.domain.ProjectFileLocator
import boonai.archivist.domain.ProjectStorageCategory
import boonai.archivist.domain.ProjectStorageEntity
import boonai.archivist.domain.ProjectStorageSpec
import boonai.archivist.service.BoonLibService
import boonai.archivist.service.DatasetService
import boonai.common.util.Json
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import java.io.ByteArrayInputStream
import kotlin.test.assertEquals

class AwsBoonLibStorageServiceTests : AbstractTest() {

    @Autowired
    lateinit var projectStorageService: ProjectStorageService

    @Autowired
    lateinit var boonLibStorageService: BoonLibStorageService

    @Autowired
    lateinit var boonLibService: BoonLibService

    @Autowired
    lateinit var datasetService: DatasetService

    @Test
    fun testCopyFromProject() {

        val loc = ProjectFileLocator(ProjectStorageEntity.ASSETS, "1234", ProjectStorageCategory.SOURCE, "bob.txt")
        val spec = ProjectStorageSpec(loc, mapOf("cats" to 100), "test".toByteArray())
        val result = projectStorageService.store(spec)

        val req = mapOf(result.id to "boonlib/test/test/bob.txt")
        boonLibStorageService.copyFromProject(req)

        val content = String(
            boonLibStorageService.stream("boonlib/test/test/bob.txt").body.inputStream.readAllBytes()
        )
        assertEquals("test", content)
    }

    @Test
    fun testStoreAndStream() {
        boonLibStorageService.store("boonlib/test2/test2/foo.txt", 4, ByteArrayInputStream("test".toByteArray()))
        val content = String(
            boonLibStorageService.stream("boonlib/test2/test2/foo.txt").body.inputStream.readAllBytes()
        )
        assertEquals("test", content)
    }

    @Test
    fun importAssetsIntoDataset() {
        val ds = datasetService.createDataset(DatasetSpec("foo", DatasetType.Classification))
        val lib = boonLibService.createBoonLib(
            BoonLibSpec(
                "foo2",
                "test",
                BoonLibEntity.Dataset,
                ds.id
            )
        )

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

        boonLibService.importBoonLibInto(lib, ds)
    }
}
