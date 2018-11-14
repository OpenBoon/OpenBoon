package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.AuditLogType
import com.zorroa.archivist.domain.BatchCreateAssetsRequest
import com.zorroa.archivist.domain.Pager
import com.zorroa.archivist.search.AssetSearch
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AssetServiceTests : AbstractTest() {

    @Before
    fun init() {
        addTestAssets("set04/standard")
        refreshIndex()
    }

    @Test
    fun testCreateWithWatchedField() {
        System.setProperty("archivist.auditlog.watched-fields", "foo")
        try {
            val assets = getTestAssets("set04/standard")
            assets[0].setAttr("foo", "bar")
            assetService.batchCreateOrReplace(BatchCreateAssetsRequest(assets))
            val changeCount = jdbc.queryForMap("SELECT * FROM auditlog WHERE pk_asset=?::uuid AND int_type=?",
                    assets[0].id, AuditLogType.Changed.ordinal)
            println(changeCount)

        } finally {
            System.clearProperty("archivist.auditlog.watched-fields")
        }
    }


    @Test
    fun testBatchCreateOrReplace() {
        val assets = getTestAssets("set04/standard")
        val rsp = assetService.batchCreateOrReplace(BatchCreateAssetsRequest(assets))
        assertEquals(2, rsp.replacedAssetIds.size)
        assertEquals(0, rsp.createdAssetIds.size)
    }

    @Test
    fun testCreateOrReplace() {
        val asset = getTestAssets("set04/standard")[0]
        asset.setAttr("foo", "bar")
        val newAsset = assetService.createOrReplace(asset)
        assertEquals(newAsset.getAttr("foo", String::class.java), "bar")
    }

    @Test
    fun testGet() {
        val asset1 = getTestAssets("set04/standard")[0]
        val asset2 = assetService.get(asset1.id)
        assertEquals(asset1.id, asset2.id)
        assertEquals(asset1.getAttr("source.path", String::class.java),
                asset2.getAttr("source.path", String::class.java))
    }

    @Test
    fun testUpdate() {
        val asset1 = getTestAssets("set04/standard")[0]
        val asset2 = assetService.update(asset1.id, mapOf("foo" to "bar"))
        assertEquals(asset1.id, asset2.id)
        assertEquals(asset1.getAttr("source.path", String::class.java),
                asset2.getAttr("source.path", String::class.java))
        assertEquals(asset2.getAttr("foo", String::class.java), "bar")
    }

    @Test
    fun testDelete() {
        val page = searchService.search(Pager.first(), AssetSearch())
        assertTrue(assetService.delete(page[0].id))
        refreshIndex()

        val leftOvers = searchService.search(Pager.first(), AssetSearch())
        assertEquals(page.size() - leftOvers.size(), 1)
    }

    @Test
    fun batchDelete() {
        val page = searchService.search(Pager.first(), AssetSearch())
        val ids = page.map { it.id }
        val rsp = assetService.batchDelete(ids)
        assertEquals(0, rsp.failures.size)
        assertEquals(2, rsp.totalDeleted)
        assertEquals(2, rsp.totalRequested)
    }
}