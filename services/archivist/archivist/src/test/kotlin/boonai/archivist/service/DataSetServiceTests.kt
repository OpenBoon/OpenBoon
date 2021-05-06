package boonai.archivist.service

import boonai.archivist.AbstractTest
import boonai.archivist.domain.DataSetSpec
import boonai.archivist.domain.DataSetType
import boonai.archivist.domain.DataSetUpdate
import boonai.archivist.repository.DataSetDao
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals

class DataSetServiceTests : AbstractTest() {

    @Autowired
    lateinit var dataSetDao: DataSetDao

    @Autowired
    lateinit var dataSetService: DataSetService

    @Test
    fun testCreate() {
        val spec = DataSetSpec("pets", DataSetType.Classification)
        val ds = dataSetService.createDataSet(spec)
        assertEquals("pets", ds.name)
        assertEquals(DataSetType.Classification, ds.type)
    }

    @Test
    fun testDataSetUpdate() {
        val ds = dataSetService.createDataSet(DataSetSpec("pets", DataSetType.Classification))
        dataSetService.updateDataSet(ds, DataSetUpdate(name = "cars"))
        assertEquals("cars", ds.name)
    }

    @Test
    fun testGet() {
        val ds = dataSetService.createDataSet(DataSetSpec("pets", DataSetType.Classification))
        val ds2 = dataSetService.getDataSet(ds.id)
        val ds3 = dataSetService.getDataSet(ds.name)
        assertEquals(ds, ds2)
        assertEquals(ds2, ds3)
    }
}
