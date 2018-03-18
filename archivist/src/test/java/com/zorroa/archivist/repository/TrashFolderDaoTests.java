package com.zorroa.archivist.repository;

import com.zorroa.archivist.AbstractTest;
import com.zorroa.archivist.domain.Folder;
import com.zorroa.archivist.domain.FolderSpec;
import com.zorroa.archivist.domain.TrashedFolder;
import com.zorroa.archivist.security.UtilsKt;
import com.zorroa.archivist.service.FolderService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Created by chambers on 12/5/16.
 */
public class TrashFolderDaoTests extends AbstractTest {

    @Autowired
    TrashFolderDao trashFolderDao;

    @Autowired
    FolderService folderService;

    @Test
    public void testCreate() {
        Folder folder1 = folderService.create(new FolderSpec("test1"));
        trashFolderDao.create(folder1, "a", true, 1);

        int count = jdbc.queryForObject(
                "SELECT COUNT(1) FROM folder_trash WHERE str_opid=?", Integer.class, "a");
        assertEquals(count, 1);
    }

    @Test
    public void testGetAllByOp() {
        Folder folder1 = folderService.create(new FolderSpec("test1"));
        Folder folder2 = folderService.create(new FolderSpec("test2"));

        trashFolderDao.create(folder1, "a", true, 1);
        trashFolderDao.create(folder2, "a", false, 1);

        List<TrashedFolder> folders = trashFolderDao.getAll("a");
        assertEquals(2, folders.size());
    }

    @Test
    public void testGetAllByOpWithChildren() {
        Folder folder1 = folderService.create(new FolderSpec("test1"));
        Folder folder2 = folderService.create(new FolderSpec("test2", folder1.getId()));

        trashFolderDao.create(folder1, "a", true, 0);
        trashFolderDao.create(folder2, "a", false, 1);

        List<TrashedFolder> folders = trashFolderDao.getAll("a");
        assertEquals(2, folders.size());
    }

    @Test
    public void testGetAllByParent() {
        Folder folder1 = folderService.create(new FolderSpec("test1"));
        Folder folder2 = folderService.create(new FolderSpec("test2"));
        trashFolderDao.create(folder1, "a", true, 1);
        trashFolderDao.create(folder2, "a", false, 2);

        List<TrashedFolder> folders = trashFolderDao.getAll(folderService.get(Folder.ROOT_ID),
                UtilsKt.getUserId());
        assertEquals(1, folders.size());

        folders = trashFolderDao.getAll(folder1, UtilsKt.getUserId());
        assertEquals(0, folders.size());

        folders = trashFolderDao.getAll(folder2, UtilsKt.getUserId());
        assertEquals(0, folders.size());
    }

    @Test
    public void testGetAllByUser() {
        Folder folder1 = folderService.create(new FolderSpec("test1"));
        Folder folder2 = folderService.create(new FolderSpec("test2"));
        trashFolderDao.create(folder1, "a", true, 1);
        trashFolderDao.create(folder2, "a", false, 2);

        List<TrashedFolder> folders = trashFolderDao.getAll(UtilsKt.getUserId());
        assertEquals(1, folders.size());
    }


    @Test
    public void testCount() {
        Folder folder1 = folderService.create(new FolderSpec("test1"));
        Folder folder2 = folderService.create(new FolderSpec("test2"));
        trashFolderDao.create(folder1, "a", true, 1);
        trashFolderDao.create(folder2, "a", false, 2);

        int count = trashFolderDao.count(UtilsKt.getUserId());
        assertEquals(1, count);
    }
}
