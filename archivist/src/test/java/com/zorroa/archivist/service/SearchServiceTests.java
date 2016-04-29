package com.zorroa.archivist.service;

import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.zorroa.archivist.AbstractTest;
import com.zorroa.sdk.domain.*;
import com.zorroa.sdk.schema.LocationSchema;
import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.common.repository.AssetDao;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by chambers on 10/30/15.
 */
public class SearchServiceTests extends AbstractTest {

    @Autowired
    AssetDao assetDao;

    @Test
    public void testSearchPermissionsMiss() throws IOException {

        String filename = "captain_america.jpg";
        String filepath = "/tmp/" + filename;
        Files.touch(new File(filepath));

        Permission perm = userService.createPermission(new PermissionBuilder("group", "test"));

        AssetBuilder builder = new AssetBuilder(filepath);
        builder.setSearchPermissions(Lists.newArrayList(perm));
        Asset asset1 = assetDao.upsert(builder);
        refreshIndex(100);

        AssetSearch search = new AssetSearch().setQuery("captain");
        assertEquals(0, searchService.search(search).getHits().getTotalHits());

    }

    @Test
    public void testSearchPermissionsHit() throws IOException {
        authenticate("admin");
        String filename = "captain_america.jpg";
        String filepath = "/tmp/" + filename;
        Files.touch(new File(filepath));

        AssetBuilder builder = new AssetBuilder(filepath);
        builder.addKeywords("source", builder.getFilename());
        /*
         * Add a permission from the current user to the asset.
         */
        builder.setSearchPermissions(
                Lists.newArrayList(userService.getPermissions(SecurityUtils.getUser()).get(0)));
        Asset asset1 = assetDao.upsert(builder);
        refreshIndex(100);

        AssetSearch search = new AssetSearch().setQuery("captain");
        assertEquals(1, searchService.search(search).getHits().getTotalHits());
    }

    @Test
    public void testFolderSearch() throws IOException {

        FolderBuilder builder = new FolderBuilder("Avengers");
        Folder folder1 = folderService.create(builder);

        String filename = "captain_america.jpg";
        String filepath = "/tmp/" + filename;
        Files.touch(new File(filepath));

        AssetBuilder assetBuilder = new AssetBuilder(filepath);
        assetBuilder.addKeywords("source", assetBuilder.getFilename());
        Asset asset1 = assetDao.upsert(assetBuilder);
        refreshIndex(100);

        folderService.addAssets(folder1, Lists.newArrayList(asset1.getId()));
        refreshIndex(100);

        AssetFilter filter = new AssetFilter().setFolderIds(Lists.newArrayList(folder1.getId()));
        AssetSearch search = new AssetSearch().setFilter(filter);
        assertEquals(1, searchService.search(search).getHits().getTotalHits());
    }

    @Test
    public void testRecursiveFolderSearch() throws IOException {

        FolderBuilder builder = new FolderBuilder("Avengers");
        Folder folder1 = folderService.create(builder);

        builder = new FolderBuilder("Age Of Ultron", folder1);
        Folder folder2 = folderService.create(builder);

        builder = new FolderBuilder("Characters", folder2);
        Folder folder3 = folderService.create(builder);

        String filename = "captain_america.jpg";
        String filepath = "/tmp/" + filename;
        Files.touch(new File(filepath));

        AssetBuilder assetBuilder = new AssetBuilder(filepath);
        Asset asset1 = assetDao.upsert(assetBuilder);
        refreshIndex(100);

        folderService.addAssets(folder3, Lists.newArrayList(asset1.getId()));
        refreshIndex(100);

        AssetFilter filter = new AssetFilter().setFolderIds(Lists.newArrayList(folder1.getId()));
        AssetSearch search = new AssetSearch().setFilter(filter);
        assertEquals(1, searchService.search(search).getHits().getTotalHits());
    }

    @Test
    public void testSmartFolderSearch() throws IOException {

        FolderBuilder builder = new FolderBuilder("Avengers");
        Folder folder1 = folderService.create(builder);

        builder = new FolderBuilder("Age Of Ultron", folder1);
        Folder folder2 = folderService.create(builder);

        builder = new FolderBuilder("Characters", folder2);
        builder.setSearch(new AssetSearch("captain america"));
        Folder folder3 = folderService.create(builder);

        String filename = "captain_america.jpg";
        String filepath = "/tmp/" + filename;
        Files.touch(new File(filepath));

        AssetBuilder assetBuilder = new AssetBuilder(filepath);
        assetBuilder.addKeywords("source", assetBuilder.getFilename());
        Asset asset1 = assetDao.upsert(assetBuilder);
        refreshIndex(100);

        AssetFilter filter = new AssetFilter().setFolderIds(Lists.newArrayList(folder1.getId()));
        AssetSearch search = new AssetSearch().setFilter(filter);
        assertEquals(1, searchService.search(search).getHits().getTotalHits());
    }

    @Test
    public void testLotsOfSmartFolders() throws IOException {

        FolderBuilder builder = new FolderBuilder("people");
        Folder folder1 = folderService.create(builder);

        for (int i=0; i<100; i++) {
            builder = new FolderBuilder("person" + i, folder1);
            builder.setSearch(new AssetSearch("captain america"));
            folderService.create(builder);
        }

        refreshIndex();

        String filename = "captain_america.jpg";
        String filepath = "/tmp/" + filename;
        Files.touch(new File(filepath));

        AssetBuilder assetBuilder = new AssetBuilder(filepath);
        assetBuilder.addKeywords("source", assetBuilder.getFilename());
        Asset asset1 = assetDao.upsert(assetBuilder);
        refreshIndex();

        AssetFilter filter = new AssetFilter().setFolderIds(Lists.newArrayList(folder1.getId()));
        AssetSearch search = new AssetSearch().setFilter(filter);
        assertEquals(1, searchService.search(search).getHits().getTotalHits());
    }

    @Test
    public void testGetTotalFileSize() {

        AssetBuilder assetBuilder1 = new AssetBuilder(getStaticImagePath() + "/beer_kettle_01.jpg");
        assetBuilder1.addKeywords("source", assetBuilder1.getFilename());
        assetBuilder1.getSource().setFileSize(1000L);
        AssetBuilder assetBuilder2 = new AssetBuilder(getStaticImagePath() + "/new_zealand_wellington_harbour.jpg");
        assetBuilder2.addKeywords("source", assetBuilder1.getFilename());
        assetBuilder2.getSource().setFileSize(1000L);

        assetDao.upsert(assetBuilder1);
        assetDao.upsert(assetBuilder2);
        refreshIndex();

        long size = searchService.getTotalFileSize(new AssetSearch());
        assertEquals(2000, size);
    }

    @Test
    public void testHighConfidenceSearch() throws IOException {

        AssetBuilder assetBuilder = new AssetBuilder(getStaticImagePath() + "/beer_kettle_01.jpg");
        assetBuilder.addKeywords("source", "zipzoom");
        assetDao.upsert(assetBuilder);
        refreshIndex();

        /*
         * High confidence words are found at every level.
         */
        assertEquals(1, searchService.search(
                new AssetSearch("zipzoom")).getHits().getTotalHits());
        assertEquals(1, searchService.search(
                new AssetSearch("zipzoom")).getHits().getTotalHits());
        assertEquals(1, searchService.search(
                new AssetSearch("zipzoom")).getHits().getTotalHits());
    }

    @Test
    public void testNoConfidenceSearch() throws IOException {

        AssetBuilder assetBuilder = new AssetBuilder(getStaticImagePath() + "/beer_kettle_01.jpg");
        assetBuilder.addKeywords("source","zipzoom");
        assetDao.upsert(assetBuilder);
        refreshIndex();

        assertEquals(1, searchService.search(
                new AssetSearch("zipzoom")).getHits().getTotalHits());
    }

    @Test
    public void testFuzzySearch() throws IOException {

        AssetBuilder assetBuilder = new AssetBuilder(getStaticImagePath() + "/beer_kettle_01.jpg");
        assetBuilder.addKeywords("source", "zoolander");
        assetDao.upsert(assetBuilder);
        refreshIndex();

        assertEquals(1, searchService.search(
                new AssetSearch("zoolandar").setFuzzy(true)).getHits().getTotalHits());
    }

    @Test
    public void testDoubleFuzzySearch() throws IOException {
        /**
         * Handles the case where the client specified ~
         */
        AssetBuilder assetBuilder = new AssetBuilder(getStaticImagePath() + "/beer_kettle_01.jpg");
        assetBuilder.addKeywords("source", "zoolander~");
        assetDao.upsert(assetBuilder);
        refreshIndex();

        assertEquals(1, searchService.search(
                new AssetSearch("zoolandar").setFuzzy(true)).getHits().getTotalHits());
    }

    @Test
    public void getFields() {

        AssetBuilder assetBuilder = new AssetBuilder(getStaticImagePath() + "/beer_kettle_01.jpg");
        assetBuilder.setAttr("location", new LocationSchema(new double[] {1.0, 2.0}).setCountry("USA"));
        assetDao.upsert(assetBuilder);
        refreshIndex();

        Map<String, Set<String>> fields = searchService.getFields();
        assertTrue(fields.get("date").size() > 0);
        assertTrue(fields.get("string").size() > 0);
        assertTrue(fields.get("integer").size() > 0);
        assertTrue(fields.get("point").size() > 0);
    }
}
