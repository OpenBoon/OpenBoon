package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.repository.IndexDao
import com.zorroa.archivist.repository.KPage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    override fun requiresElasticSearch(): Boolean {
        return true
    }

    @Before
    fun addTestAssets() {
        addTestAssets("set04/standard")
    }

    @Test
    fun testGet() {
        val assets = indexService.getAll(KPage())
        for (a in assets) {
            assertEquals(
                a.id,
                indexService.get(Paths.get(a.getAttr("source.path", String::class.java))).id
            )
        }
    }

    @Test
    fun testDelete() {
        val assets = indexService.getAll(KPage())
        for (a in assets) {
            assertTrue(indexService.delete(a.id))
        }
    }
}
