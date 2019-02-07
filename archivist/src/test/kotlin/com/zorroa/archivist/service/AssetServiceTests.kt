package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.*
import com.zorroa.archivist.search.AssetSearch
import com.zorroa.common.schema.PermissionSchema
import com.zorroa.security.Groups
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNull
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
            val assets =  searchService.search(Pager.first(), AssetSearch())
            assets[0].setAttr("foo", "bar")
            assetService.batchCreateOrReplace(BatchCreateAssetsRequest(assets.list))
            val change = jdbc.queryForMap(
                    "SELECT * FROM auditlog WHERE pk_asset=?::uuid AND int_type=?",
                    assets[0].id, AuditLogType.Changed.ordinal)
            assertEquals(assets[0].id, change["pk_asset"].toString())
            assertEquals(AuditLogType.Changed.ordinal, change["int_type"] as Int)

        } finally {
            System.clearProperty("archivist.auditlog.watched-fields")
        }
    }

    @Test
    fun testBatchCreateOrReplace() {
        val assets = searchService.search(Pager.first(), AssetSearch())
        val rsp = assetService.batchCreateOrReplace(BatchCreateAssetsRequest(assets.list))
        assertEquals(2, rsp.replacedAssetIds.size)
        assertEquals(0, rsp.createdAssetIds.size)
    }

    @Test
    fun testBatchCreateOrReplaceWithLinks() {
        var assets = searchService.search(Pager.first(), AssetSearch())
        assetService.batchCreateOrReplace(BatchCreateAssetsRequest(assets.list))

        val ids = assets.map { it.id }
        val folderId = UUID.randomUUID()
        assertEquals(2, assetService.addLinks(
                LinkType.Folder, folderId, BatchUpdateAssetLinks(ids)).updatedAssetIds.size)


        assetService.batchCreateOrReplace(BatchCreateAssetsRequest(assets.list))
        assets = searchService.search(Pager.first(), AssetSearch())
        assertTrue(assets.size() > 0)
        assets.list.forEach {
            assertTrue(it.attrExists("system.links"))
        }
    }


    @Test
    fun testCreateOrReplace() {
        val asset = searchService.search(Pager.first(), AssetSearch())[0]
        asset.setAttr("foo", "bar")
        val newAsset = assetService.createOrReplace(asset)
        assertEquals(newAsset.getAttr("foo", String::class.java), "bar")
    }

    @Test
    fun testGet() {
        val asset1 = searchService.search(Pager.first(), AssetSearch())[0]
        val asset2 = assetService.get(asset1.id)
        assertEquals(asset1.id, asset2.id)
        assertEquals(asset1.getAttr("source.path", String::class.java),
                asset2.getAttr("source.path", String::class.java))
    }

    @Test
    fun testBatchUpdate() {

        var updates= mutableMapOf<String, UpdateAssetRequest>()
        searchService.search(Pager.first(), AssetSearch()).forEach {doc ->
            updates[doc.id] = UpdateAssetRequest(mapOf("foos" to "ball"))
        }
        val rsp = assetService.batchUpdate(BatchUpdateAssetsRequest(updates))
        assertEquals(2, rsp.updatedAssetIds.size)
        refreshIndex()

        val search =  searchService.search(Pager.first(), AssetSearch())
        assertEquals(2, search.size())
        for (doc: Document in search.list) {
            assertEquals("ball", doc.getAttr("foos", String::class.java))
        }
    }

    @Test
    fun testBatchUpdateRemove() {
        val batch = mutableMapOf<String, Map<String, Any?>>()

        var updates= mutableMapOf<String, UpdateAssetRequest>()
        searchService.search(Pager.first(), AssetSearch()).forEach {doc ->
            updates[doc.id] = UpdateAssetRequest(null, listOf("source"))
        }
        val rsp = assetService.batchUpdate(BatchUpdateAssetsRequest(updates))
        assertEquals(2, rsp.updatedAssetIds.size)
        refreshIndex()

        val search =  searchService.search(Pager.first(), AssetSearch())
        assertEquals(2, search.size())
        for (doc: Document in search.list) {
            assertNull(doc.getAttr("source"))
        }
    }

    @Test
    fun testUpdate() {
        val asset1 = searchService.search(Pager.first(), AssetSearch())[0]
        val asset2 = assetService.update(asset1.id, mapOf("foo" to "bar"))
        assertEquals(asset1.id, asset2.id)
        assertEquals(asset1.getAttr("source.path", String::class.java),
                asset2.getAttr("source.path", String::class.java))
        assertEquals(asset2.getAttr("foo", String::class.java), "bar")
    }

    @Test
    fun testUpdateWithUpdateAssetRequest() {
        val asset1 = searchService.search(Pager.first(), AssetSearch())[0]
        val updated = assetService.update(asset1.id, UpdateAssetRequest(
                mapOf("foo" to "bar"),
                listOf("source.fileSize"))
        )
        assertTrue(updated)
        val asset2 = assetService.get(asset1.id)
        assertEquals(asset2.getAttr("foo", String::class.java), "bar")
        assertNull(asset2.getAttr("source.fileSize"))
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
    fun testBatchDelete() {
        val page = searchService.search(Pager.first(), AssetSearch())
        val ids = page.map { it.id }
        val rsp = assetService.batchDelete(ids)
        assertEquals(0, rsp.errors.size)
        assertEquals(2, rsp.deletedAssetIds.size)
        assertEquals(2, rsp.totalRequested)
    }

    @Test
    fun testAddLinks() {
        val page = searchService.search(Pager.first(), AssetSearch())
        val ids = page.map { it.id }
        val folderId = UUID.randomUUID()
        assertEquals(2, assetService.addLinks(
                LinkType.Folder, folderId, BatchUpdateAssetLinks(ids)).updatedAssetIds.size)
        assertEquals(0, assetService.addLinks(
                LinkType.Folder, folderId, BatchUpdateAssetLinks(ids)).erroredAssetIds.size)
    }

    @Test
    fun testAddChildLinks() {
        val page = searchService.search(Pager.first(), AssetSearch())
        assetService.update(page[0].id, mapOf("media.clip.parent" to page[1].id))
        refreshIndex()

        val folderId = UUID.randomUUID()
        assertEquals(1,
                assetService.addLinks(LinkType.Folder, folderId,
                        BatchUpdateAssetLinks(null, listOf(page[1].id), AssetSearch())).updatedAssetIds.size)
    }

    @Test
    fun testRemoveLinks() {
        val page = searchService.search(Pager.first(), AssetSearch())
        val ids = page.map { it.id }
        val folderId = UUID.randomUUID()
        assertEquals(2, assetService.addLinks(
                LinkType.Folder, folderId, BatchUpdateAssetLinks(ids)).updatedAssetIds.size)
        assertEquals(2, assetService.removeLinks(LinkType.Folder, folderId, ids).updatedAssetIds.size)
        assertEquals(0, assetService.removeLinks(LinkType.Folder, folderId, ids).updatedAssetIds.size)
    }

    @Test
    fun testSetPermissions() {
        val perm = permissionService.getPermission(Groups.ADMIN)
        val result = assetService.setPermissions(
                BatchUpdatePermissionsRequest(AssetSearch(), Acl().addEntry(perm.id, 4)))

        val page = searchService.search(Pager.first(), AssetSearch())
        for (asset in page) {
            val perms = asset.getAttr("system.permissions", PermissionSchema::class.java)
            assertTrue(perms!!.export.contains(perm.id))
        }
    }

    @Test
    fun testSetPermissionsWithReplace() {
        val perm = permissionService.getPermission(Groups.ADMIN)
        val result = assetService.setPermissions(
                BatchUpdatePermissionsRequest(AssetSearch(), Acl().addEntry(perm.id, 4), replace = true))

        val page = searchService.search(Pager.first(), AssetSearch())
        for (asset in page) {
            val perms = asset.getAttr("system.permissions", PermissionSchema::class.java)
            assertTrue(perms!!.export.contains(perm.id))
            assertTrue(perms!!.read.isEmpty())
            assertTrue(perms!!.write.isEmpty())
        }
    }

    @Test
    fun edit() {
        val field = fieldSystemService.createField(FieldSpec("File Ext",
                "source.extension", null, true))

        val page = searchService.search(Pager.first(), AssetSearch())
        for (asset in page) {
            assetService.edit(asset.id, FieldEditSpec(field.id, null,"bilbo"))
        }


    }

    @Test
    fun undo() {
        val field = fieldSystemService.createField(FieldSpec("File Ext",
                "source.extension", null, true))

        val page = searchService.search(Pager.first(), AssetSearch())
        for (asset in page) {
            val edit = assetService.edit(asset.id, FieldEditSpec(field.id, null, "bilbo"))
            assertTrue(assetService.undo(edit))
        }
    }
}