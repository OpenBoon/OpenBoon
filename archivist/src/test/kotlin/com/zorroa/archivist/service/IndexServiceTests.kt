package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.common.domain.Pager
import com.zorroa.common.domain.Source
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
    fun tetDelete() {
        val assets = indexService.getAll(Pager.first())
        for (a in assets) {
            assertTrue(indexService.delete(a.id as String))
        }
    }


    @Test
    @Throws(InterruptedException::class)
    fun testIndexCheckOrigin() {
        val builder = Source(getTestImagePath("set01/toucan.jpg"))
        val asset1 = indexService.index(builder)

        assertNotNull(asset1.getAttr("zorroa.timeCreated"))
        assertNotNull(asset1.getAttr("zorroa.timeModified"))
        assertEquals(asset1.getAttr("zorroa.timeCreated", String::class.java),
                asset1.getAttr("zorroa.timeModified", String::class.java))

        refreshIndex()
        Thread.sleep(1000)
        val builder2 = Source(getTestImagePath("set01/toucan.jpg"))
        val asset2 = indexService.index(builder2)

        refreshIndex()
        assertNotEquals(asset2.getAttr("zorroa.timeCreated", String::class.java),
                asset2.getAttr("zorroa.timeModified", String::class.java))
    }
}
