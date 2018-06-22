package com.zorroa.irm.studio.repository

import com.zorroa.irm.studio.AbstractTest
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AssetDaoTests : AbstractTest() {

    @Autowired
    lateinit var assetDao: AssetDao<CDVAssetSpec>

    @Test
    fun testCreateAndGet() {
        val spec = CDVAssetSpec(
                100,
                UUID.randomUUID().toString() + ".jpg")
        val result = assetDao.create(spec)
        assertEquals("100", result.organizationName)
        assertNotNull(result.id)
    }
}
