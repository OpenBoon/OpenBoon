package com.zorroa.archivist.repository;

import com.google.common.collect.ImmutableList;
import com.zorroa.archivist.AbstractTest;
import com.zorroa.archivist.domain.*;
import com.zorroa.archivist.service.DyHierarchyService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

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
                        new DyHierarchyLevel("source.date", DyHierarchyLevelType.Year),
                        new DyHierarchyLevel("source.type.raw"),
                        new DyHierarchyLevel("source.extension.raw"),
                        new DyHierarchyLevel("source.filename.raw")));
        dyhi = dyHierarchyDao.create(spec);
    }

    @Test
    public void testCreate() {
        dyHierarchyService.generateAll();

    }
}
