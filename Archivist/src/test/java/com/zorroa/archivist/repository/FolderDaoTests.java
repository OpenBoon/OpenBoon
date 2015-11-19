package com.zorroa.archivist.repository;

import com.zorroa.archivist.ArchivistApplicationTests;
import com.zorroa.archivist.SecurityUtils;
import com.zorroa.archivist.sdk.domain.Folder;
import com.zorroa.archivist.sdk.domain.FolderBuilder;
import org.elasticsearch.index.query.QueryBuilders;
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
        FolderBuilder builder = new FolderBuilder(name, 1);
        Folder folder1 = folderDao.create(builder);
        refreshIndex(100);

        Folder folder2 = folderDao.get(folder1.getId());
        assertEquals(folder2.getName(), name);
    }

    @Test
    public void testGetUser() {
        String name = "Goo the folder";
        FolderBuilder builder = new FolderBuilder(name, 3);
        folderDao.create(builder);
        builder = new FolderBuilder("Bam his brother", 4);
        folderDao.create(builder);
        refreshIndex(100);

        List<Folder> folders = folderDao.getAll(3);
        assertEquals(1, folders.size());
    }

    @Test
    public void testGetChildren() {
        String name = "Grandpa";
        FolderBuilder builder = new FolderBuilder(name, 1);
        Folder grandpa = folderDao.create(builder);
        builder = new FolderBuilder("Dad", 1);
        builder.setParentId(grandpa.getId());
        Folder dad = folderDao.create(builder);
        builder = new FolderBuilder("Uncle", 1);
        builder.setParentId(grandpa.getId());
        Folder uncle = folderDao.create(builder);
        builder = new FolderBuilder("Child", 1);
        builder.setParentId(dad.getId());
        Folder child = folderDao.create(builder);
        refreshIndex(1000);

        List<Folder> folders = folderDao.getChildren(grandpa);
        assertEquals(2, folders.size());
    }

    @Test
    public void testUpdate() {
        String name = "Gimbo";
        FolderBuilder builder = new FolderBuilder(name, 1);
        Folder gimbo = folderDao.create(builder);
        builder = new FolderBuilder("Bimbo", 1);
        Folder bimbo  = folderDao.create(builder);
        builder = new FolderBuilder("Bimbo-updated", 1);
        builder.setParentId(gimbo.getId());
        builder.setQuery(QueryBuilders.matchAllQuery().toString());
        boolean ok = folderDao.update(bimbo, builder);
        assertTrue(ok);
        refreshIndex(1000);

        Folder bimbo2 = folderDao.get(bimbo.getId());
        assertEquals(bimbo2.getName(), "Bimbo-updated");
        assertEquals(bimbo2.getParentId(), gimbo.getId());
    }

    @Test
    public void testDelete() {
        String name = "Foofoo";
        FolderBuilder builder = new FolderBuilder(name, 5);
        Folder foofoo = folderDao.create(builder);
        builder = new FolderBuilder("Snusnu", 5);
        Folder snusnu  = folderDao.create(builder);
        folderDao.delete(foofoo);
        List<Folder> folders = folderDao.getAll(5);
        assertEquals(folders.size(), 1);
        Folder f = folders.get(0);
        assertEquals(f.getName(), "Snusnu");
    }

    @Test
    public void testExists() {
        FolderBuilder builder = new FolderBuilder("foo", SecurityUtils.getUser().getId());
        Folder folder1 = folderDao.create(builder);
        assertTrue(folderDao.exists(null, "foo"));

        builder = new FolderBuilder("bar", SecurityUtils.getUser().getId());
        builder.setParentId(folder1.getId());
        Folder folder2 = folderDao.create(builder);
        assertTrue(folderDao.exists(folder1.getId(), "bar"));
    }
}
