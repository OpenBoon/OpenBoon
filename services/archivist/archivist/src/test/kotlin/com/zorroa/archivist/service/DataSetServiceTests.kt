package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.DataSetSpec
import com.zorroa.archivist.domain.DataSetType
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals

class DataSetServiceTests : AbstractTest() {

    @Autowired
    lateinit var dataSetService: DataSetService

    val spec = DataSetSpec("dog-breeds", DataSetType.LabelDetection)

    @Test
    fun testCreate() {

        val ts = dataSetService.create(spec)
        assertEquals(spec.name, ts.name)
        assertEquals(spec.type, ts.type)
    }

    @Test
    fun testGetById() {
        val ts1 = dataSetService.create(spec)
        val ts2 = dataSetService.get(ts1.id)
        assertEquals(ts1.id, ts2.id)
    }

    @Test
    fun testGetByName() {
        val ts1 = dataSetService.create(spec)
        val ts2 = dataSetService.get(ts1.name)
        assertEquals(ts1.id, ts2.id)
    }
}