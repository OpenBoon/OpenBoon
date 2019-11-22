package com.zorroa.archivist.repository

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.TestAsset
import com.zorroa.archivist.domain.Document
import com.zorroa.archivist.security.getProjectId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired

class IndexDaoTests : AbstractTest() {

    @Autowired
    internal lateinit var indexDao: IndexDao
    internal lateinit var asset1: Document

    override fun requiresElasticSearch(): Boolean {
        return true
    }


    @Before
    fun testAssets() {
        logger.info("creating test assets")
        val builder = TestAsset(getTestImagePath("set04/standard/beer_kettle_01.jpg").toFile())
        builder.setAttr("bar.str", "dog")
        builder.setAttr("bar.int", 100)
        builder.setAttr("system.projectId", getProjectId().toString())
        asset1 = indexDao.index(builder, true)
        logger.info("Creating asset: {}", asset1.id)
    }


    @Test
    fun testGetFieldValue() {
        assertEquals("dog", indexDao.getFieldValue(asset1.id, "bar.str"))
        assertEquals(100, (indexDao.getFieldValue<Any>(asset1.id, "bar.int") as Int).toLong())
    }

    @Test
    fun testGetById() {
        val asset2 = indexDao.get(asset1.id)
        assertEquals(asset1.id, asset2.id)
    }

    @Test
    fun testGetByPath() {
        val p = getTestImagePath("set04/standard/beer_kettle_01.jpg")
        val asset2 = indexDao.get(p)
        assertNotNull(asset2)
    }

    @Test
    fun testExistsById() {
        assertTrue(indexDao.exists(asset1.id))
        assertFalse(indexDao.exists("abc"))
    }

    @Test
    fun testUpdate() {
        asset1.setAttr("foo.bar", 100)
        indexDao.update(asset1)
        val asset2 = indexDao.get(asset1.id)
        assertEquals(100, (asset2.getAttr<Any>("foo.bar") as Int).toLong())
    }

    @Test
    fun testDelete() {
        assertTrue(indexDao.delete(asset1))
    }

    @Test
    fun testBatchDelete() {
        val rsp1 = indexDao.batchDelete(listOf(asset1))
        refreshIndex()
        val rsp2 = indexDao.batchDelete(listOf(asset1))
        assertEquals(1, rsp1.deletedAssetIds.size)
        assertEquals(1, rsp1.totalRequested)
        assertEquals(0, rsp1.errors.size)

        assertEquals(0, rsp2.deletedAssetIds.size)
        assertEquals(1, rsp2.totalRequested)
        assertEquals(1, rsp2.missingAssetIds.size)
        assertEquals(0, rsp2.errors.size)
    }

    /*
    @Test
    @Throws(InterruptedException::class)
    fun testRetryBrokenFields() {

        val assets = ImmutableList.of<Document>(
            Source(getTestImagePath("set01/standard/faces.jpg"))
        )
        assets[0].setAttr("custom.foobar", 1000)
        var result = indexDao.index(assets)
        refreshIndex()

        val next = ImmutableList.of<Document>(
            Source(getTestImagePath("set01/standard/hyena.jpg")),
            Source(getTestImagePath("set01/standard/toucan.jpg")),
            Source(getTestImagePath("set01/standard/visa.jpg")),
            Source(getTestImagePath("set01/standard/visa12.jpg"))
        )
        for (s in next) {
            s.setAttr("custom.foobar", "bob")
        }
        result = indexDao.index(next)

        assertEquals(4, result.createdAssetIds.size)
        assertEquals(4, result.warningAssetIds.size)
        assertEquals(1, result.retryCount.toLong())
    }

     */
}
