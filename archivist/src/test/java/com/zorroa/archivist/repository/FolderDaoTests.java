package com.zorroa.archivist.repository;

import com.google.common.collect.Lists;
import com.zorroa.archivist.AbstractTest;
import com.zorroa.archivist.domain.*;
import com.zorroa.archivist.security.SecurityUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;

public class FolderDaoTests extends AbstractTest {

    @Autowired
    FolderDao folderDao;

    @Test
    public void testCreateAndGet() throws IOException {
        String name = "Foobar the folder";
        FolderSpec builder = new FolderSpec(name);
        Folder folder1 = folderDao.create(builder);

        Folder folder2 = folderDao.get(folder1.getId());
        assertEquals(folder2.getName(), name);
    }

    @Test
    public void testGetByParentAndName() throws IOException {
        String name = "test";
        FolderSpec builder = new FolderSpec(name);
        Folder folder1 = folderDao.create(builder);
        Folder folder2 = folderDao.get(Folder.ROOT_ID, name);
        assertEquals(folder1, folder1);
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
    public void testGetChildrenInsecure() {
        authenticate("admin");
        assertFalse(SecurityUtils.hasPermission("group::administrator"));

        Folder pub = folderDao.get(0, "Users");
        Folder f1 = folderDao.create(new FolderSpec("level1", pub));
        Folder f2 = folderDao.create(new FolderSpec("level2", pub));
        Folder f3 = folderDao.create(new FolderSpec("level3", pub));
        folderDao.setAcl(f3.getId(), new Acl().addEntry(
                userService.getPermission("group::administrator")));

        assertFalse(folderDao.hasAccess(f3, Access.Read));
        assertEquals(5, folderDao.getChildrenInsecure(pub.getId()).size());
        assertEquals(3, folderDao.getChildren(pub.getId()).size());
    }

    @Test
    public void testHasAccess() throws IOException {
        authenticate("admin");
        FolderSpec builder = new FolderSpec("test");
        Folder folder1 = folderDao.create(builder);
        assertTrue(folderDao.hasAccess(folder1, Access.Read));

        Permission p = userService.createPermission(new PermissionSpec("group", "foo"));
        Acl acl = new Acl();
        acl.addEntry(p, Access.Read);
        folderDao.setAcl(folder1.getId(), acl);

        assertFalse(folderDao.hasAccess(folder1, Access.Read));
    }

    @Test
    public void testGetAndSetAcl() throws IOException {
        FolderSpec builder = new FolderSpec("test");
        Folder folder1 = folderDao.create(builder);

        Permission p = userService.createPermission(new PermissionSpec("group","foo"));
        folderDao.setAcl(folder1.getId(), new Acl().addEntry(p, Access.Read));

        Acl acl = folderDao.getAcl(folder1.getId());
        assertTrue(acl.hasAccess(p.getId(), Access.Read));
    }

    @Test
    public void testUpdate() {
        FolderSpec spec = new FolderSpec("v1", Folder.ROOT_ID);
        Folder v1 = folderDao.create(spec);
        folderDao.update(v1.getId(), v1.setName("v2"));
        Folder v2 = folderDao.get(v1.getId());
        assertEquals("v2", v2.getName());
        assertEquals(null, v2.getSearch());
        assertEquals(0, v2.getAcl().size());
    }

    @Test
    public void testDelete() {
        int count = folderDao.getChildren(Folder.ROOT_ID).size();
        Folder f = folderDao.create(new FolderSpec("test"));
        assertTrue(folderDao.delete(f));
        assertEquals(count, folderDao.getChildren(Folder.ROOT_ID).size());
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
        assertTrue(folderDao.exists(Folder.ROOT_ID, "foo"));

        builder = new FolderSpec("bar", folder1);
        Folder folder2 = folderDao.create(builder);
        assertTrue(folderDao.exists(folder1.getId(), "bar"));
    }
}
