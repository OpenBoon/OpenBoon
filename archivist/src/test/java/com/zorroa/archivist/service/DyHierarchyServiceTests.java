package com.zorroa.archivist.service;

import com.google.common.collect.ImmutableList;
import com.zorroa.archivist.AbstractTest;
import com.zorroa.archivist.domain.DyHierarchy;
import com.zorroa.archivist.domain.DyHierarchyLevel;
import com.zorroa.archivist.domain.DyHierarchyLevelType;
import com.zorroa.archivist.domain.FolderSpec;
import com.zorroa.common.repository.AssetDao;
import com.zorroa.archivist.domain.Folder;
import com.zorroa.sdk.processor.Source;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;

/**
 * Created by chambers on 7/14/16.
 */
public class DyHierarchyServiceTests extends AbstractTest {

    @Autowired
    FolderService folderService;

    @Autowired
    DyHierarchyService dyhiService;

    @Autowired
    AssetDao assetDao;

    @Before
    public void init() {
        for (File f: getTestImagePath("set01").toFile().listFiles()) {
            if (!f.isFile()) {
                continue;
            }
            Source ab = new Source(f);
            assetDao.index(ab);
        }
        for (File f: getTestPath("office").toFile().listFiles()) {
            if (!f.isFile()) {
                continue;
            }
            Source ab = new Source(f);
            assetDao.index(ab);
        }
        for (File f: getTestPath("video").toFile().listFiles()) {
            if (!f.isFile()) {
                continue;
            }
            Source ab = new Source(f);
            assetDao.index(ab);
        }
        refreshIndex();
    }

    @Test
    public void testGenerate() {
        Folder f = folderService.create(new FolderSpec("foo"), false);
        DyHierarchy agg = new DyHierarchy();
        agg.setFolderId(f.getId());
        agg.setLevels(
                ImmutableList.of(
                        new DyHierarchyLevel("source.date", DyHierarchyLevelType.Date)
                                .setOption("interval", "1m")
                                .setOption("format", "E"),
                        new DyHierarchyLevel("source.type.raw"),
                        new DyHierarchyLevel("source.extension.raw"),
                        new DyHierarchyLevel("source.filename.raw")));

        dyhiService.generate(agg);
    }

    @Test
    public void testGenerateAndUpdate() {

        Folder folder = folderService.create(new FolderSpec("foo"), false);
        DyHierarchy agg = new DyHierarchy();
        agg.setFolderId(folder.getId());
        agg.setLevels(
                ImmutableList.of(
                        new DyHierarchyLevel("source.date", DyHierarchyLevelType.Date)
                                .setOption("interval", "1m")
                                .setOption("format", "E"),
                        new DyHierarchyLevel("source.type.raw"),
                        new DyHierarchyLevel("source.extension.raw"),
                        new DyHierarchyLevel("source.filename.raw")));

        dyhiService.generate(agg);

        for (File f: getTestImagePath("set02").toFile().listFiles()) {
            if (!f.isFile()) {
                continue;
            }
            Source ab = new Source(f);
            assetDao.index(ab);
        }

        for (File f: getTestImagePath("set03").toFile().listFiles()) {
            if (!f.isFile()) {
                continue;
            }
            Source ab = new Source(f);
            assetDao.index(ab);
        }

        refreshIndex();
        dyhiService.generate(agg);
    }
}
