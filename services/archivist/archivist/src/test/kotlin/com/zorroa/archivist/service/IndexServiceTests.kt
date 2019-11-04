package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.BatchCreateAssetsRequest
import com.zorroa.archivist.domain.Document
import com.zorroa.archivist.domain.Pager
import com.zorroa.archivist.domain.Source
import com.zorroa.archivist.repository.IndexDao
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.TestPropertySource
import java.nio.file.Paths

/**
 * Created by chambers on 9/1/16.
 */
class IndexServiceTests : AbstractTest() {

    @Autowired
    lateinit var indexDao: IndexDao

    override fun requiresElasticSearch(): Boolean {
        return true
    }

    @Before
    fun init() {
        addTestAssets("set04/standard")
    }

    @Test
    fun testIndexWithBackup() {
        throw NotImplementedError()
    }

    @Test
    fun testGet() {
        val assets = indexService.getAll(Pager.first())
        for (a in assets) {
            assertEquals(
                a.id,
                indexService.get(Paths.get(a.getAttr("source.path", String::class.java))).id
            )
        }
    }

    @Test
    fun testGetAll() {
        val assets = indexService.getAll(Pager.first())
        assertEquals(2, assets.size())
    }

    @Test
    fun testDelete() {
        val assets = indexService.getAll(Pager.first())
        for (a in assets) {
            assertTrue(indexService.delete(a.id))
        }
    }

    @Test
    @Throws(InterruptedException::class)
    fun testIndexCheckOrigin() {
        val source = Source(getTestImagePath("set01/toucan.jpg"))
        assetService.createOrReplaceAssets(BatchCreateAssetsRequest(source))
        val asset1 = assetService.get(source.id)

        assertNotNull(asset1.getAttr("system.timeCreated"))
        assertNotNull(asset1.getAttr("system.timeModified"))
        assertEquals(
            asset1.getAttr("system.timeCreated", String::class.java),
            asset1.getAttr("system.timeModified", String::class.java)
        )

        refreshIndex()
        Thread.sleep(1000)
        val source2 = Source(getTestImagePath("set01/toucan.jpg"))
        assetService.createOrReplaceAssets(BatchCreateAssetsRequest(source2))
        val asset2 = assetService.get(source2.id)

        refreshIndex()
        assertNotEquals(
            asset2.getAttr("system.timeCreated", String::class.java),
            asset2.getAttr("system.timeModified", String::class.java)
        )
    }
}

@TestPropertySource(locations = ["classpath:test.properties", "classpath:jwt.properties"])
class JwtTokenSecurityIndexServiceTests : AbstractTest() {

    override fun requiresElasticSearch(): Boolean {
        return true
    }

    @Before
    fun init() {
        addTestAssets("set04/standard")
    }
}
