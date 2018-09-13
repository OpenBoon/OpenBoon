package com.zorroa.archivist.repository


import com.google.common.collect.ImmutableList
import com.google.common.collect.Lists
import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.*
import com.zorroa.archivist.security.hasPermission
import com.zorroa.common.domain.Access
import com.zorroa.security.Groups
import org.junit.Assert.*
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import java.io.IOException

class FolderDaoTests : AbstractTest() {

    @Autowired
    lateinit var taxonomyDao: TaxonomyDao

    @Autowired
    lateinit var folderDao: FolderDao

    @Autowired
    lateinit var dyHierarchyDao: DyHierarchyDao

    @Autowired
    lateinit var organizationDao: OrganizationDao

    @Test
    fun testCreateAndGet() {
        val name = "Foobar the folder"
        val builder = FolderSpec(name)
        val (id) = folderDao.create(builder)

        val (_, name1) = folderDao.get(id)
        assertEquals(name1, name)
    }

    @Test
    @Throws(IOException::class)
    fun testChildCount() {
        val name = "Foobar the folder"
        val builder1 = FolderSpec(name)
        var folder1 = folderDao.create(builder1)
        assertEquals(0, folder1.childCount.toLong())

        val builder2 = FolderSpec(name + "2", folder1)
        val folder2 = folderDao.create(builder2)
        folder1 = folderDao.get(folder1.id)
        assertEquals(1, folder1.childCount.toLong())

        folderDao.delete(folder2)
        folder1 = folderDao.get(folder1.id)
        assertEquals(0, folder1.childCount.toLong())
    }

    @Test
    @Throws(IOException::class)
    fun testGetByParentAndName() {
        val name = "test"
        val builder = FolderSpec(name)
        val folder1 = folderDao.create(builder)
        val folder2 = folderDao.get(folderDao.getRootFolder().id, name, false)
        assertEquals(folder1, folder2)
    }

    @Test
    @Throws(IOException::class)
    fun testGetRootFolder() {
        val folder = folderDao.getRootFolder()
        assertNull(folder.parentId)
        assertEquals("/", folder.name)
    }

    @Test
    @Throws(IOException::class)
    fun createRootFolder() {
        val org = organizationDao.create(OrganizationSpec("test"))
        val root = folderDao.createRootFolder(org)
        assertEquals("/", root.name)
        assertNull(root.parentId)
    }

    @Test
    @Throws(IOException::class)
    fun testGetAll() {
        val folder1 = folderDao.create(FolderSpec("test1"))
        val folder2 = folderDao.create(FolderSpec("test2"))
        val folder3 = folderDao.create(FolderSpec("test3"))
        val some = folderDao.getAll(listOf(folder2.id, folder3.id))
        assertTrue(some.contains(folder2))
        assertTrue(some.contains(folder3))
        assertFalse(some.contains(folder1))
        assertTrue(folderDao.getAll(listOf()).isEmpty())
    }

    @Test(expected = DataIntegrityViolationException::class)
    @Throws(IOException::class)
    fun testCreateDuplicate() {
        val name = "Foobar the folder"
        val builder = FolderSpec(name)
        folderDao.create(builder)
        folderDao.create(builder)
    }

    @Test
    fun testGetChildren() {
        val name = "Grandpa"
        var builder = FolderSpec(name)
        val grandpa = folderDao.create(builder)
        builder = FolderSpec("Dad", grandpa)
        val dad = folderDao.create(builder)
        builder = FolderSpec("Uncle", grandpa)
        val (id, name1, parentId, organizationId, dyhiId, user, timeCreated, timeModified, recursive, dyhiRoot, dyhiField, childCount, acl, search, taxonomyRoot, attrs) = folderDao.create(builder)
        builder = FolderSpec("Child", dad)
        val (id1, name2, parentId1, organizationId1, dyhiId1, user1, timeCreated1, timeModified1, recursive1, dyhiRoot1, dyhiField1, childCount1, acl1, search1, taxonomyRoot1, attrs1) = folderDao.create(builder)

        val folders = folderDao.getChildren(grandpa)
        assertEquals(2, folders.size.toLong())
    }

    @Test
    fun testsetDyHierarchyRoot() {
        val name = "Foobar the folder"
        val builder = FolderSpec(name)
        val folder = folderDao.create(builder)
        assertTrue(folderDao.setDyHierarchyRoot(folder, "source.extension"))
    }

    @Test
    fun testSetTaxonomyRoot() {
        val name = "Foobar the folder"
        val builder = FolderSpec(name)
        val folder = folderDao.create(builder)

        val tax = taxonomyDao!!.create(TaxonomySpec(folder))
        assertTrue(folderDao.setTaxonomyRoot(folder, true))
        assertFalse(folderDao.setTaxonomyRoot(folder, true))

        assertTrue(folderDao.setTaxonomyRoot(folder, false))
        assertFalse(folderDao.setTaxonomyRoot(folder, false))
    }

    @Test
    fun removeDyHierarchyRoot() {
        val name = "Foobar the folder"
        val builder = FolderSpec(name)
        val folder = folderDao.create(builder)
        assertTrue(folderDao.setDyHierarchyRoot(folder, "source.extension"))
        assertTrue(folderDao.removeDyHierarchyRoot(folder))
    }


    @Test
    fun testGetChildrenInsecure() {
        authenticate("user")
        assertFalse(hasPermission(Groups.ADMIN))

        val pub = folderDao.get(folderDao.getRootFolder().id, "Users", false)
        val startCount = folderDao.getChildren(pub.id).size
        val (id, name, parentId, organizationId, dyhiId, user, timeCreated, timeModified, recursive, dyhiRoot, dyhiField, childCount, acl, search, taxonomyRoot, attrs) = folderDao.create(FolderSpec("level1", pub))
        val (id1, name1, parentId1, organizationId1, dyhiId1, user1, timeCreated1, timeModified1, recursive1, dyhiRoot1, dyhiField1, childCount1, acl1, search1, taxonomyRoot1, attrs1) = folderDao.create(FolderSpec("level2", pub))
        val f3 = folderDao.create(FolderSpec("level3", pub))
        folderDao.setAcl(f3.id, Acl().addEntry(
                permissionService.getPermission(Groups.ADMIN)))

        assertFalse(folderDao.hasAccess(f3, Access.Read))
        assertEquals(6, folderDao.getChildrenInsecure(pub.id).size.toLong())
        assertEquals((startCount + 2).toLong(), folderDao.getChildren(pub.id).size.toLong())
    }

    @Test
    @Throws(IOException::class)
    fun testHasAccess() {
        authenticate("user")
        val builder = FolderSpec("test")
        val folder1 = folderDao.create(builder)
        assertTrue(folderDao.hasAccess(folder1, Access.Read))

        val p = permissionService.createPermission(PermissionSpec("group", "foo"))
        val acl = Acl()
        acl.addEntry(p, Access.Read)
        folderDao.setAcl(folder1.id, acl)

        assertFalse(folderDao.hasAccess(folder1, Access.Read))
    }

    @Test
    @Throws(IOException::class)
    fun testGetAndSetAcl() {
        val builder = FolderSpec("test")
        val (id) = folderDao.create(builder)

        val p = permissionService.createPermission(PermissionSpec("group", "foo"))
        folderDao.setAcl(id, Acl().addEntry(p, Access.Read))

        val acl = folderDao.getAcl(id)
        assertTrue(acl.hasAccess(p.id, Access.Read))
    }

    @Test
    @Throws(IOException::class)
    fun testGetAndUpdateAcl() {
        val builder = FolderSpec("test")
        val (id) = folderDao.create(builder)

        val p1 = permissionService.createPermission(PermissionSpec("group", "foo"))
        folderDao.setAcl(id, Acl().addEntry(p1, Access.Read))

        var acl = folderDao.getAcl(id)
        assertTrue(acl.hasAccess(p1.id, Access.Read))

        val p2 = permissionService.createPermission(PermissionSpec("group", "bar"))
        folderDao.updateAcl(id, Acl().addEntry(p2, Access.Read))

        acl = folderDao.getAcl(id)
        assertTrue(acl.hasAccess(p1.id, Access.Read))
        assertTrue(acl.hasAccess(p2.id, Access.Read))
    }

    @Test
    @Throws(IOException::class)
    fun testUpdateExitingAcl() {
        val builder = FolderSpec("test")
        val (id) = folderDao.create(builder)

        val p1 = permissionService.createPermission(PermissionSpec("group", "foo"))
        folderDao.setAcl(id, Acl().addEntry(p1, Access.Read))
        folderDao.updateAcl(id, Acl().addEntry(p1, Access.Read, Access.Write))

        val acl = folderDao.getAcl(id)
        assertTrue(acl.hasAccess(p1.id, Access.Read))
        assertTrue(acl.hasAccess(p1.id, Access.Write))
        assertFalse(acl.hasAccess(p1.id, Access.Export))
    }

    @Test(expected = IllegalArgumentException::class)
    @Throws(IOException::class)
    fun testGetAndSetBadAcl() {
        val builder = FolderSpec("test")
        val (id) = folderDao.create(builder)

        val p = permissionService.createPermission(PermissionSpec("group", "foo"))
        val acl = Acl()
        acl.add(AclEntry(p.id, 12))
        folderDao.setAcl(id, acl)
    }

    @Test
    fun testUpdate() {
        val spec = FolderSpec("v1")
        val v1 = folderDao.create(spec)

        val up = FolderUpdate(v1)
        up.name = "v2"

        folderDao.update(v1.id, up)
        val (_, name, _, _, _, _, _, _, _, _, _, _, acl, search) = folderDao.get(v1.id)
        assertEquals("v2", name)
        assertEquals(null, search)
        assertEquals(0, acl!!.size.toLong())
    }

    @Test
    fun testUpdateWithMove() {
        val spec1 = FolderSpec("f1")
        var f1 = folderDao.create(spec1)

        val spec2 = FolderSpec("f2")
        val f2 = folderDao.create(spec2)

        var root = folderDao.get(folderDao.getRootFolder().id)
        val rootCount = root.childCount

        assertEquals(0, f1.childCount.toLong())
        assertEquals(0, f2.childCount.toLong())

        val up = FolderUpdate(f2)
        up.parentId = f1.id

        folderDao.update(f2.id, up)

        f1 = folderDao.get(f1.id)
        assertEquals(1, f1.childCount.toLong())

        root = folderDao.get(folderDao.getRootFolder().id)
        assertEquals((rootCount - 1).toLong(), root.childCount.toLong())
    }

    @Test
    fun testDelete() {
        val count = folderDao.getChildren(folderDao.getRootFolder().id).size
        val f = folderDao.create(FolderSpec("test"))
        assertTrue(folderDao.delete(f))
        assertEquals(count.toLong(), folderDao.getChildren(folderDao.getRootFolder().id).size.toLong())
    }

    @Test
    fun testCount() {
        val count = folderDao.count()
        val (id, name, parentId, organizationId, dyhiId, user, timeCreated, timeModified, recursive, dyhiRoot, dyhiField, childCount, acl, search, taxonomyRoot, attrs) = folderDao.create(FolderSpec("test"))
        assertEquals((count + 1).toLong(), folderDao.count().toLong())
    }

    @Test
    fun testExists() {
        var builder = FolderSpec("foo")
        val folder1 = folderDao.create(builder)
        assertTrue(folderDao.exists(folderDao.getRootFolder().id, "foo"))

        builder = FolderSpec("bar", folder1)
        val (id, name, parentId, organizationId, dyhiId, user, timeCreated, timeModified, recursive, dyhiRoot, dyhiField, childCount, acl, search, taxonomyRoot, attrs) = folderDao.create(builder)
        assertTrue(folderDao.exists(folder1.id, "bar"))
    }

    @Test
    fun testGetAllIds() {
        val spec1 = FolderSpec("foo")
        val (id) = folderDao.create(spec1)

        val spec = DyHierarchySpec().setFolderId(id)
        spec.levels = ImmutableList.of(
                DyHierarchyLevel("source.type.raw"),
                DyHierarchyLevel("source.extension.raw"))
        val dyhi = dyHierarchyDao.create(spec)

        val spec2 = FolderSpec("bar")
        spec2.dyhiId = dyhi.id
        val (id1) = folderDao.create(spec2)
        val ids = folderDao.getAllIds(dyhi)
        assertTrue(ids.contains(id1))
    }

    @Test
    fun testDeleteAllById() {

        val spec1 = FolderSpec("foo")
        val (id) = folderDao.create(spec1)

        val spec2 = FolderSpec("bar")
        val (id1) = folderDao.create(spec2)

        assertEquals(2, folderDao.deleteAll(Lists.newArrayList(id, id1)).toLong())
    }
}
