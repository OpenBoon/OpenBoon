package com.zorroa.archivist.repository

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.AssetSpec
import com.zorroa.archivist.domain.AssetState
import com.zorroa.archivist.security.getUser
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AssetDaoTests : AbstractTest() {

    @Autowired
    private lateinit var assetDao : AssetDao

    val assetSpec = AssetSpec(
            "visa.jpg",
            location = "../zorroa-test-data/images/set01/visa.jpg",
            directAccess = true)

    @Test
    fun testCreateNoLocation() {
        val spec = AssetSpec("bilbo.png")
        val asset = assetDao.create(spec)
        assertEquals(asset.state, AssetState.PENDING_FILE,
                "Asset should be waiting for a file because no location was specified")
        assertNotNull(asset.id)
        assertEquals(getUser().organizationId, asset.organizationId)
    }

    @Test
    fun testCreateDirectAccessFile() {
        val asset = assetDao.create(assetSpec)
        assertEquals(asset.state, AssetState.PENDING,
                "Asset should be pending because its ready to process")
        assertNotNull(asset.id)
        assertEquals(getUser().organizationId, asset.organizationId)
    }

    @Test
    fun testGetIdByLocation() {
        val asset1 = assetDao.create(assetSpec)
        val asset2 = assetDao.getId(assetSpec.location!!)
        assertEquals(asset1.id, asset2.id)
        assertEquals(asset1.organizationId, asset2.organizationId)
        assertEquals(asset1.state, asset2.state)
    }

    @Test
    fun testGetIdById() {
        val asset1 = assetDao.create(assetSpec)
        val asset2 = assetDao.getId(asset1.id)
        assertEquals(asset1.id, asset2.id)
        assertEquals(asset1.organizationId, asset2.organizationId)
        assertEquals(asset1.state, asset2.state)
    }
}
