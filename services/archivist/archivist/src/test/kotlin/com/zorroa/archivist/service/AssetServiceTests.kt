package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.Asset
import com.zorroa.archivist.domain.AssetSearch
import com.zorroa.archivist.domain.AssetSpec
import com.zorroa.archivist.domain.BatchCreateAssetsRequest
import com.zorroa.archivist.domain.BatchUpdateAssetsRequest
import com.zorroa.archivist.domain.BatchUploadAssetsRequest
import com.zorroa.archivist.domain.Clip
import com.zorroa.archivist.domain.InternalTask
import com.zorroa.archivist.domain.TaskState
import org.junit.Test
import org.springframework.mock.web.MockMultipartFile
import java.io.File
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
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

    @Test
    fun testBatchCreateAssets_failInvalidDynamicField() {
        val spec = AssetSpec(
            "gs://cats/large-brown-cat.jpg",
            mapOf("dog" to "cat")
        )

        val req = BatchCreateAssetsRequest(
            assets = listOf(spec)
        )
        val rsp = assetService.batchCreate(req)
        assertTrue(rsp.status[0].failed)
        assertNotNull(rsp.status[0].failureMessage)
        assertTrue(rsp.status[0].failureMessage!!.contains("is not allowed"))
    }

    @Test
    fun testBatchCreateAssets_WithAuxFields() {
        val spec = AssetSpec(
            "gs://cats/large-brown-cat.jpg",
            mapOf("aux.pet" to "dog")
        )

        val req = BatchCreateAssetsRequest(
            assets = listOf(spec)
        )
        val rsp = assetService.batchCreate(req)
        assertFalse(rsp.status[0].failed)
        assertEquals("dog", rsp.assets[0].getAttr("aux.pet", String::class.java))
    }

    @Test
    fun testBatchCreateAssets_WithIgnoreFields() {
        val spec = AssetSpec(
            "gs://cats/large-brown-cat.jpg",
            mapOf("system.hello" to "foo")
        )

        val req = BatchCreateAssetsRequest(
            assets = listOf(spec)
        )
        val rsp = assetService.batchCreate(req)
        assertFalse(rsp.status[0].failed)
        assertNull(rsp.assets[0].getAttr("system.hello"))
    }

    @Test
    fun testBatchCreateAssets_WithClip() {
        val spec = AssetSpec(
            "gs://cats/large-brown-cat.jpg",
            mapOf("system.hello" to "foo"),
            clip = Clip("page", 3f, 3f, "pages")
        )

        val req = BatchCreateAssetsRequest(
            assets = listOf(spec)
        )
        val rsp = assetService.batchCreate(req)
        assertEquals(3f, rsp.assets[0].getAttr<Float?>("clip.start"))
        assertEquals(3f, rsp.assets[0].getAttr<Float?>("clip.stop"))
        assertEquals("page", rsp.assets[0].getAttr<String?>("clip.type"))
        assertEquals("pages", rsp.assets[0].getAttr<String?>("clip.timeline"))
        assertEquals("gIjnIgVCxeoDkrc6DrUzT_lA1ws", rsp.assets[0].getAttr<String?>("clip.pile"))
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
        asset.setAttr("aux.field", 1)

        val batchIndex = BatchUpdateAssetsRequest(
            assets = listOf(asset)
        )
        val indexRsp = assetService.batchUpdate(batchIndex)
        assertFalse(indexRsp.status[0]!!.failed)
    }

    @Test
    fun testBatchUpdateAssetsWithTempFields() {
        val batchCreate = BatchCreateAssetsRequest(
            assets = listOf(AssetSpec("gs://cats/large-brown-cat.jpg"))
        )
        val createRsp = assetService.batchCreate(batchCreate)
        var asset = assetService.getAsset(createRsp.status[0].assetId)
        asset.setAttr("aux.field", 1)
        asset.setAttr("tmp.field", 1)

        val batchIndex = BatchUpdateAssetsRequest(
            assets = listOf(asset)
        )
        val updateRsp = assetService.batchUpdate(batchIndex)
        assertFalse(updateRsp.status[0]!!.failed)

        asset = assetService.getAsset(createRsp.status[0].assetId)
        assertFalse(asset.attrExists("tmp.field"))
        assertFalse(asset.attrExists("tmp"))
    }

    @Test
    fun testBatchUpdateAssetsWithClip() {
        val batchCreate = BatchCreateAssetsRequest(
            assets = listOf(AssetSpec("gs://cats/large-brown-cat.jpg"))
        )
        val createRsp = assetService.batchCreate(batchCreate)
        var asset = assetService.getAsset(createRsp.status[0].assetId)
        asset.setAttr("clip", mapOf("type" to "page", "start" to 2f, "stop" to 2f))

        val batchIndex = BatchUpdateAssetsRequest(assets = listOf(asset))
        assetService.batchUpdate(batchIndex)

        asset = assetService.getAsset(createRsp.status[0].assetId)
        assertEquals("page", asset.getAttr<String?>("clip.type"))
        assertEquals(2.0, asset.getAttr<Double?>("clip.start"))
        assertEquals(2.0, asset.getAttr<Double?>("clip.stop"))
        assertEquals(1.0, asset.getAttr<Double?>("clip.length"))
        assertEquals("h_mv_VYvSyP3ViYiffJKiJD6Pvg", asset.getAttr<String?>("clip.pile"))

        val clip = asset.getAttr("clip", Clip::class.java)
        assertEquals("page", clip?.type)
        assertEquals(2.0f, clip?.start)
        assertEquals(2.0f, clip?.stop)
        assertEquals(1.0f, clip?.length)
        assertEquals("h_mv_VYvSyP3ViYiffJKiJD6Pvg", clip?.pile)
    }

    /**
     * Trying to update assets that don't exist should fail.
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
        assertEquals(1582911032, rsp.assets[0].getAttr("source.checksum", Int::class.java))
        assertEquals(97221, rsp.assets[0].getAttr("source.filesize", Long::class.java))
        assertFalse(rsp.status[0].failed)
    }

    @Test
    fun testSearch() {
        val batchCreate = BatchCreateAssetsRequest(
            assets = listOf(AssetSpec("https://i.imgur.com/LRoLTlK.jpg"))
        )
        assetService.batchCreate(batchCreate)

        // Note that here, scroll is not allowed yet the result should have no scroll id.
        val search = AssetSearch(
            mapOf(
                "query" to mapOf("term" to mapOf("source.filename" to "LRoLTlK.jpg")),
                "scroll" to "2s"
            )
        )
        val rsp = assetService.search(search)
        assertEquals(1, rsp.hits.hits.size)
        assertNull(rsp.scrollId)
    }
}
