package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.Pager
import com.zorroa.archivist.domain.Source
import com.zorroa.common.util.Json
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.nio.file.Paths

/**
 * Created by chambers on 9/1/16.
 */
class IndexServiceTests : AbstractTest() {

    @Before
    fun init() {
        addTestAssets("set04/standard")
        refreshIndex()
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
            assertTrue(indexService.delete(a.id as String))
        }
    }

    @Test
    fun testBatchDelete() {
        val assets = indexService.getAll(Pager.first())
        val res = indexService.batchDelete(assets.map { it.id })
        assertEquals(2, res.totalRequested)
        assertEquals(2, res.totalDeleted)
        assertTrue(res.failures.isEmpty())
    }

    @Test
    fun testBatchDeleteWithChildren() {
        val assets = indexService.getAll(Pager.first())
        val child = assets[1]
        indexService.update(child.id, mapOf("media.clip.parent" to assets[0].id))
        refreshIndex()
        Thread.sleep(1000)

        val res = indexService.batchDelete(listOf(assets[0].id))
        assertEquals(2, res.totalRequested)
        assertEquals(2, res.totalDeleted)
        assertTrue(res.failures.isEmpty())
    }

    @Test
    fun testBatchDeleteSkipChildren() {
        val assets = indexService.getAll(Pager.first())
        val child = assets[1]
        indexService.update(child.id, mapOf("media.clip.parent" to assets[0].id))
        refreshIndex()
        Thread.sleep(1000)

        val res = indexService.batchDelete(listOf(child.id))
        assertEquals(0, res.totalRequested)
        assertEquals(0, res.totalDeleted)
        assertTrue(res.failures.isEmpty())
    }

    @Test
    @Throws(InterruptedException::class)
    fun testIndexCheckOrigin() {
        val builder = Source(getTestImagePath("set01/toucan.jpg"))
        val asset1 = indexService.index(builder)

        assertNotNull(asset1.getAttr("system.timeCreated"))
        assertNotNull(asset1.getAttr("system.timeModified"))
        assertEquals(asset1.getAttr("system.timeCreated", String::class.java),
                asset1.getAttr("system.timeModified", String::class.java))

        refreshIndex()
        Thread.sleep(1000)
        val builder2 = Source(getTestImagePath("set01/toucan.jpg"))
        val asset2 = indexService.index(builder2)

        refreshIndex()
        assertNotEquals(asset2.getAttr("system.timeCreated", String::class.java),
                asset2.getAttr("system.timeModified", String::class.java))
    }
}
