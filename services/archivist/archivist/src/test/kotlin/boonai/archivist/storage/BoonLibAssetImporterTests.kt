package boonai.archivist.storage

import boonai.archivist.AbstractTest
import boonai.archivist.domain.Asset
import boonai.archivist.domain.AssetSpec
import boonai.archivist.domain.AssetState
import boonai.archivist.domain.BatchCreateAssetsRequest
import boonai.archivist.domain.BoonLibEntity
import boonai.archivist.domain.BoonLibSpec
import boonai.archivist.domain.DatasetSpec
import boonai.archivist.domain.DatasetType
import boonai.archivist.domain.Label
import boonai.archivist.service.BoonLibService
import boonai.archivist.service.DatasetService
import boonai.common.util.Json
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals

class BoonLibAssetImporterTests : AbstractTest() {

    @Autowired
    lateinit var projectStorageService: ProjectStorageService

    @Autowired
    lateinit var boonLibService: BoonLibService

    @Autowired
    lateinit var datasetService: DatasetService

    @Test
    fun testRelabel() {
        val ds = datasetService.createDataset(DatasetSpec("foo", DatasetType.Classification))
        val lib = boonLibService.createBoonLib(
            BoonLibSpec(
                "foo2",
                "test",
                BoonLibEntity.Dataset,
                ds.id
            )
        )

        val label = ds.makeLabel("cat")
        val req = BatchCreateAssetsRequest(
            assets = listOf(AssetSpec("gs://cats/large-brown-cat.jpg", label = label)),
            state = AssetState.Analyzed
        )
        assetService.batchCreate(req)
        val asset = getSample(1)[0]

        val ds2 = datasetService.createDataset(DatasetSpec("foo2", DatasetType.Classification))
        val importer = BoonLibAssetImporter(
            lib, ds2, assetService
        )

        importer.addAsset(Json.serialize(asset)!!)
        val batch = importer.getCurrentBatch()

        val updatedAsset = Asset(asset.id, batch.getValue(asset.id))
        val labels = updatedAsset.getAttr("labels", Label.LIST_OF)
        assertEquals(1, labels?.size)
        assertEquals(labels?.get(0)?.datasetId, ds2.id)
        assertEquals(lib.id.toString(), updatedAsset.getAttr("system.boonLibId"))
    }
}
