package com.zorroa.archivist.repository;

import com.google.common.collect.Lists;
import com.zorroa.archivist.ArchivistApplicationTests;
import com.zorroa.archivist.sdk.domain.*;
import com.zorroa.archivist.security.SecurityUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;

public class FolderDaoTests extends ArchivistApplicationTests {

    @Autowired
    FolderDao folderDao;

    @Test
    public void testCreateAndGet() throws IOException {
        String name = "Foobar the folder";
        FolderBuilder builder = new FolderBuilder(name);
        Folder folder1 = folderDao.create(builder);

        Folder folder2 = folderDao.get(folder1.getId());
        assertEquals(folder2.getName(), name);
    }

    @Test
    public void testGetByParentAndName() throws IOException {
        String name = "test";
        FolderBuilder builder = new FolderBuilder(name);
        Folder folder1 = folderDao.create(builder);
        Folder folder2 = folderDao.get(Folder.ROOT_ID, name);
        assertEquals(folder1, folder1);
    }

    @Test
    public void testGetAll() throws IOException {
        Folder folder1 = folderDao.create(new FolderBuilder("test1"));
        Folder folder2 = folderDao.create(new FolderBuilder("test2"));
        Folder folder3 = folderDao.create(new FolderBuilder("test3"));
        List<Folder> some = folderDao.getAll(
                Lists.newArrayList(folder2.getId(), folder3.getId()));
        assertTrue(some.contains(folder2));
        assertTrue(some.contains(folder3));
        assertFalse(some.contains(folder1));
    }

    @Test(expected= DataIntegrityViolationException.class)
    public void testCreateDuplicate() throws IOException {
        String name = "Foobar the folder";
        FolderBuilder builder = new FolderBuilder(name);
        folderDao.create(builder);
        folderDao.create(builder);
    }

    @Test
    public void testGetChildren() {
        String name = "Grandpa";
        FolderBuilder builder = new FolderBuilder(name);
        Folder grandpa = folderDao.create(builder);
        builder = new FolderBuilder("Dad", grandpa);
        Folder dad = folderDao.create(builder);
        builder = new FolderBuilder("Uncle", grandpa);
        Folder uncle = folderDao.create(builder);
        builder = new FolderBuilder("Child", dad);
        Folder child = folderDao.create(builder);

        List<Folder> folders = folderDao.getChildren(grandpa);
        assertEquals(2, folders.size());
    }

    @Test
    public void testGetChildrenInsecure() {

        assertFalse(SecurityUtils.hasPermission("group::superuser"));

        Folder pub = folderDao.get(0, "Folders");
        Folder f1 = folderDao.create(new FolderBuilder("level1", pub));
        Folder f2 = folderDao.create(new FolderBuilder("level2", pub));
        Folder f3 = folderDao.create(new FolderBuilder("level3", pub));
        folderDao.setAcl(f3, new Acl().addEntry(
                userService.getPermission("group::superuser")));

        assertFalse(folderDao.hasAccess(f3, Access.Read));
        assertEquals(3, folderDao.getChildrenInsecure(pub.getId()).size());
        assertEquals(2, folderDao.getChildren(pub.getId()).size());
    }

    @Test
    public void testHasAccess() throws IOException {
        FolderBuilder builder = new FolderBuilder("test");
        Folder folder1 = folderDao.create(builder);
        assertTrue(folderDao.hasAccess(folder1, Access.Read));

        Permission p = userService.createPermission(new PermissionBuilder("group", "foo"));
        Acl acl = new Acl();
        acl.addEntry(p, Access.Read);
        folderDao.setAcl(folder1, acl);

        assertFalse(folderDao.hasAccess(folder1, Access.Read));
    }

    @Test
    public void testGetAndSetAcl() throws IOException {
        FolderBuilder builder = new FolderBuilder("test");
        Folder folder1 = folderDao.create(builder);

        Permission p = userService.createPermission(new PermissionBuilder("group","foo"));
        folderDao.setAcl(folder1, new Acl().addEntry(p, Access.Read));

        Acl acl = folderDao.getAcl(folder1);
        assertTrue(acl.hasAccess(p.getId(), Access.Read));
    }

    @Test
    public void testUpdate() {
        Folder v1 = folderDao.create(new FolderBuilder("v1", Folder.ROOT_ID));
        folderDao.update(v1, new FolderUpdateBuilder().setName("v2"));
        Folder v2 = folderDao.get(v1.getId());
        assertEquals("v2", v2.getName());
        assertEquals(null, v2.getSearch());
        assertEquals(0, v2.getAcl().size());
    }

    @Test
    public void testDelete() {
        int count = folderDao.getChildren(Folder.ROOT_ID).size();
        Folder f = folderDao.create(new FolderBuilder("test"));
        assertTrue(folderDao.delete(f));
        assertEquals(count, folderDao.getChildren(Folder.ROOT_ID).size());
    }

    @Test
    public void testExists() {
        FolderBuilder builder = new FolderBuilder("foo");
        Folder folder1 = folderDao.create(builder);
        assertTrue(folderDao.exists(Folder.ROOT_ID, "foo"));

        builder = new FolderBuilder("bar", folder1);
        Folder folder2 = folderDao.create(builder);
        assertTrue(folderDao.exists(folder1.getId(), "bar"));
    }
}
