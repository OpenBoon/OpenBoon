package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.Asset
import com.zorroa.archivist.domain.AssetSpec
import com.zorroa.archivist.domain.BatchCreateAssetsRequest
import com.zorroa.archivist.domain.BatchUpdateAssetsRequest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AssetServiceTests : AbstractTest() {

    override fun requiresElasticSearch(): Boolean {
        return true
    }

    override fun requiresFieldSets(): Boolean {
        return true
    }

    @Test
    fun testBatchCreateAssets() {
        val req = BatchCreateAssetsRequest(
            assets = listOf(AssetSpec("gs://cats/large-brown-cat.jpg")))

        val rsp = assetService.batchCreate(req)
        assertEquals(1, rsp.status.size)
        assertEquals(1, rsp.assets.size)
        assertFalse(rsp.status[0].failed)

        val asset = rsp.assets[0]
        assertEquals(asset.getAttr("source.path", String::class.java), req.assets[0].uri)
    }

    /**
     * Recreating an asset that already exists should fail.
     */
    @Test
    fun testBatchCreateAssets_failAlreadyExists() {
        val req = BatchCreateAssetsRequest(
            assets = listOf(AssetSpec("gs://cats/large-brown-cat.jpg"))
        )

        assetService.batchCreate(req)
        val rsp = assetService.batchCreate(req)
        assertEquals(1, rsp.status.size)
        assertEquals(1, rsp.assets.size)
        assertTrue(rsp.status[0].failed)
        assertEquals("asset already exists", rsp.status[0].failureMessage)
    }

    @Test
    fun testBatchUpdateAssets() {
        val batchCreate = BatchCreateAssetsRequest(
            assets = listOf(AssetSpec("gs://cats/large-brown-cat.jpg"))
        )
        val createRsp = assetService.batchCreate(batchCreate)
        createRsp.assets[0].setAttr("test.field", 1)

        val batchIndex = BatchUpdateAssetsRequest(
            assets = createRsp.assets
        )
        val indexRsp = assetService.batchUpdate(batchIndex)
        assertFalse(indexRsp.status[0]!!.failed)
    }

    /**
     * Trying to index assets that don't exist should fail.
     */
    @Test
    fun testBatchUpdateAssets_failNotCreatedSingle() {
        val req = BatchUpdateAssetsRequest(
            assets = listOf(Asset())
        )
        val rsp = assetService.batchUpdate(req)
        assertTrue(rsp.status[0]!!.failed)
    }

    @Test
    fun testBatchIndexAssets_failCreatedMultiple() {
        val batchCreate = BatchCreateAssetsRequest(
            assets = listOf(AssetSpec("gs://cats/large-brown-cat.jpg"))
        )
        val createRsp = assetService.batchCreate(batchCreate)

        val req = BatchUpdateAssetsRequest(
            assets = listOf(
                Asset(),
                Asset(createRsp.assets[0].id),
                Asset(),
                Asset(createRsp.assets[0].id)
            )
        )

        val rsp = assetService.batchUpdate(req)
        assertTrue(rsp.status[0]!!.failed)
        assertFalse(rsp.status[1]!!.failed)
        assertTrue(rsp.status[2]!!.failed)
        assertFalse(rsp.status[3]!!.failed)
    }
}
