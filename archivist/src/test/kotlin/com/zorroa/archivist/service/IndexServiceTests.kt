package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.Pager
import com.zorroa.archivist.domain.Source
import com.zorroa.archivist.repository.IndexDao
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import java.nio.file.Paths

/**
 * Created by chambers on 9/1/16.
 */
class IndexServiceTests : AbstractTest() {

    @Autowired
    lateinit var indexDao: IndexDao

    override fun requiresElasticSearch() : Boolean {
        return true
    }

    @Before
    fun init() {
        addTestAssets("set04/standard")
    }

    @Test
    fun testGetAsset() {
        val assets = indexService.getAll(Pager.first())
        for (a in assets) {
            assertEquals(a.id,
                    indexService.get(Paths.get(a.getAttr("source.path", String::class.java))).id)
        }
    }

    @Test
    fun testDelete() {
        val assets = indexService.getAll(Pager.first())
        for (a in assets) {
            assertTrue(indexService.delete(a.id))
        }
    }

    @Test
    fun testBatchDelete() {
        val assets = indexService.getAll(Pager.first())
        val res = indexService.batchDelete(assets.map { it.id })
        assertEquals(2, res.totalRequested)
        assertEquals(2, res.deletedAssetIds.size)
        assertTrue(res.errors.isEmpty())
        Thread.sleep(2000)
    }

    @Test
    fun testBatchDeleteEmptyList() {
        val assets = indexService.getAll(Pager.first())
        val res = indexService.batchDelete(listOf())
        assertEquals(0, res.totalRequested)
        assertEquals(0, res.deletedAssetIds.size)
        assertTrue(res.errors.isEmpty())
    }

    @Test
    fun testBatchDeleteWithChildren() {
        val assets = indexService.getAll(Pager.first())
        val child = assets[1]
        indexService.update(child, mapOf("media.clip.parent" to assets[0].id))
        refreshIndex()
        Thread.sleep(1000)

        val res = indexService.batchDelete(listOf(assets[0].id))
        assertEquals(2, res.totalRequested)
        assertEquals(2, res.deletedAssetIds.size)
        assertTrue(res.errors.isEmpty())
    }

    @Test
    fun testBatchDeleteWithOnHold() {
        val assets = indexService.getAll(Pager.first())
        assets[0].setAttr("system.hold", true)
        indexDao.update(assets[0])
        refreshIndex()

        val res = indexService.batchDelete(assets.map { it.id })
        assertEquals(1, res.totalRequested)
        assertEquals(1, res.deletedAssetIds.size)
        assertEquals(1, res.onHoldAssetIds.size)
        assertTrue(res.errors.isEmpty())
    }

    @Test
    fun testBatchDeleteSkipChildren() {
        val assets = indexService.getAll(Pager.first())
        val child = assets[1]
        indexService.update(child, mapOf("media.clip.parent" to assets[0].id))
        refreshIndex()
        Thread.sleep(1000)

        val res = indexService.batchDelete(listOf(child.id))
        assertEquals(0, res.totalRequested)
        assertEquals(0, res.deletedAssetIds.size)
        assertTrue(res.errors.isEmpty())
    }

    @Test
    fun testUpdate() {
        val asset = indexService.getAll(Pager.first())[0]
        val result = indexService.update(asset, mapOf("foo.bar.bing" to "bang"))
        assertEquals("bang", result.getAttr("foo.bar.bing"))
    }

    @Test
    @Throws(InterruptedException::class)
    fun testIndexCheckOrigin() {
        val source = Source(getTestImagePath("set01/toucan.jpg"))
        val asset1 = assetService.createOrReplace(source)

        assertNotNull(asset1.getAttr("system.timeCreated"))
        assertNotNull(asset1.getAttr("system.timeModified"))
        assertEquals(asset1.getAttr("system.timeCreated", String::class.java),
                asset1.getAttr("system.timeModified", String::class.java))

        refreshIndex()
        Thread.sleep(1000)
        val source2 = Source(getTestImagePath("set01/toucan.jpg"))
        val asset2 = assetService.createOrReplace(source2)

        refreshIndex()
        assertNotEquals(asset2.getAttr("system.timeCreated", String::class.java),
                asset2.getAttr("system.timeModified", String::class.java))
    }
}
