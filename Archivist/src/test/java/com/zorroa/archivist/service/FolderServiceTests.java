package com.zorroa.archivist.service;

import com.zorroa.archivist.ArchivistApplicationTests;
import com.zorroa.archivist.sdk.domain.Folder;
import com.zorroa.archivist.sdk.domain.FolderBuilder;
import com.zorroa.archivist.sdk.service.FolderService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FolderServiceTests extends ArchivistApplicationTests {

    @Autowired
    FolderService folderService;

    @Test
    public void testCreateAndGet() {
        FolderBuilder builder = new FolderBuilder("Da Kind Assets", 1);
        Folder folder1 = folderService.create(builder);

        Folder folder2 = folderService.get(folder1.getId());
        assertEquals(folder1.getName(), folder2.getName());
    }

    @Test
    public void testDescendents() {
        Folder grandpa = folderService.create(new FolderBuilder("grandpa", 1));
        Folder dad = folderService.create(new FolderBuilder("dad", 1, grandpa.getId()));
        Folder uncle = folderService.create(new FolderBuilder("uncle", 1, grandpa.getId()));
        Folder child = folderService.create(new FolderBuilder("child", 1, dad.getId()));
        Folder cousin = folderService.create(new FolderBuilder("cousin", 1, uncle.getId()));
        List<Folder> descendents = folderService.getAllDecendents(grandpa);
        assertEquals(4, descendents.size());
    }

    @Test
    public void testUpdate() {
        Folder folder = folderService.create(new FolderBuilder("orig", 1));
        boolean ok = folderService.update(folder, new FolderBuilder("new", 2));
        assertTrue(ok);
        Folder revised = folderService.get(folder.getId());
        assertEquals("new", revised.getName());
        assertEquals(2, revised.getUserId());
    }
}
