package boonai.archivist.service

import boonai.archivist.AbstractTest
import boonai.archivist.domain.AssetSpec
import boonai.archivist.domain.BatchCreateAssetsRequest
import boonai.archivist.domain.DataSet
import boonai.archivist.domain.DataSetSpec
import boonai.archivist.domain.DataSetType
import boonai.archivist.domain.DataSetUpdate
import boonai.archivist.domain.Label
import boonai.archivist.domain.Model
import boonai.archivist.domain.UpdateAssetLabelsRequest
import boonai.archivist.repository.DataSetDao
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DataSetServiceTests : AbstractTest() {

    @Autowired
    lateinit var dataSetDao: DataSetDao

    @Autowired
    lateinit var dataSetService: DataSetService

    @Autowired
    lateinit var assetSearchService: AssetSearchService

    fun create(name: String = "test", type: DataSetType = DataSetType.Classification): DataSet {
        val mspec = DataSetSpec(
            name,
            type
        )
        return dataSetService.createDataSet(mspec)
    }

    fun dataSet(ds: DataSet): List<AssetSpec> {
        return listOf(
            AssetSpec("https://i.imgur.com/12abc.jpg", label = ds.makeLabel("beaver")),
            AssetSpec("https://i.imgur.com/abc123.jpg", label = ds.makeLabel("ant")),
            AssetSpec("https://i.imgur.com/horse.jpg", label = ds.makeLabel("horse")),
            AssetSpec("https://i.imgur.com/zani.jpg", label = ds.makeLabel("zanzibar"))
        )
    }

    @Test
    fun testCreate() {
        val spec = DataSetSpec("pets", DataSetType.Classification)
        val ds = dataSetService.createDataSet(spec)
        assertEquals("pets", ds.name)
        assertEquals(DataSetType.Classification, ds.type)
    }

    @Test
    fun testUpdate() {
        val ds = dataSetService.createDataSet(DataSetSpec("pets", DataSetType.Classification))
        dataSetService.updateDataSet(ds, DataSetUpdate(name = "cars"))
        assertEquals("cars", ds.name)
    }

    @Test
    fun testDelete() {
        val ds = create()
        val specs = dataSet(ds)

        assetService.batchCreate(BatchCreateAssetsRequest(specs))
        refreshIndex(1000)
        assertFalse(dataSetService.getLabelCounts(ds).isEmpty())
        dataSetService.deleteDataSet(ds)
        Thread.sleep(3000)
        assertTrue(dataSetService.getLabelCounts(ds).isEmpty())
    }

    @Test
    fun testGet() {
        val ds = dataSetService.createDataSet(DataSetSpec("pets", DataSetType.Classification))
        val ds2 = dataSetService.getDataSet(ds.id)
        val ds3 = dataSetService.getDataSet(ds.name)
        assertEquals(ds, ds2)
        assertEquals(ds2, ds3)
    }

    @Test
    fun excludeLabeledAssetsFromSearch() {
        val dataSet = create()

        val spec = AssetSpec("https://i.imgur.com/SSN26nN.jpg", label = dataSet.makeLabel("husky"))
        val rsp = assetService.batchCreate(BatchCreateAssetsRequest(listOf(spec)))
        val asset = assetService.getAsset(rsp.created[0])
        assetService.index(asset.id, asset.document, true)
        refreshIndex()

        val counts = dataSetService.getLabelCounts(dataSet)
        assertEquals(1, counts["husky"])

        assertEquals(1, assetSearchService.count(Model.matchAllSearch))
        assertEquals(
            0,
            assetSearchService.count(
                dataSetService.wrapSearchToExcludeTrainingSet(dataSet, Model.matchAllSearch)
            )
        )
    }

    @Test
    fun getLabelCounts() {
        val ds1 = create("test1")
        val ds2 = create("test2")

        val rsp = assetService.batchCreate(
            BatchCreateAssetsRequest(dataSet(ds1))
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

        val counts = dataSetService.getLabelCounts(ds1)
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
    fun testRenameLabel() {
        val model = create()
        val specs = dataSet(model)

        assetService.batchCreate(
            BatchCreateAssetsRequest(specs)
        )
        dataSetService.updateLabel(model, "beaver", "horse")
        refreshElastic()
        val counts = dataSetService.getLabelCounts(model)
        assertEquals(1, counts["ant"])
        assertEquals(2, counts["horse"])
        assertEquals(null, counts["beaver"])
        assertEquals(1, counts["zanzibar"])
    }

    @Test
    fun testRenameLabelToNullForDelete() {
        val model = create()
        val specs = dataSet(model)

        assetService.batchCreate(
            BatchCreateAssetsRequest(specs)
        )
        dataSetService.updateLabel(model, "horse", null)
        refreshIndex(1000L)

        val counts = dataSetService.getLabelCounts(model)
        assertEquals(1, counts["ant"])
        assertEquals(null, counts["horse"])
        assertEquals(1, counts["beaver"])
        assertEquals(1, counts["zanzibar"])
    }
}
