package com.zorroa.archivist.repository;

import com.zorroa.archivist.ArchivistApplicationTests;
import com.zorroa.archivist.sdk.domain.AssetSearch;
import com.zorroa.archivist.sdk.domain.Folder;
import com.zorroa.archivist.sdk.domain.FolderBuilder;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FolderDaoTests extends ArchivistApplicationTests {

    @Autowired
    FolderDao folderDao;

    @Test
    public void testCreateAndGet() throws IOException {
        String name = "Foobar the folder";
        FolderBuilder builder = new FolderBuilder(name);
        Folder folder1 = folderDao.create(builder);
        refreshIndex();

        Folder folder2 = folderDao.get(folder1.getId());
        assertEquals(folder2.getName(), name);
    }

    @Test
    public void testGetUser() {
        String name = "Goo the folder";
        FolderBuilder builder = new FolderBuilder(name);
        folderDao.create(builder);

        builder = new FolderBuilder("Bam his brother");
        folderDao.create(builder);
        refreshIndex();

        List<Folder> folders = folderDao.getChildren(Folder.ROOT_ID);
        assertEquals(2, folders.size());
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
        refreshIndex();

        List<Folder> folders = folderDao.getChildren(grandpa);
        assertEquals(2, folders.size());
    }

    @Test
    public void testUpdate() {
        String name = "Gimbo";
        FolderBuilder builder = new FolderBuilder(name);
        Folder gimbo = folderDao.create(builder);
        builder = new FolderBuilder("Bimbo");
        Folder bimbo  = folderDao.create(builder);
        builder = new FolderBuilder("Bimbo-updated", gimbo);
        builder.setSearch(new AssetSearch());
        boolean ok = folderDao.update(bimbo, builder);
        assertTrue(ok);
        refreshIndex();
        logger.info("1");

        Folder bimbo2 = folderDao.get(bimbo.getId());
        assertEquals(bimbo2.getName(), "Bimbo-updated");
        assertEquals(bimbo2.getParentId(), gimbo.getId());
    }

    @Test
    public void testDelete() {
        String name = "Foofoo";
        FolderBuilder builder = new FolderBuilder(name);
        Folder foofoo = folderDao.create(builder);
        builder = new FolderBuilder("Snusnu");
        Folder snusnu  = folderDao.create(builder);
        folderDao.delete(foofoo);
        List<Folder> folders = folderDao.getChildren(Folder.ROOT_ID);
        assertEquals(folders.size(), 1);
        Folder f = folders.get(0);
        assertEquals(f.getName(), "Snusnu");
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
