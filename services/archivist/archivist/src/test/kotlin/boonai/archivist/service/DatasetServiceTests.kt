package boonai.archivist.service

import boonai.archivist.AbstractTest
import boonai.archivist.domain.AssetSpec
import boonai.archivist.domain.AssetState
import boonai.archivist.domain.BatchCreateAssetsRequest
import boonai.archivist.domain.Dataset
import boonai.archivist.domain.DatasetSpec
import boonai.archivist.domain.DatasetType
import boonai.archivist.domain.DatasetUpdate
import boonai.archivist.domain.Label
import boonai.archivist.domain.LabelScope
import boonai.archivist.domain.Model
import boonai.archivist.domain.UpdateAssetLabelsRequest
import boonai.archivist.repository.DatasetDao
import boonai.common.util.Json
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DatasetServiceTests : AbstractTest() {

    @Autowired
    lateinit var datasetDao: DatasetDao

    @Autowired
    lateinit var datasetService: DatasetService

    @Autowired
    lateinit var assetSearchService: AssetSearchService

    fun create(name: String = "test", type: DatasetType = DatasetType.Classification): Dataset {
        val mspec = DatasetSpec(
            name,
            type,
            "A Test DS"
        )
        return datasetService.createDataset(mspec)
    }

    fun dataset(ds: Dataset): List<AssetSpec> {
        return listOf(
            AssetSpec("https://i.imgur.com/12abc.jpg", label = ds.makeLabel("beaver")),
            AssetSpec("https://i.imgur.com/abc123.jpg", label = ds.makeLabel("ant")),
            AssetSpec("https://i.imgur.com/horse.jpg", label = ds.makeLabel("horse")),
            AssetSpec("https://i.imgur.com/zani.jpg", label = ds.makeLabel("zanzibar"))
        )
    }

    @Test
    fun testCreate() {
        val spec = DatasetSpec("pets", DatasetType.Classification, "TEST")
        val ds = datasetService.createDataset(spec)
        assertEquals("pets", ds.name)
        assertEquals(DatasetType.Classification, ds.type)
        assertEquals("TEST", ds.description)
    }

    @Test
    fun testUpdate() {
        val ds = datasetService.createDataset(DatasetSpec("pets", DatasetType.Classification))
        datasetService.updateDataset(ds, DatasetUpdate(name = "cars"))
        assertEquals("cars", ds.name)
    }

    @Test
    fun testDelete() {
        val ds = create()
        val specs = dataset(ds)

        assetService.batchCreate(BatchCreateAssetsRequest(specs, state=AssetState.Analyzed))
        refreshIndex(1000)
        assertFalse(datasetService.getLabelCounts(ds).isEmpty())
        datasetService.deleteDataset(ds)
        Thread.sleep(3000)
        assertTrue(datasetService.getLabelCounts(ds).isEmpty())
    }

    @Test
    fun testGet() {
        val ds = datasetService.createDataset(DatasetSpec("pets", DatasetType.Classification))
        val ds2 = datasetService.getDataset(ds.id)
        val ds3 = datasetService.getDataset(ds.name)
        assertEquals(ds, ds2)
        assertEquals(ds2, ds3)
    }

    @Test
    fun excludeLabeledAssetsFromSearch() {
        val dataset = create()

        val spec = AssetSpec("https://i.imgur.com/SSN26nN.jpg", label = dataset.makeLabel("husky"))
        val rsp = assetService.batchCreate(BatchCreateAssetsRequest(listOf(spec)))
        val asset = assetService.getAsset(rsp.created[0])
        assetService.index(asset.id, asset.document, true)
        refreshIndex()

        val counts = datasetService.getLabelCounts(dataset)
        assertEquals(1, counts["husky"])

        assertEquals(1, assetSearchService.count(Model.matchAllSearch))
        assertEquals(
            0,
            assetSearchService.count(
                datasetService.wrapSearchToExcludeTrainingSet(dataset, Model.matchAllSearch)
            )
        )
    }

    @Test
    fun getLabelCounts() {
        val ds1 = create("test1")
        val ds2 = create("test2")

        val rsp = assetService.batchCreate(
            BatchCreateAssetsRequest(dataset(ds1), state=AssetState.Analyzed)
        )

        assetService.updateLabels(
            UpdateAssetLabelsRequest(
                // Validate adding 2 identical labels only adds 1
                mapOf(
                    rsp.created[0] to listOf(
                        Label(ds2.id, "house"),
                    ),
                    rsp.created[1] to listOf(
                        Label(ds2.id, "tree"),
                    )
                )
            )
        )

        val counts = datasetService.getLabelCounts(ds1)
        assertEquals(4, counts.size)
        assertEquals(1, counts["ant"])
        assertEquals(1, counts["horse"])
        assertEquals(1, counts["beaver"])
        assertEquals(1, counts["zanzibar"])

        val keys = counts.keys.toList()
        assertEquals("ant", keys[0])
        assertEquals("zanzibar", keys[3])
    }

    @Test
    fun getLabelCountsV4() {
        val ds1 = create("test1")
        val ds2 = create("test2")

        val rsp = assetService.batchCreate(
            BatchCreateAssetsRequest(dataset(ds1), state=AssetState.Analyzed)
        )

        assetService.updateLabels(
            UpdateAssetLabelsRequest(
                mapOf(
                    rsp.created[0] to listOf(
                        Label(ds2.id, "house"),
                    ),
                    rsp.created[1] to listOf(
                        Label(ds2.id, "tree", scope = LabelScope.TEST),
                    )
                )
            )
        )
        refreshIndex(100)

        val counts = datasetService.getLabelCountsV4(ds2)
        Json.prettyPrint(counts)
        assertEquals(2, counts.size)
        assertEquals(1, counts["tree"]?.get("TEST"))
        assertEquals(1, counts["house"]?.get("TRAIN"))
    }

    @Test
    fun testRenameLabel() {
        val model = create()
        val specs = dataset(model)

        assetService.batchCreate(
            BatchCreateAssetsRequest(specs, state=AssetState.Analyzed)
        )
        datasetService.updateLabel(model, "beaver", "horse")
        refreshElastic()
        val counts = datasetService.getLabelCounts(model)
        assertEquals(1, counts["ant"])
        assertEquals(2, counts["horse"])
        assertEquals(null, counts["beaver"])
        assertEquals(1, counts["zanzibar"])
    }

    @Test
    fun testRenameLabelToNullForDelete() {
        val model = create()
        val specs = dataset(model)

        assetService.batchCreate(
            BatchCreateAssetsRequest(specs, state=AssetState.Analyzed)
        )
        datasetService.updateLabel(model, "horse", null)
        refreshIndex(1000L)

        val counts = datasetService.getLabelCounts(model)
        assertEquals(1, counts["ant"])
        assertEquals(null, counts["horse"])
        assertEquals(1, counts["beaver"])
        assertEquals(1, counts["zanzibar"])
    }
}
