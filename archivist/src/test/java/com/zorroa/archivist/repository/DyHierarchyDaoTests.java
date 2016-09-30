package com.zorroa.archivist.repository;

import com.google.common.collect.ImmutableList;
import com.zorroa.archivist.AbstractTest;
import com.zorroa.archivist.domain.*;
import com.zorroa.archivist.service.DyHierarchyService;
import com.zorroa.sdk.domain.PagedList;
import com.zorroa.sdk.domain.Pager;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

/**
 * Created by chambers on 7/15/16.
 */
public class DyHierarchyDaoTests extends AbstractTest {

    @Autowired
    DyHierarchyService dyHierarchyService;

    @Autowired
    DyHierarchyDao dyHierarchyDao;

    DyHierarchy dyhi;
    Folder folder;

    @Before
    public void init() {
        folder = folderService.create(new FolderSpec("foo"), false);
        DyHierarchySpec spec = new DyHierarchySpec();
        spec.setFolderId(folder.getId());
        spec.setLevels(
                ImmutableList.of(
                        new DyHierarchyLevel("source.date", DyHierarchyLevelType.Day),
                        new DyHierarchyLevel("source.type.raw"),
                        new DyHierarchyLevel("source.extension.raw"),
                        new DyHierarchyLevel("source.filename.raw")));
        dyhi = dyHierarchyDao.create(spec);
    }

    @Test
    public void testCreate() {
        assertEquals(dyhi.getFolderId(), folder.getId());
        assertEquals(4, dyhi.getLevels().size());
    }

    @Test
    public void testGetByFolder() {
        DyHierarchy d2 = dyHierarchyDao.get(folder);
        assertEquals(dyhi, d2);
    }

    @Test
    public void testGet() {
        DyHierarchy d2 = dyHierarchyDao.get(dyhi.getId());
        assertEquals(dyhi, d2);
    }

    @Test
    public void testGetSetWorking() {
        assertFalse(dyHierarchyDao.isWorking(dyhi));
        assertTrue(dyHierarchyDao.setWorking(dyhi, true));
        assertTrue(dyHierarchyDao.isWorking(dyhi));
        assertFalse(dyHierarchyDao.setWorking(dyhi, true));
        assertTrue(dyHierarchyDao.isWorking(dyhi));
        assertTrue(dyHierarchyDao.setWorking(dyhi, false));
    }

    @Test
    public void testDelete() {
        assertTrue(dyHierarchyDao.delete(dyhi.getId()));
    }

    @Test
    public void testUpdate() {
        boolean result = dyHierarchyDao.update(dyhi.getId(),
                dyhi.setLevels(ImmutableList.of(new DyHierarchyLevel("source.filename.raw"))));
        assertTrue(result);
        DyHierarchy d2 = dyHierarchyDao.refresh(dyhi);
        assertEquals(1, d2.getLevels().size());
    }

    @Test
    public void testGetAll() {
        long count = dyHierarchyDao.count();
        List<DyHierarchy> list = dyHierarchyDao.getAll();
        assertEquals(count, list.size());

        Folder f2 = folderService.create(new FolderSpec("bar"), false);
        DyHierarchySpec spec = new DyHierarchySpec();
        spec.setFolderId(f2.getId());
        spec.setLevels(
                ImmutableList.of(
                        new DyHierarchyLevel("source.date", DyHierarchyLevelType.Day)));
        dyHierarchyDao.create(spec);
        list = dyHierarchyDao.getAll();
        assertEquals(count+1, list.size());
    }


    @Test
    public void testGetAllPaged() {
        long count = dyHierarchyDao.count();
        PagedList<DyHierarchy> list = dyHierarchyDao.getAll(Pager.first());
        assertEquals(count, list.size());

        Folder f2 = folderService.create(new FolderSpec("bar"), false);
        DyHierarchySpec spec = new DyHierarchySpec();
        spec.setFolderId(f2.getId());
        spec.setLevels(
                ImmutableList.of(
                        new DyHierarchyLevel("source.date", DyHierarchyLevelType.Day)));
        dyHierarchyDao.create(spec);
        list = dyHierarchyDao.getAll(Pager.first());
        assertEquals(count+1, list.size());
    }
}
