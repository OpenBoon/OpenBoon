package com.zorroa.archivist.service;

import com.google.common.collect.Lists;
import com.zorroa.archivist.ArchivistApplicationTests;
import com.zorroa.archivist.sdk.domain.DuplicateElementException;
import com.zorroa.archivist.sdk.domain.Folder;
import com.zorroa.archivist.sdk.domain.FolderBuilder;
import com.zorroa.archivist.sdk.service.FolderService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.Assert.*;

public class FolderServiceTests extends ArchivistApplicationTests {

    @Autowired
    FolderService folderService;

    @Test
    public void testCreateAndGet() {
        FolderBuilder builder = new FolderBuilder("Da Kind Assets");
        Folder folder1 = folderService.create(builder);

        Folder folder2 = folderService.get(folder1.getId());
        assertEquals(folder1.getName(), folder2.getName());
    }

    @Test
    public void testDescendents() {
        Folder grandpa = folderService.create(new FolderBuilder("grandpa"));
        Folder dad = folderService.create(new FolderBuilder("dad", grandpa.getId()));
        Folder uncle = folderService.create(new FolderBuilder("uncle", grandpa.getId()));
        folderService.create(new FolderBuilder("child", dad.getId()));
        folderService.create(new FolderBuilder("cousin", uncle.getId()));
        List<Folder> descendents = folderService.getAllDecendents(grandpa);
        assertEquals(4, descendents.size());
    }

    @Test
    public void testGetAllDescendantIds() {
        Folder grandpa = folderService.create(new FolderBuilder("grandpa"));
        Folder dad = folderService.create(new FolderBuilder("dad", grandpa.getId()));
        Folder uncle = folderService.create(new FolderBuilder("uncle", grandpa.getId()));
        folderService.create(new FolderBuilder("child", dad.getId()));
        folderService.create(new FolderBuilder("cousin", uncle.getId()));
        assertEquals(5, folderService.getAllDescendantIds(Lists.newArrayList(grandpa.getId()), true).size());
        assertEquals(4, folderService.getAllDescendantIds(Lists.newArrayList(grandpa.getId()), false).size());

        assertEquals(5, folderService.getAllDescendantIds(
                Lists.newArrayList(grandpa.getId(), dad.getId(), uncle.getId()), true).size());
        assertEquals(4, folderService.getAllDescendantIds(
                Lists.newArrayList(grandpa.getId(), dad.getId(), uncle.getId()), false).size());
    }

    @Test
    public void testGetChildren() {
        Folder folder1 = folderService.create(new FolderBuilder("test1"));
        Folder folder1a = folderService.create(new FolderBuilder("test1a", folder1));
        Folder folder1b = folderService.create(new FolderBuilder("test1b", folder1));
        Folder folder1c = folderService.create(new FolderBuilder("test1c", folder1));

        List<Folder> children = folderService.getChildren(folder1);
        assertEquals(3, children.size());
        assertTrue(children.contains(folder1a));
        assertTrue(children.contains(folder1b));
        assertTrue(children.contains(folder1c));
        assertFalse(children.contains(folder1));
    }

    @Test
    public void testUpdate() {
        Folder folder = folderService.create(new FolderBuilder("orig"));
        boolean ok = folderService.update(folder, new FolderBuilder("new"));
        assertTrue(ok);
        Folder revised = folderService.get(folder.getId());
        assertEquals("new", revised.getName());
    }

    @Test(expected=DuplicateElementException.class)
    public void testCreateFailureInRoot() {
        FolderBuilder builder = new FolderBuilder("shizzle");
        Folder folder1 = folderService.create(builder);
        folderService.create(builder);
    }

    @Test(expected=DuplicateElementException.class)
    public void testDeepCreateFailure() {
        FolderBuilder builder = new FolderBuilder("shizzle");
        Folder folder1 = folderService.create(builder);
        assertEquals(Folder.ROOT_ID, folder1.getParentId());

        builder = new FolderBuilder("shizzle");
        builder.setParentId(folder1.getId());

        Folder folder2 = folderService.create(builder);
        assertEquals(folder2.getParentId(), folder1.getId());
        folderService.create(builder);
    }
}
