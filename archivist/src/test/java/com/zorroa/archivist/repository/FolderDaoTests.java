package com.zorroa.archivist.repository;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.zorroa.archivist.AbstractTest;
import com.zorroa.archivist.domain.*;
import com.zorroa.security.Groups;
import com.zorroa.archivist.security.UtilsKt;
import com.zorroa.archivist.service.PermissionService;
import com.zorroa.sdk.domain.Access;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static com.zorroa.archivist.domain.FolderKt.getRootFolderId;
import static org.junit.Assert.*;

public class FolderDaoTests extends AbstractTest {

    @Autowired
    TaxonomyDao taxonomyDao;

    @Autowired
    FolderDao folderDao;

    @Autowired
    DyHierarchyDao dyHierarchyDao;

    @Autowired
    PermissionService permissionService;

    @Test
    public void testCreateAndGet()  {
        String name = "Foobar the folder";
        FolderSpec builder = new FolderSpec(name);
        Folder folder1 = folderDao.create(builder);

        Folder folder2 = folderDao.get(folder1.getId());
        assertEquals(folder2.getName(), name);
    }

    @Test
    public void testChildCount() throws IOException {
        String name = "Foobar the folder";
        FolderSpec builder1 = new FolderSpec(name);
        Folder folder1 = folderDao.create(builder1);
        assertEquals(0, folder1.getChildCount());

        FolderSpec builder2 = new FolderSpec(name + "2", folder1);
        Folder folder2 = folderDao.create(builder2);
        folder1 = folderDao.get(folder1.getId());
        assertEquals(1, folder1.getChildCount());

        folderDao.delete(folder2);
        folder1 = folderDao.get(folder1.getId());
        assertEquals(0, folder1.getChildCount());
    }

    @Test
    public void testGetByParentAndName() throws IOException {
        String name = "test";
        FolderSpec builder = new FolderSpec(name);
        Folder folder1 = folderDao.create(builder);
        Folder folder2 = folderDao.get(getRootFolderId(), name, false);
        assertEquals(folder1, folder1);
    }

    @Test
    public void testGetRootFolder() throws IOException {
        Folder folder2 = folderDao.get(getRootFolderId());
        assertNull(folder2.getParentId());
    }

    @Test
    public void testGetAll() throws IOException {
        Folder folder1 = folderDao.create(new FolderSpec("test1"));
        Folder folder2 = folderDao.create(new FolderSpec("test2"));
        Folder folder3 = folderDao.create(new FolderSpec("test3"));
        List<Folder> some = folderDao.getAll(
                Lists.newArrayList(folder2.getId(), folder3.getId()));
        assertTrue(some.contains(folder2));
        assertTrue(some.contains(folder3));
        assertFalse(some.contains(folder1));
    }

    @Test(expected=DataIntegrityViolationException.class)
    public void testCreateDuplicate() throws IOException {
        String name = "Foobar the folder";
        FolderSpec builder = new FolderSpec(name);
        folderDao.create(builder);
        folderDao.create(builder);
    }

    @Test
    public void testGetChildren() {
        String name = "Grandpa";
        FolderSpec builder = new FolderSpec(name);
        Folder grandpa = folderDao.create(builder);
        builder = new FolderSpec("Dad", grandpa);
        Folder dad = folderDao.create(builder);
        builder = new FolderSpec("Uncle", grandpa);
        Folder uncle = folderDao.create(builder);
        builder = new FolderSpec("Child", dad);
        Folder child = folderDao.create(builder);

        List<Folder> folders = folderDao.getChildren(grandpa);
        assertEquals(2, folders.size());
    }

    @Test
    public void testsetDyHierarchyRoot() {
        String name = "Foobar the folder";
        FolderSpec builder = new FolderSpec(name);
        Folder folder = folderDao.create(builder);
        assertTrue(folderDao.setDyHierarchyRoot(folder, "source.extension"));
    }

    @Test
    public void testSetTaxonomyRoot() {
        String name = "Foobar the folder";
        FolderSpec builder = new FolderSpec(name);
        Folder folder = folderDao.create(builder);

        Taxonomy tax = taxonomyDao.create(new TaxonomySpec(folder));
        assertTrue(folderDao.setTaxonomyRoot(folder, true));
        assertFalse(folderDao.setTaxonomyRoot(folder, true));

        assertTrue(folderDao.setTaxonomyRoot(folder, false));
        assertFalse(folderDao.setTaxonomyRoot(folder, false));
    }

    @Test
    public void removeDyHierarchyRoot() {
        String name = "Foobar the folder";
        FolderSpec builder = new FolderSpec(name);
        Folder folder = folderDao.create(builder);
        assertTrue(folderDao.setDyHierarchyRoot(folder, "source.extension"));
        assertTrue(folderDao.removeDyHierarchyRoot(folder));
    }


    @Test
    public void testGetChildrenInsecure() {
        authenticate("user");
        assertFalse(UtilsKt.hasPermission(Groups.ADMIN));

        Folder pub = folderDao.get(getRootFolderId(), "Users", false);
        int startCount =  folderDao.getChildren(pub.getId()).size();
        Folder f1 = folderDao.create(new FolderSpec("level1", pub));
        Folder f2 = folderDao.create(new FolderSpec("level2", pub));
        Folder f3 = folderDao.create(new FolderSpec("level3", pub));
        folderDao.setAcl(f3.getId(), new Acl().addEntry(
                permissionService.getPermission(Groups.ADMIN)));

        assertFalse(folderDao.hasAccess(f3, Access.Read));
        assertEquals(6, folderDao.getChildrenInsecure(pub.getId()).size());
        assertEquals(startCount+2, folderDao.getChildren(pub.getId()).size());
    }

    @Test
    public void testHasAccess() throws IOException {
        authenticate("user");
        FolderSpec builder = new FolderSpec("test");
        Folder folder1 = folderDao.create(builder);
        assertTrue(folderDao.hasAccess(folder1, Access.Read));

        Permission p = permissionService.createPermission(new PermissionSpec("group", "foo"));
        Acl acl = new Acl();
        acl.addEntry(p, Access.Read);
        folderDao.setAcl(folder1.getId(), acl);

        assertFalse(folderDao.hasAccess(folder1, Access.Read));
    }

    @Test
    public void testGetAndSetAcl() throws IOException {
        FolderSpec builder = new FolderSpec("test");
        Folder folder1 = folderDao.create(builder);

        Permission p = permissionService.createPermission(new PermissionSpec("group","foo"));
        folderDao.setAcl(folder1.getId(), new Acl().addEntry(p, Access.Read));

        Acl acl = folderDao.getAcl(folder1.getId());
        assertTrue(acl.hasAccess(p.getId(), Access.Read));
    }

    @Test
    public void testGetAndUpdateAcl() throws IOException {
        FolderSpec builder = new FolderSpec("test");
        Folder folder1 = folderDao.create(builder);

        Permission p1 = permissionService.createPermission(new PermissionSpec("group","foo"));
        folderDao.setAcl(folder1.getId(), new Acl().addEntry(p1, Access.Read));

        Acl acl = folderDao.getAcl(folder1.getId());
        assertTrue(acl.hasAccess(p1.getId(), Access.Read));

        Permission p2 = permissionService.createPermission(new PermissionSpec("group","bar"));
        folderDao.updateAcl(folder1.getId(), new Acl().addEntry(p2, Access.Read));

        acl = folderDao.getAcl(folder1.getId());
        assertTrue(acl.hasAccess(p1.getId(), Access.Read));
        assertTrue(acl.hasAccess(p2.getId(), Access.Read));
    }

    @Test
    public void testUpdateExitingAcl() throws IOException {
        FolderSpec builder = new FolderSpec("test");
        Folder folder1 = folderDao.create(builder);

        Permission p1 = permissionService.createPermission(new PermissionSpec("group","foo"));
        folderDao.setAcl(folder1.getId(), new Acl().addEntry(p1, Access.Read));
        folderDao.updateAcl(folder1.getId(), new Acl().addEntry(p1, Access.Read, Access.Write));

        Acl acl = folderDao.getAcl(folder1.getId());
        assertTrue(acl.hasAccess(p1.getId(), Access.Read));
        assertTrue(acl.hasAccess(p1.getId(), Access.Write));
        assertFalse(acl.hasAccess(p1.getId(), Access.Export));
    }

    @Test(expected=IllegalArgumentException.class)
    public void testGetAndSetBadAcl() throws IOException {
        FolderSpec builder = new FolderSpec("test");
        Folder folder1 = folderDao.create(builder);

        Permission p = permissionService.createPermission(new PermissionSpec("group","foo"));
        Acl acl = new Acl();
        acl.add(new AclEntry(p.getId(), 12));
        folderDao.setAcl(folder1.getId(), acl);
    }

    @Test
    public void testUpdate() {
        FolderSpec spec = new FolderSpec("v1");
        Folder v1 = folderDao.create(spec);

        FolderUpdate up = new FolderUpdate(v1);
        up.setName("v2");

        folderDao.update(v1.getId(), up);
        Folder v2 = folderDao.get(v1.getId());
        assertEquals("v2", v2.getName());
        assertEquals(null, v2.getSearch());
        assertEquals(0, v2.getAcl().size());
    }

    @Test
    public void testUpdateWithMove() {
        FolderSpec spec1 = new FolderSpec("f1");
        Folder f1 = folderDao.create(spec1);

        FolderSpec spec2 = new FolderSpec("f2");
        Folder f2 = folderDao.create(spec2);

        Folder root = folderDao.get(getRootFolderId());
        int rootCount = root.getChildCount();

        assertEquals(0, f1.getChildCount());
        assertEquals(0, f2.getChildCount());

        FolderUpdate up = new FolderUpdate(f2);
        up.setParentId(f1.getId());

        folderDao.update(f2.getId(), up);

        f1 = folderDao.get(f1.getId());
        assertEquals(1, f1.getChildCount());

        root = folderDao.get(getRootFolderId());
        assertEquals(rootCount -1, root.getChildCount());
    }

    @Test
    public void testDelete() {
        int count = folderDao.getChildren(getRootFolderId()).size();
        Folder f = folderDao.create(new FolderSpec("test"));
        assertTrue(folderDao.delete(f));
        assertEquals(count, folderDao.getChildren(getRootFolderId()).size());
    }

    @Test
    public void testCount() {
        int count = folderDao.count();
        Folder f = folderDao.create(new FolderSpec("test"));
        assertEquals(count+1, folderDao.count());
    }

    @Test
    public void testExists() {
        FolderSpec builder = new FolderSpec("foo");
        Folder folder1 = folderDao.create(builder);
        assertTrue(folderDao.exists(getRootFolderId(), "foo"));

        builder = new FolderSpec("bar", folder1);
        Folder folder2 = folderDao.create(builder);
        assertTrue(folderDao.exists(folder1.getId(), "bar"));
    }

    @Test
    public void testGetAllIds() {
        FolderSpec spec1 = new FolderSpec("foo");
        Folder folder1 = folderDao.create(spec1);

        DyHierarchySpec spec = new DyHierarchySpec().setFolderId(folder1.getId());
        spec.setLevels(
                ImmutableList.of(
                        new DyHierarchyLevel("source.type.raw"),
                        new DyHierarchyLevel("source.extension.raw")));
        DyHierarchy dyhi = dyHierarchyDao.create(spec);

        FolderSpec spec2 = new FolderSpec("bar");
        spec2.setDyhiId(dyhi.getId());
        Folder folder2 = folderDao.create(spec2);
        List<UUID> ids = folderDao.getAllIds(dyhi);
        assertTrue(ids.contains(folder2.getId()));
    }

    @Test
    public void testDeleteAllById() {

        FolderSpec spec1 = new FolderSpec("foo");
        Folder folder1 = folderDao.create(spec1);

        FolderSpec spec2 = new FolderSpec("bar");
        Folder folder2 = folderDao.create(spec2);

        assertEquals(2, folderDao.deleteAll(Lists.newArrayList(folder1.getId(), folder2.getId())));
    }
}
