package com.zorroa.archivist.service;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.zorroa.archivist.AbstractTest;
import com.zorroa.archivist.domain.*;
import com.zorroa.sdk.domain.Asset;
import com.zorroa.sdk.domain.PagedList;
import com.zorroa.sdk.domain.Pager;
import com.zorroa.sdk.processor.Source;
import com.zorroa.sdk.search.AssetSearch;
import com.zorroa.sdk.util.Json;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by chambers on 7/14/16.
 */
public class DyHierarchyServiceTests extends AbstractTest {

    @Autowired
    FolderService folderService;

    @Autowired
    DyHierarchyService dyhiService;


    @Before
    public void init() throws ParseException {
        for (File f: getTestImagePath("set01").toFile().listFiles()) {
            if (!f.isFile() || f.isHidden()) {
                continue;
            }
            Source ab = new Source(f);
            ab.setAttr("source.date",
                    new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").parse("04-07-2014 11:22:33"));
            ab.setAttr("tree.path", ImmutableList.of("/foo/bar/", "/bing/bang/", "/foo/shoe/"));
            assetService.index(ab);
        }
        for (File f: getTestPath("office").toFile().listFiles()) {
            if (!f.isFile() || f.isHidden()) {
                continue;
            }
            Source ab = new Source(f);
            ab.setAttr("source.date",
                    new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").parse("03-05-2013 09:11:14"));
            assetService.index(ab);
        }
        for (File f: getTestPath("video").toFile().listFiles()) {
            if (!f.isFile() || f.isHidden()) {
                continue;
            }
            Source ab = new Source(f);
            ab.setAttr("source.date",
                    new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").parse("11-12-2015 06:14:10"));
            assetService.index(ab);
        }
        refreshIndex();
    }

    @Test
    public void testGenerateWithMultiplePaths() {
        Folder f = folderService.create(new FolderSpec("foo"), false);
        DyHierarchy agg = new DyHierarchy();
        agg.setFolderId(f.getId());
        agg.setLevels(
                ImmutableList.of(
                        new DyHierarchyLevel("tree.path", DyHierarchyLevelType.Path)));
        int result = dyhiService.generate(agg);

        Folder folder = folderService.get("/foo/foo/bar");
        logger.info("{}", Json.serializeToString(folder.getSearch()));
        assertEquals(5, searchService.count(folder.getSearch()));

        folder = folderService.get("/foo/bing/bang");
        assertEquals(5, searchService.count(folder.getSearch()));

        folder = folderService.get("/foo/foo/shoe");
        assertEquals(5, searchService.count(folder.getSearch()));

        folder = folderService.get("/foo/foo");
        assertEquals(5, searchService.count(folder.getSearch()));
    }

    @Test
    public void testGenerateWithPath() {
        Folder f = folderService.create(new FolderSpec("foo"), false);
        DyHierarchy agg = new DyHierarchy();
        agg.setFolderId(f.getId());
        agg.setLevels(
                ImmutableList.of(
                        new DyHierarchyLevel("source.type.raw"),
                        new DyHierarchyLevel("source.directory", DyHierarchyLevelType.Path),
                        new DyHierarchyLevel("source.extension.raw")));
        int result = dyhiService.generate(agg);

        Folder folder = folderService.get("/foo/video/Users/chambers/src/zorroa-test-data/video/m4v");
        assertEquals(1, searchService.count(folder.getSearch()));

        folder = folderService.get("/foo/video/Users/chambers/src/zorroa-test-data/video");
        assertEquals(1, searchService.count(folder.getSearch()));

        folder = folderService.get("/foo/video/Users/chambers/src/zorroa-test-data");
        logger.info("{}", Json.serializeToString(folder.getSearch()));
        assertEquals(1, searchService.count(folder.getSearch()));

        folder = folderService.get("/foo/video");
        assertEquals(1, searchService.count(folder.getSearch()));
    }

    @Test
    public void testGenerate() {
        Folder f = folderService.create(new FolderSpec("foo"), false);
        DyHierarchy agg = new DyHierarchy();
        agg.setFolderId(f.getId());
        agg.setLevels(
                ImmutableList.of(
                        new DyHierarchyLevel("source.date", DyHierarchyLevelType.Year),
                        new DyHierarchyLevel("source.type.raw"),
                        new DyHierarchyLevel("source.extension.raw"),
                        new DyHierarchyLevel("source.filename.raw")));
        dyhiService.generate(agg);
    }


    @Test
    public void testDeleteEmptyFolders() {
        Folder f = folderService.create(new FolderSpec("foo"), false);
        DyHierarchy agg = new DyHierarchy();
        agg.setFolderId(f.getId());
        agg.setLevels(
                ImmutableList.of(
                        new DyHierarchyLevel("source.type.raw"),
                        new DyHierarchyLevel("source.extension.raw")));

        dyhiService.generate(agg);

        PagedList<Asset> assets = assetService.getAll(Pager.first(100));
        for (Asset asset: assets) {
            assetService.update(asset.getId(), ImmutableMap.of("source",
                    ImmutableMap.of("extension", "abc")));
        }
        refreshIndex();
        dyhiService.generate(agg);
    }


    @Test
    public void testGenerateDateHierarchy() {
        Folder f = folderService.create(new FolderSpec("foo"), false);
        DyHierarchy agg = new DyHierarchy();
        agg.setFolderId(f.getId());
        agg.setLevels(
                ImmutableList.of(
                        new DyHierarchyLevel("source.date", DyHierarchyLevelType.Year),
                        new DyHierarchyLevel("source.date", DyHierarchyLevelType.Month),
                        new DyHierarchyLevel("source.date", DyHierarchyLevelType.Day)));

        dyhiService.generate(agg);

        Folder year = folderService.get(f.getId(), "2014");
        assertEquals(5, searchService.search(year.getSearch()).getHits().getTotalHits());

        Folder month = folderService.get(year.getId(), "July");
        assertEquals(5, searchService.search(month.getSearch()).getHits().getTotalHits());

        Folder day = folderService.get(month.getId(), "4");
        assertEquals(5, searchService.search(day.getSearch()).getHits().getTotalHits());

        year = folderService.get(f.getId(), "2013");
        assertEquals(2, searchService.search(year.getSearch()).getHits().getTotalHits());
    }

    @Test
    public void testGenerateAndUpdate() {

        Folder folder = folderService.create(new FolderSpec("foo"), false);
        DyHierarchy agg = new DyHierarchy();
        agg.setFolderId(folder.getId());
        agg.setLevels(
                ImmutableList.of(
                        new DyHierarchyLevel("source.date", DyHierarchyLevelType.Year),
                        new DyHierarchyLevel("source.type.raw"),
                        new DyHierarchyLevel("source.extension.raw"),
                        new DyHierarchyLevel("source.filename.raw")));

        dyhiService.generate(agg);

        for (File f: getTestImagePath("set02").toFile().listFiles()) {
            if (!f.isFile()) {
                continue;
            }
            Source ab = new Source(f);
            assetService.index(ab);
        }

        for (File f: getTestImagePath("set03").toFile().listFiles()) {
            if (!f.isFile()) {
                continue;
            }
            Source ab = new Source(f);
            assetService.index(ab);
        }

        refreshIndex();
        dyhiService.generate(agg);
    }

    @Test
    public void testUpdate() {
        Folder folder = folderService.create(new FolderSpec("foo"), false);
        DyHierarchySpec spec = new DyHierarchySpec();
        spec.setFolderId(folder.getId());
        spec.setLevels(
                ImmutableList.of(
                        new DyHierarchyLevel("source.date", DyHierarchyLevelType.Day),
                        new DyHierarchyLevel("source.type.raw"),
                        new DyHierarchyLevel("source.extension.raw"),
                        new DyHierarchyLevel("source.filename.raw")));
        DyHierarchy dyhi = dyhiService.create(spec);
        folder = folderService.get(folder.getId());
        assertTrue(folder.getSearch().getFilter().getExists().contains("source.date"));

        dyhi.setLevels(ImmutableList.of(
                new DyHierarchyLevel("source.type.raw"),
                new DyHierarchyLevel("source.extension.raw"),
                new DyHierarchyLevel("source.filename.raw")));
        dyhiService.update(dyhi.getId(), dyhi);
        folder = folderService.get(folder.getId());
        assertTrue(folder.getSearch().getFilter().getExists().contains("source.type.raw"));
    }

    @Test
    public void testUpdateWithSmartQuery() {
        FolderSpec fspec = new FolderSpec("foo").setSearch(new AssetSearch("beer"));
        Folder folder = folderService.create(fspec, false);
        DyHierarchySpec spec = new DyHierarchySpec();
        spec.setFolderId(folder.getId());
        spec.setLevels(
                ImmutableList.of(
                        new DyHierarchyLevel("source.date", DyHierarchyLevelType.Day),
                        new DyHierarchyLevel("source.type.raw"),
                        new DyHierarchyLevel("source.extension.raw"),
                        new DyHierarchyLevel("source.filename.raw")));
        DyHierarchy dyhi = dyhiService.create(spec);
        folder = folderService.get(folder.getId());
        assertTrue(folder.getSearch().getFilter().getExists().contains("source.date"));
        assertEquals("beer", folder.getSearch().getQuery());
        assertEquals(1, folder.getSearch().getFilter().getExists().size());

        dyhi.setLevels(ImmutableList.of(
                new DyHierarchyLevel("source.type.raw"),
                new DyHierarchyLevel("source.extension.raw"),
                new DyHierarchyLevel("source.filename.raw")));
        dyhiService.update(dyhi.getId(), dyhi);
        folder = folderService.get(folder.getId());
        assertTrue(folder.getSearch().getFilter().getExists().contains("source.type.raw"));
        assertEquals(1, folder.getSearch().getFilter().getExists().size());
        assertEquals("beer", folder.getSearch().getQuery());
    }
}
