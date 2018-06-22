package com.zorroa.archivist.repository

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.security.getUser
import com.zorroa.common.domain.AssetSpec
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AssetDaoTests : AbstractTest() {

    @Autowired
    private lateinit var assetDao : AssetDao

    val assetSpec = AssetSpec(
            "visa.jpg")

    @Test
    fun testCreate() {
        val spec = AssetSpec("bilbo.png")
        val asset = assetDao.create(spec)
        assertNotNull(asset.id)
        assertEquals(getUser().organizationId, asset.organizationId)
    }

    @Test
    fun testGetIdById() {
        val asset1 = assetDao.create(assetSpec)
        val asset2 = assetDao.getId(asset1.id)
        assertEquals(asset1.id, asset2.id)
        assertEquals(asset1.organizationId, asset2.organizationId)
    }
}
