package com.zorroa.archivist.service

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSet
import com.google.common.collect.Lists
import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.*
import com.zorroa.archivist.repository.FolderDao
import com.zorroa.archivist.repository.OrganizationDao
import com.zorroa.archivist.search.AssetFilter
import com.zorroa.archivist.search.AssetSearch
import com.zorroa.archivist.security.SuperAdminAuthentication
import com.zorroa.common.domain.ArchivistWriteException
import com.zorroa.security.Groups
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.security.core.context.SecurityContextHolder
import java.util.*

class FolderServiceTests : AbstractTest() {

    @Autowired
    lateinit var dyhiService: DyHierarchyService

    @Autowired
    lateinit var folderDao: FolderDao

    @Autowired
    lateinit var taxonomyService: TaxonomyService

    @Autowired
    lateinit var organizationDao: OrganizationDao

    @Before
    fun init() {
        addTestAssets("set04/standard")
        refreshIndex()
    }

    @Test
    fun testGet() {
        val root = UUID.fromString("00000000-0000-0000-0000-000000000000")
        val folder = folderService.get(root)
        assertEquals("/", folder.name)
    }

    @Test
    fun testSetAssets() {

        val folders = Lists.newArrayList<UUID>()
        for (i in 0..9) {
            val builder = FolderSpec("Folder$i")
            val (id) = folderService.create(builder)
            folders.add(id)
        }

        val assets = indexService.getAll(Pager.first(1)).list
        assertEquals(1, assets.size.toLong())
        var doc = assets[0]

        folderService.setFoldersForAsset(doc.id, folders)
        refreshIndex()

        doc = indexService.get(doc.id)
        assertEquals(10, doc.getAttr("system.links.folder", List::class.java).size.toLong())
    }

    @Test
    fun testAddAssetToFolder() {

        val builder = FolderSpec("Folder")
        val folder = folderService.create(builder)

        val results = folderService.addAssets(folder, indexService.getAll(
                Pager.first()).map { a -> a.id }.toList())

        assertTrue(results["failed"]!!.isEmpty())
        assertFalse(results["success"]!!.isEmpty())
    }

    @Test
    fun testAddDuplicateAssetsToFolder() {

        val builder = FolderSpec("Folder")
        val folder = folderService.create(builder)

        folderService.addAssets(folder, indexService.getAll(
                Pager.first()).map { a -> a.id }.toList())

        refreshIndex()

        folderService.addAssets(folder, indexService.getAll(
                Pager.first()).map { a -> a.id }.toList())

        refreshIndex()

        val assets = indexService.getAll(Pager.first())
        for (a in assets) {
            assertEquals(1, (a.getAttr<Any>("system.links.folder") as List<*>).size.toLong())
        }
    }

    @Test
    fun testRemoveAssetFromFolder() {

        val builder = FolderSpec("Folder")
        val folder = folderService.create(builder)

        var results = folderService.addAssets(folder, indexService.getAll(
                Pager.first()).map { a -> a.id }.toList())

        assertTrue(results["failed"]!!.isEmpty())
        assertFalse(results["success"]!!.isEmpty())

        results = folderService.removeAssets(folder, indexService.getAll(
                Pager.first()).map { a -> a.id }.toList())
        assertTrue(results["failed"]!!.isEmpty())
        assertFalse(results["success"]!!.isEmpty())
    }

    @Test
    fun testAddAssetToTaxonomyFolder() {

        val builder = FolderSpec("bilbo")
        var folder = folderService.create(builder)
        taxonomyService!!.create(TaxonomySpec(folder))
        folder = folderService.get(folder.id)

        val results = folderService.addAssets(folder, indexService.getAll(
                Pager.first()).map { a -> a.id }.toList())
        refreshIndex(2000)

        assertEquals(2, searchService.count(AssetSearch().setQuery("bilbo")))
    }

    @Test
    fun testRemoveAssetFromTaxonomyFolder() {

        val builder = FolderSpec("Folder")
        var folder = folderService.create(builder)
        taxonomyService!!.create(TaxonomySpec(folder))
        folder = folderService.get(folder.id)

        var results = folderService.addAssets(folder, indexService.getAll(
                Pager.first()).map { a -> a.id })
        refreshIndex()

        assertEquals(2, searchService.search(AssetSearch(
                AssetFilter().addToTerms("system.links.folder", folder.id))).hits.getTotalHits())
        fieldService.invalidateFields()
        refreshIndex()
        assertEquals(2, searchService.search(AssetSearch("Folder")).hits.getTotalHits())

        assertTrue(results["failed"]!!.isEmpty())
        assertFalse(results["success"]!!.isEmpty())

        results = folderService.removeAssets(folder, indexService.getAll(
                Pager.first()).map { a -> a.id }.toList())
        assertTrue(results["failed"]!!.isEmpty())
        assertFalse(results["success"]!!.isEmpty())
        refreshIndex()

        assertEquals(0, searchService.search(AssetSearch(
                AssetFilter().addToTerms("system.links.folder", folder.id))).hits.getTotalHits())
        assertEquals(0, searchService.search(AssetSearch("Folder")).hits.getTotalHits())

    }

    @Test
    fun testCountAssetSmartFolder() {
        val builder = FolderSpec("Folder")
        builder.search = AssetSearch("jpg")

        val (id) = folderService.create(builder)
        assertEquals(2, searchService.count(folderService.get(id)))

        val builder2 = FolderSpec("Folder2")
        builder2.search = AssetSearch("wdsdsdsdsds")
        val (id1) = folderService.create(builder2)

        assertEquals(0, searchService.count(folderService.get(id1)))
    }

    @Test
    fun testSetAcl() {
        val builder = FolderSpec("Folder")
        val folder = folderService.create(builder)
        folderService.get(folder.id)

        folderService.setAcl(folder, Acl().addEntry(
                permissionService.getPermission(Groups.MANAGER),
                Access.Read, Access.Write, Access.Export), false, false)
        folderService.get(folder.id)
    }

    @Test(expected = ArchivistWriteException::class)
    fun testSetAclFailure() {
        authenticate("librarian")
        val builder = FolderSpec("Folder")
        val folder = folderService.create(builder)
        folderService.get(folder.id)

        /**
         * Since we have all permissions now, this should fail because we
         * are taking away write/export permissions from ourself.
         */
        folderService.setAcl(folder, Acl().addEntry(
                permissionService.getPermission(Groups.LIBRARIAN),
                Access.Read), false, false)
        folderService.get(folder.id)
    }

    @Test(expected = ArchivistWriteException::class)
    fun testSetAclFailureDoesntHavePermission() {
        authenticate("librarian")
        val builder = FolderSpec("Folder")
        val folder = folderService.create(builder)
        folderService.get(folder.id)

        /**
         * Since we have all permissions now, this should fail because we
         * are taking away write/export permissions from ourself.
         */
        folderService.setAcl(folder, Acl().addEntry(
                permissionService.getPermission(Groups.ADMIN),
                Access.Read), false, false)
        folderService.get(folder.id)
    }

    @Test(expected = ArchivistWriteException::class)
    fun testGetFolderWithoutAcl() {
        authenticate("librarian")
        val builder = FolderSpec("Folder")
        val folder = folderService.create(builder)
        folderService.get(folder.id)

        // use the DAO so don't fail the remove access from self check.
        folderDao.setAcl(folder.id, Acl().addEntry(
                permissionService.getPermission(Groups.ADMIN), Access.Read))
        folderService.invalidate(folder)
        folderService.get(folder.id)
    }

    @Test(expected = EmptyResultDataAccessException::class)
    fun testCreateWithReadAcl() {
        authenticate("librarian")
        val builder = FolderSpec("Folder", folderService.get("/Library")!!)
        val folder = folderService.create(builder)
        folderDao.setAcl(folder.id,
                Acl().addEntry(permissionService.getPermission(Groups.ADMIN), Access.Read))
        folderService.invalidate(folder)
        folderService.get(folder.id)
    }

    @Test(expected = ArchivistWriteException::class)
    fun testAddAssetsWithWriteAcl() {
        authenticate("librarian")
        val builder = FolderSpec("Folder", folderService.get("/Library")!!)
        val folder = folderService.create(builder)
        val acl = Acl().addEntry(permissionService.getPermission(Groups.ADMIN), Access.Write)
        folderService.setAcl(folder, acl, false, false)
        folderService.addAssets(folderService.get(folder), indexService.getAll(
                Pager.first()).map { a -> a.id }.toList())
    }

    @Test(expected = ArchivistWriteException::class)
    fun testDeleteFolderWithWriteAcl() {
        authenticate("librarian")
        val builder = FolderSpec("Folder", folderService.get("/Library")!!)
        val folder = folderService.create(builder)
        val acl = Acl().addEntry(permissionService.getPermission(Groups.ADMIN), Access.Write)
        folderService.setAcl(folder, acl, false, false)
        folderService.delete(folderService.get(folder))
    }

    @Test(expected = ArchivistWriteException::class)
    fun testUpdateFolderWithWriteAcl() {
        authenticate("librarian")
        val builder = FolderSpec("Folder", folderService.get("/Library")!!)
        val folder = folderService.create(builder)
        folderDao.setAcl(folder.id,
                Acl().addEntry(permissionService.getPermission(Groups.ADMIN), Access.Write))
        val up = FolderUpdate(folder)
        up.name = "bilbo"
        folderService.update(folder.id, up)
    }

    @Test
    fun testRenameUserFolder() {
        authenticate("librarian")
        val user = userService.get("librarian")
        assertFalse(folderService.exists("/Users/foo"))
        assertTrue(folderService.renameUserFolder(user, "foo"))
        assertTrue(folderService.exists("/Users/foo"))
    }

    @Test
    fun testCreateAndGet() {
        val builder = FolderSpec("Da Kind Assets")
        val (id, name) = folderService.create(builder)

        val (_, name1) = folderService.get(id)
        assertEquals(name, name1)
    }

    @Test
    fun testDescendants() {
        val grandpa = folderService.create(FolderSpec("grandpa"))
        val dad = folderService.create(FolderSpec("dad", grandpa))
        val uncle = folderService.create(FolderSpec("uncle", grandpa))
        folderService.create(FolderSpec("child", dad))
        folderService.create(FolderSpec("cousin", uncle))
        val descendents = folderService.getAllDescendants(grandpa, false)
        assertEquals(4, descendents.size.toLong())
    }

    @Test
    fun testGetAllDescendants() {
        val grandpa = folderService.create(FolderSpec("grandpa"))
        val dad = folderService.create(FolderSpec("dad", grandpa))
        val uncle = folderService.create(FolderSpec("uncle", grandpa))
        folderService.create(FolderSpec("child", dad))
        folderService.create(FolderSpec("cousin", uncle))
        assertEquals(5, folderService.getAllDescendants(Lists.newArrayList(grandpa), true, true).size.toLong())
        assertEquals(4, folderService.getAllDescendants(Lists.newArrayList(grandpa), false, true).size.toLong())

        assertEquals(5, ImmutableSet.copyOf(folderService.getAllDescendants(
                Lists.newArrayList(grandpa, dad, uncle), true, true)).size.toLong())
        assertEquals(4, ImmutableSet.copyOf(folderService.getAllDescendants(
                Lists.newArrayList(grandpa, dad, uncle), false, true)).size.toLong())
    }

    @Test
    fun testGetChildren() {
        val folder1 = folderService.create(FolderSpec("test1"))
        val folder1a = folderService.create(FolderSpec("test1a", folder1))
        val folder1b = folderService.create(FolderSpec("test1b", folder1))
        val folder1c = folderService.create(FolderSpec("test1c", folder1))

        val children = folderService.getChildren(folder1)
        assertEquals(3, children.size.toLong())
        assertTrue(children.contains(folder1a))
        assertTrue(children.contains(folder1b))
        assertTrue(children.contains(folder1c))
        assertFalse(children.contains(folder1))
    }

    @Test
    fun testGetByPath() {
        val folder1 = folderService.create(FolderSpec("test1"))
        val folder1a = folderService.create(FolderSpec("test1a", folder1))
        val folder1b = folderService.create(FolderSpec("test1b", folder1a))
        val (_, name) = folderService.create(FolderSpec("test1c", folder1b))

        val folder = folderService.get("/test1/test1a/test1b/test1c")
        assertEquals(folder1b.id, folder!!.parentId)
        assertEquals(name, folder.name)
    }

    @Test(expected = EmptyResultDataAccessException::class)
    fun testGetByPathFail() {
        val folder = folderService.get("/foo/bar/bam")
        assertEquals(null, folder)
    }

    @Test
    fun testExistsByPath() {
        val folder1 = folderService.create(FolderSpec("test1"))
        val folder1a = folderService.create(FolderSpec("test1a", folder1))
        val folder1b = folderService.create(FolderSpec("test1b", folder1a))
        folderService.create(FolderSpec("test1c", folder1b))

        assertTrue(folderService.exists("/test1/test1a/test1b/test1c"))
        assertTrue(folderService.exists("/test1/test1a"))
        assertFalse(folderService.exists("/testb"))
        assertFalse(folderService.exists("/testb/test123"))
    }

    @Test
    fun testUpdate() {
        val folder = folderService.create(FolderSpec("orig"))
        val up = FolderUpdate(folder)
        up.name = "new"
        val ok = folderService.update(folder.id, up)
        assertTrue(ok)
        val (_, name) = folderService.get(folder.id)
        assertEquals("new", name)
    }

    @Test
    fun testUpdateWithNewParent() {

        val fs1 = FolderSpec("orig")
        val fs2 = FolderSpec("unorig")

        val folder1 = folderService.create(fs1)
        val folder2 = folderService.create(fs2)
        val up = FolderUpdate(folder2)
        up.parentId = folder1.id
        val ok = folderService.update(folder2.id, up)
        assertTrue(ok)

        val (_, _, parentId) = folderService.get(folder2.id)
        assertEquals(folder1.id, parentId)

        var folders = folderService.getAllDescendants(Lists.newArrayList(folder1), false, false)
        assertTrue(folders.contains(folder2))

        folders = folderService.getAllDescendants(Lists.newArrayList(folder2), false, false)
        assertTrue(folders.isEmpty())
    }

    @Test
    fun testUpdateWithNewTaxonomyParent() {

        assertEquals(0, searchService.count(AssetSearch().setQuery("bilbo")))

        val builder1 = FolderSpec("bilbo")
        var folder1 = folderService.create(builder1)
        taxonomyService!!.create(TaxonomySpec(folder1))
        folder1 = folderService.get(folder1.id)

        val builder2 = FolderSpec("baggins")
        val folder2 = folderService.create(builder2)

        val results = folderService.addAssets(folder2, indexService.getAll(
                Pager.first()).map { a -> a.id }.toList())
        refreshIndex(1000)

        assertEquals(0, searchService.count(AssetSearch().setQuery("bilbo")))

        val update = FolderUpdate(folder2)
        update.parentId = folder1.id

        folderService.update(folder2.id, update)
        refreshIndex(1000)

        assertEquals(2, searchService.count(AssetSearch().setQuery("bilbo")))
        assertEquals(2, searchService.count(AssetSearch().setQuery("baggins")))
    }

    @Test
    fun testCreateFolderInheritedPermissions() {
        val library = folderService.get("/Library")
        val (_, _, _, _, _, _, _, _, _, _, _, _, acl) = folderService.create(FolderSpec("orig", library!!))

        assertTrue(acl!!.hasAccess(permissionService.getPermission(Groups.LIBRARIAN), Access.Write))
        assertTrue(acl.hasAccess(permissionService.getPermission(Groups.EVERYONE), Access.Read))
        assertEquals(2, acl.size.toLong())
    }

    @Test
    fun testMoveFolderWithPermmissionChange() {

        val library = folderService.get("/Library")
        val admin = folderService.get("/Users/admin")
        val moving = folderService.create(FolderSpec("folder_to_move", admin!!))

        assertTrue(moving.acl!!.hasAccess(permissionService.getPermission("user::admin"), Access.Read))
        assertTrue(moving.acl!!.hasAccess(permissionService.getPermission("user::admin"), Access.Write))
        assertEquals(1, moving.acl!!.size.toLong())

        val up = FolderUpdate(moving)
        up.parentId = library!!.id

        // Move the folder into the library
        assertTrue(folderService.update(moving.id, up))
        val acl = folderDao.getAcl(moving.id)

        assertTrue(acl.hasAccess(permissionService.getPermission(Groups.LIBRARIAN), Access.Write))
        assertTrue(acl.hasAccess(permissionService.getPermission(Groups.EVERYONE), Access.Read))
        assertEquals(2, acl.size.toLong())
    }

    @Test(expected = ArchivistWriteException::class)
    fun testUpdateHierarchyFailure() {
        val folder1 = folderService.create(FolderSpec("test3"))
        val folder1a = folderService.create(FolderSpec("test3a", folder1))
        val folder1b = folderService.create(FolderSpec("test3b", folder1a))
        val (id) = folderService.create(FolderSpec("test3c", folder1b))

        val up = FolderUpdate(folder1)
        up.parentId = id

        folderService.update(folder1.id, up)
    }

    @Test(expected = ArchivistWriteException::class)
    fun testUpdateHierarchyFailureSelfAsParent() {
        val folder1 = folderService.create(FolderSpec("test2"))
        val folder1a = folderService.create(FolderSpec("test2a", folder1))
        val folder1b = folderService.create(FolderSpec("test2b", folder1a))
        val (id, name, parentId, organizationId, dyhiId, user, timeCreated, timeModified, recursive, dyhiRoot, dyhiField, childCount, acl, search, taxonomyRoot, attrs) = folderService.create(FolderSpec("test2c", folder1b))
        val up = FolderUpdate(folder1)
        up.parentId = folder1.id
        folderService.update(folder1.id, up)
    }

    @Test
    fun testUpdateRecursive() {
        val folder = folderService.create(FolderSpec("orig"))
        assertTrue(folder.recursive)
        val updated = FolderUpdate(folder)
        updated.recursive = false
        val ok = folderService.update(folder.id, updated)
        assertTrue(ok)
        val updatedFolder = folderService.get(folder.id)
        assertFalse(updatedFolder.recursive)
    }

    @Test
    fun testDelete() {
        var builder = FolderSpec("shizzle")
        var start = folderService.create(builder)
        val root = start
        for (i in 0..9) {
            builder = FolderSpec("shizzle$i", start)
            start = folderService.create(builder)
        }
        assertTrue(folderService.delete(root))
    }

    @Test
    fun testDeleteWithDyhi() {
        val folder = folderService.create(FolderSpec("foo"), false)
        val spec = DyHierarchySpec()
        spec.folderId = folder.id
        spec.levels = ImmutableList.of(
                DyHierarchyLevel("source.date", DyHierarchyLevelType.Day))
        dyhiService!!.create(spec)
        assertTrue(folderService.delete(folder))
    }

    @Test
    fun setDyHierarchyTest() {
        var folder = folderService.create(FolderSpec("root"))
        folderService.setDyHierarchyRoot(folder, "source.file")

        folder = folderService.get(folder.id)
        assertTrue(folder.search!!.filter.exists.contains("source.file"))
        assertTrue(folder.dyhiRoot)
    }

    @Test
    fun removeDyHierarchyTest() {
        var folder = folderService.create(FolderSpec("root"))
        folderService.setDyHierarchyRoot(folder, "source.file")

        folder = folderService.get(folder.id)
        assertTrue(folder.search!!.filter.exists.contains("source.file"))

        folderService.removeDyHierarchyRoot(folder)
        folder = folderService.get(folder.id)
        assertNull(folder.search)
        assertFalse(folder.dyhiRoot)
    }

    @Test
    fun testTrashFolder() {
        val count = folderService.count()

        val folder1 = folderService.create(FolderSpec("folder1"))
        val (id, name, parentId, organizationId, dyhiId, user, timeCreated, timeModified, recursive, dyhiRoot, dyhiField, childCount, acl, search, taxonomyRoot, attrs) = folderService.create(FolderSpec("folder2", folder1))
        assertEquals((count + 2).toLong(), folderService.count().toLong())

        val result = folderService.trash(folder1)

        // Deleted 2 folders
        assertEquals(2, result.count.toLong())
        // count back to normal
        assertEquals(count.toLong(), folderService.count().toLong())
    }

    @Test
    fun getTrashedFolder() {
        val folder1 = folderService.create(FolderSpec("folder1"))
        val result = folderService.trash(folder1)
        val tf = folderService.getTrashedFolder(result.trashFolderId)
    }

    @Test
    fun restoreFolder() {

        val folder1 = folderService.create(FolderSpec("folder1"))
        val folder2 = folderService.create(FolderSpec("folder2", folder1))
        val (id, name, parentId, organizationId, dyhiId, user, timeCreated, timeModified, recursive, dyhiRoot, dyhiField, childCount, acl, search, taxonomyRoot, attrs) = folderService.create(FolderSpec("folder3", folder2))
        val result = folderService.trash(folder1)

        // Deleted 2 folders
        assertEquals(3, result.count.toLong())

        assertEquals(3, folderService.restore(
                folderService.getTrashedFolder(result.trashFolderId)).count.toLong())
    }

    @Test
    fun emptyTrash() {

        val folder1 = folderService.create(FolderSpec("folder1"))
        val folder2 = folderService.create(FolderSpec("folder2", folder1))
        val (id, name, parentId, organizationId, dyhiId, user, timeCreated, timeModified, recursive, dyhiRoot, dyhiField, childCount, acl, search, taxonomyRoot, attrs) = folderService.create(FolderSpec("folder3", folder2))
        val result = folderService.trash(folder1)

        // Deleted 2 folders
        assertEquals(3, result.count.toLong())
        assertEquals(3, folderService.emptyTrash().size.toLong())

        assertEquals(0, (jdbc.queryForObject("SELECT COUNT(1) FROM folder_trash", Int::class.java) as Int).toLong())
    }

    @Test
    fun isDescendantOf() {

        val folder1 = folderService.create(FolderSpec("folder1"))
        val folder2 = folderService.create(FolderSpec("folder2", folder1))
        val folder3 = folderService.create(FolderSpec("folder3", folder2))

        assertTrue(folderService.isDescendantOf(folder3, folder1))
        assertFalse(folderService.isDescendantOf(folder1, folder3))
        //assertTrue(folderService.isDescendantOf(folder3, folderService.get(getRootFolderId())))

    }

    @Test
    fun getAnscestors() {

        val folder1 = folderService.create(FolderSpec("f1"))
        val folder2 = folderService.create(FolderSpec("f2", folder1))
        val folder3 = folderService.create(FolderSpec("f3", folder2))

        val folders = folderService.getAllAncestors(folder3, true, false)
    }

    @Test
    fun getFolderByPathWithSpaces() {
        val folder1 = folderService.create(FolderSpec("  f1  "))
        val folder2 = folderService.get("/  f1  ")
        assertEquals(folder1, folder2)
    }

    @Test
    fun testCreateUserFolder() {
        val spec = PermissionSpec("group::wizards")
        val perm = permissionService.createPermission(spec)
        val folder = folderService.createUserFolder("gandalf", perm)
        assertTrue(folder.acl!!.hasAccess(permissionService.getPermission(Groups.EVERYONE), Access.Read))
    }

    @Test
    fun createStandardfolders() {
        val org = organizationDao.create(OrganizationSpec("test"))
        SecurityContextHolder.getContext().authentication = SuperAdminAuthentication(org.id)
        permissionService.createStandardPermissions(org)
        folderService.createStandardFolders(org)

        assertTrue(folderService.exists("/Users"))
        assertTrue(folderService.exists("/Library"))
    }

}
