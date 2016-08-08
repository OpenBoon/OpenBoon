package com.zorroa.archivist.service;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.zorroa.archivist.AbstractTest;
import com.zorroa.archivist.domain.Acl;
import com.zorroa.archivist.domain.Folder;
import com.zorroa.archivist.domain.FolderSpec;
import com.zorroa.sdk.domain.Access;
import org.junit.Test;
import org.springframework.dao.EmptyResultDataAccessException;

import java.util.List;

import static org.junit.Assert.*;

public class FolderServiceTests extends AbstractTest {

    @Test(expected=EmptyResultDataAccessException.class)
    public void testSetAcl() {
        authenticate("admin");

        FolderSpec builder = new FolderSpec("Folder");
        Folder folder = folderService.create(builder);
        folderService.get(folder.getId());

        folderService.setAcl(folder, new Acl().addEntry(
                userService.getPermission("group::superuser"), Access.Read));
        folderService.get(folder.getId());
    }

    @Test
    public void testCreateAndGet() {
        FolderSpec builder = new FolderSpec("Da Kind Assets");
        Folder folder1 = folderService.create(builder);

        Folder folder2 = folderService.get(folder1.getId());
        assertEquals(folder1.getName(), folder2.getName());
    }

    @Test
    public void testDescendants() {
        Folder grandpa = folderService.create(new FolderSpec("grandpa"));
        Folder dad = folderService.create(new FolderSpec("dad", grandpa.getId()));
        Folder uncle = folderService.create(new FolderSpec("uncle", grandpa.getId()));
        folderService.create(new FolderSpec("child", dad.getId()));
        folderService.create(new FolderSpec("cousin", uncle.getId()));
        List<Folder> descendents = folderService.getAllDescendants(grandpa, false);
        assertEquals(4, descendents.size());
    }

    @Test
    public void testGetAllDescendants() {
        Folder grandpa = folderService.create(new FolderSpec("grandpa"));
        Folder dad = folderService.create(new FolderSpec("dad", grandpa.getId()));
        Folder uncle = folderService.create(new FolderSpec("uncle", grandpa.getId()));
        folderService.create(new FolderSpec("child", dad.getId()));
        folderService.create(new FolderSpec("cousin", uncle.getId()));
        assertEquals(5, folderService.getAllDescendants(Lists.newArrayList(grandpa), true, true).size());
        assertEquals(4, folderService.getAllDescendants(Lists.newArrayList(grandpa), false, true).size());

        logger.info("{}", folderService.getAllDescendants(
                Lists.newArrayList(grandpa, dad, uncle), true, true));

        assertEquals(5, ImmutableSet.copyOf(folderService.getAllDescendants(
                Lists.newArrayList(grandpa, dad, uncle), true, true)).size());
        assertEquals(4, ImmutableSet.copyOf(folderService.getAllDescendants(
                Lists.newArrayList(grandpa, dad, uncle), false, true)).size());
    }

    @Test
    public void testGetChildren() {
        Folder folder1 = folderService.create(new FolderSpec("test1"));
        Folder folder1a = folderService.create(new FolderSpec("test1a", folder1));
        Folder folder1b = folderService.create(new FolderSpec("test1b", folder1));
        Folder folder1c = folderService.create(new FolderSpec("test1c", folder1));

        List<Folder> children = folderService.getChildren(folder1);
        assertEquals(3, children.size());
        assertTrue(children.contains(folder1a));
        assertTrue(children.contains(folder1b));
        assertTrue(children.contains(folder1c));
        assertFalse(children.contains(folder1));
    }

    @Test
    public void testGetByPath() {
        Folder folder1 = folderService.create(new FolderSpec("test1"));
        Folder folder1a = folderService.create(new FolderSpec("test1a", folder1));
        Folder folder1b = folderService.create(new FolderSpec("test1b", folder1a));
        Folder folder1c = folderService.create(new FolderSpec("test1c", folder1b));

        Folder folder = folderService.get("/test1/test1a/test1b/test1c");
        assertEquals(folder1b.getId(), folder.getParentId().intValue());
        assertEquals(folder1c.getName(), folder.getName());
    }

    @Test(expected=EmptyResultDataAccessException.class)
    public void testGetByPathFail() {
        Folder folder = folderService.get("/foo/bar/bam");
        assertEquals(null, folder);
    }

    @Test
    public void testExistsByPath() {
        Folder folder1 = folderService.create(new FolderSpec("test1"));
        Folder folder1a = folderService.create(new FolderSpec("test1a", folder1));
        Folder folder1b = folderService.create(new FolderSpec("test1b", folder1a));
        Folder folder1c = folderService.create(new FolderSpec("test1c", folder1b));

        assertTrue(folderService.exists("/test1/test1a/test1b/test1c"));
        assertTrue(folderService.exists("/test1/test1a"));
        assertFalse(folderService.exists("/testb"));
        assertFalse(folderService.exists("/testb/test123"));
    }

    @Test
    public void testUpdate() {
        Folder folder = folderService.create(new FolderSpec("orig"));
        boolean ok = folderService.update(folder.getId(), folder.setName("new"));
        assertTrue(ok);
        Folder revised = folderService.get(folder.getId());
        assertEquals("new", revised.getName());
    }

    @Test
    public void testUpdateWithNewParent() {

        FolderSpec fs1 = new FolderSpec("orig");
        FolderSpec fs2 = new FolderSpec("unorig");

        Folder folder1 = folderService.create(fs1);
        Folder folder2 = folderService.create(fs2);
        boolean ok = folderService.update(folder2.getId(), folder2.setParentId(folder1.getId()));
        assertTrue(ok);

        Folder revised = folderService.get(folder2.getId());
        assertEquals(folder1.getId(), revised.getParentId().intValue());

        List<Folder> folders = folderService.getAllDescendants(Lists.newArrayList(folder1), false, false);
        assertTrue(folders.contains(folder2));

        folders = folderService.getAllDescendants(Lists.newArrayList(folder2), false, false);
        assertTrue(folders.isEmpty());
    }

    @Test
    public void testUpdateRecursive() {
        Folder folder = folderService.create(new FolderSpec("orig"));
        assertTrue(folder.isRecursive());
        boolean ok = folderService.update(folder.getId(), folder.setRecursive(false));
        assertTrue(ok);
        Folder revised = folderService.get(folder.getId());
        assertFalse(revised.isRecursive());
    }

    @Test
    public void testDelete() {
        FolderSpec builder = new FolderSpec("shizzle");
        Folder start = folderService.create(builder);
        Folder root = start;
        for (int i=0; i<10; i++) {
            builder = new FolderSpec("shizzle"+i, start);
            start = folderService.create(builder);
        }
        assertTrue(folderService.delete(root));
    }
}
