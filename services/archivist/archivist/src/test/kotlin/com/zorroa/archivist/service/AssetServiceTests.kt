package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.Asset
import com.zorroa.archivist.domain.AssetSpec
import com.zorroa.archivist.domain.BatchCreateAssetsRequest
import com.zorroa.archivist.domain.BatchUpdateAssetsRequest
import com.zorroa.archivist.domain.BatchUploadAssetsRequest
import com.zorroa.archivist.domain.InternalTask
import com.zorroa.archivist.domain.TaskState
import org.junit.Test
import org.springframework.mock.web.MockMultipartFile
import java.io.File
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
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
            assets = listOf(AssetSpec("gs://cats/large-brown-cat.jpg")),
            task = InternalTask(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "test",
                TaskState.Success))

        val rsp = assetService.batchCreate(req)
        assertEquals(1, rsp.status.size)
        assertFalse(rsp.status[0].failed)

        val asset = assetService.getAsset(rsp.status[0].assetId)
        assertEquals(req.assets[0].uri, asset.getAttr("source.path", String::class.java))
        assertNotNull(asset.getAttr("system.jobId"))
        assertNotNull(asset.getAttr("system.dataSourceId"))
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
        assertTrue(rsp.status[0].failed)
        assertEquals("asset already exists", rsp.status[0].failureMessage)
    }

    @Test
    fun testBatchUpdateAssets() {
        val batchCreate = BatchCreateAssetsRequest(
            assets = listOf(AssetSpec("gs://cats/large-brown-cat.jpg"))
        )
        val createRsp = assetService.batchCreate(batchCreate)
        val asset = assetService.getAsset(createRsp.status[0].assetId)
        asset.setAttr("test.field", 1)
        val batchIndex = BatchUpdateAssetsRequest(
            assets = listOf(asset)
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
        val asset = assetService.getAsset(createRsp.status[0].assetId)

        val req = BatchUpdateAssetsRequest(
            assets = listOf(
                Asset(),
                Asset(asset.id),
                Asset(),
                Asset(asset.id)
            )
        )

        val rsp = assetService.batchUpdate(req)
        assertTrue(rsp.status[0]!!.failed)
        assertFalse(rsp.status[1]!!.failed)
        assertTrue(rsp.status[2]!!.failed)
        assertFalse(rsp.status[3]!!.failed)
    }

    @Test
    fun testBatchUploadAssets() {
        val batchUpload = BatchUploadAssetsRequest(
            assets = listOf(AssetSpec("/foo/bar/toucan.jpg"))
        )

        batchUpload.files = arrayOf(
            MockMultipartFile(
                "files", "file-name.data", "image/jpeg",
                File("src/test/resources/test-data/toucan.jpg").inputStream().readAllBytes()
            )
        )

        val rsp = assetService.batchUpload(batchUpload)
        assertEquals("toucan.jpg", rsp.assets[0].getAttr("source.filename", String::class.java))
        assertFalse(rsp.status[0].failed)
    }
}
