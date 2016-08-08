package com.zorroa.archivist.service;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.zorroa.archivist.AbstractTest;
import com.zorroa.archivist.domain.Folder;
import com.zorroa.archivist.domain.FolderSpec;
import com.zorroa.archivist.domain.Permission;
import com.zorroa.archivist.domain.PermissionSpec;
import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.common.repository.AssetDao;
import com.zorroa.sdk.domain.Asset;
import com.zorroa.sdk.domain.Color;
import com.zorroa.sdk.processor.Source;
import com.zorroa.sdk.schema.LocationSchema;
import com.zorroa.sdk.schema.SourceSchema;
import com.zorroa.sdk.search.AssetFilter;
import com.zorroa.sdk.search.AssetSearch;
import com.zorroa.sdk.search.ColorFilter;
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

        Permission perm = userService.createPermission(new PermissionSpec("group", "test"));
        Source source = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));

        SecurityUtils.setReadPermissions(source, Lists.newArrayList(perm));
        Asset asset1 = assetDao.index(source);
        refreshIndex(100);

        AssetSearch search = new AssetSearch().setQuery("beer");
        assertEquals(0, searchService.search(search).getHits().getTotalHits());
    }

    @Test
    public void testSearchPermissionsHit() throws IOException {
        authenticate("admin");

        Permission perm = userService.createPermission(new PermissionSpec("group", "test"));
        Source source = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        source.addKeywords("source", "captain");
        /*
         * Add a permission from the current user to the asset.
         */
        SecurityUtils.setReadPermissions(source, Lists.newArrayList(userService.getPermissions(SecurityUtils.getUser()).get(0)));
        Asset asset1 = assetDao.index(source);
        refreshIndex(100);

        AssetSearch search = new AssetSearch().setQuery("captain");
        assertEquals(1, searchService.search(search).getHits().getTotalHits());
    }

    @Test
    public void testFolderSearch() throws IOException {

        FolderSpec builder = new FolderSpec("Avengers");
        Folder folder1 = folderService.create(builder);

        Source source = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        source.addKeywords("source", source.getAttr("source.filename", String.class));
        Asset asset1 = assetDao.index(source);
        refreshIndex(100);

        folderService.addAssets(folder1, Lists.newArrayList(asset1.getId()));
        refreshIndex(100);

        AssetFilter filter = new AssetFilter().setFolders(Lists.newArrayList(folder1.getId()));
        AssetSearch search = new AssetSearch().setFilter(filter);
        assertEquals(1, searchService.search(search).getHits().getTotalHits());
    }

    @Test
    public void testRecursiveFolderSearch() throws IOException {

        FolderSpec builder = new FolderSpec("Avengers");
        Folder folder1 = folderService.create(builder);

        builder = new FolderSpec("Age Of Ultron", folder1);
        Folder folder2 = folderService.create(builder);

        builder = new FolderSpec("Characters", folder2);
        Folder folder3 = folderService.create(builder);

        Source source = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        Asset asset1 = assetDao.index(source);
        refreshIndex(100);

        folderService.addAssets(folder3, Lists.newArrayList(asset1.getId()));
        refreshIndex(100);

        AssetFilter filter = new AssetFilter().setFolders(Lists.newArrayList(folder1.getId()));
        AssetSearch search = new AssetSearch().setFilter(filter);
        assertEquals(1, searchService.search(search).getHits().getTotalHits());
    }

    @Test
    public void testNonRecursiveFolderSearch() throws IOException {

        FolderSpec builder = new FolderSpec("Avengers");
        Folder folder1 = folderService.create(builder);

        builder = new FolderSpec("Age Of Ultron", folder1).setRecursive(false);
        Folder folder2 = folderService.create(builder);

        builder = new FolderSpec("Characters", folder2);
        Folder folder3 = folderService.create(builder);

        Source source1 = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        source1.addKeywords("source", source1.getAttr("source", SourceSchema.class).getFilename());

        Source source2 = new Source(getTestImagePath().resolve("new_zealand_wellington_harbour.jpg"));
        source2.addKeywords("source", source2.getAttr("source", SourceSchema.class).getFilename());

        Asset asset1 = assetDao.index(source1);
        Asset asset2 = assetDao.index(source2);

        refreshIndex(100);

        folderService.addAssets(folder2, Lists.newArrayList(asset2.getId()));
        folderService.addAssets(folder3, Lists.newArrayList(asset1.getId()));
        refreshIndex(100);

        AssetFilter filter = new AssetFilter().setFolders(Lists.newArrayList(folder1.getId()));
        AssetSearch search = new AssetSearch().setFilter(filter);
        assertEquals(1, searchService.search(search).getHits().getTotalHits());
    }

    @Test
    public void testSmartFolderSearch() throws IOException {

        FolderSpec builder = new FolderSpec("Avengers");
        Folder folder1 = folderService.create(builder);

        builder = new FolderSpec("Age Of Ultron", folder1);
        Folder folder2 = folderService.create(builder);

        builder = new FolderSpec("Characters", folder2);
        builder.setSearch(new AssetSearch("captain america"));
        Folder folder3 = folderService.create(builder);

        String filename = "captain_america.jpg";
        String filepath = "/tmp/" + filename;
        Files.touch(new File(filepath));

        Source source = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        source.addKeywords("source", "captain");

        assetDao.index(source);
        refreshIndex();

        AssetFilter filter = new AssetFilter().setFolders(Lists.newArrayList(folder1.getId()));
        AssetSearch search = new AssetSearch().setFilter(filter);
        assertEquals(1, searchService.search(search).getHits().getTotalHits());
    }

    @Test
    public void testLotsOfSmartFolders() throws IOException {

        FolderSpec builder = new FolderSpec("people");
        Folder folder1 = folderService.create(builder);

        for (int i=0; i<100; i++) {
            builder = new FolderSpec("person" + i, folder1);
            builder.setSearch(new AssetSearch("beer"));
            folderService.create(builder);
        }

        refreshIndex();

        Source source = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        source.addKeywords("source", source.getAttr("source", SourceSchema.class).getFilename());

        assetDao.index(source);
        refreshIndex();

        AssetFilter filter = new AssetFilter().setFolders(Lists.newArrayList(folder1.getId()));
        AssetSearch search = new AssetSearch().setFilter(filter);
        assertEquals(1, searchService.search(search).getHits().getTotalHits());
    }

    @Test
    public void testHighConfidenceSearch() throws IOException {

        Source Source = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        Source.addKeywords("source", "zipzoom");
        assetDao.index(Source);
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

        Source Source = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        Source.addKeywords("source","zipzoom");
        assetDao.index(Source);
        refreshIndex();

        assertEquals(1, searchService.search(
                new AssetSearch("zipzoom")).getHits().getTotalHits());
    }

    @Test
    public void testFuzzySearch() throws IOException {

        Source Source = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        Source.addKeywords("source", "zoolander");
        assetDao.index(Source);
        refreshIndex();

        assertEquals(1, searchService.search(
                new AssetSearch("zoolandar").setFuzzy(true)).getHits().getTotalHits());
    }

    @Test
    public void testDoubleFuzzySearch() throws IOException {
        /**
         * Handles the case where the client specified ~
         */
        Source Source = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        Source.addKeywords("source", "zoolander~");
        assetDao.index(Source);
        refreshIndex();

        assertEquals(1, searchService.search(
                new AssetSearch("zoolandar").setFuzzy(true)).getHits().getTotalHits());
    }

    @Test
    public void getFields() {

        Source Source = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        Source.setAttr("location", new LocationSchema(new double[] {1.0, 2.0}).setCountry("USA"));
        assetDao.index(Source);
        refreshIndex();

        Map<String, Set<String>> fields = searchService.getFields();
        assertTrue(fields.get("date").size() > 0);
        assertTrue(fields.get("string").size() > 0);
        assertTrue(fields.get("integer").size() > 0);
        assertTrue(fields.get("point").size() > 0);
    }


    @Test
    public void testColorSearch() {
        Color color = new Color(255, 10, 10).setRatio(50f);

        Source Source = new Source(getTestImagePath().resolve("beer_kettle_01.jpg"));
        Source.setAttr("colors.original", ImmutableList.of(color));
        assetDao.index(Source);
        refreshIndex();

        assertEquals(1, searchService.search(
                new AssetSearch().setFilter(new AssetFilter().addToColors(
                        new ColorFilter()
                        .setField("colors.original")
                        .setMinRatio(45)
                        .setMaxRatio(55)
                        .setHueAndRange(0, 5)
                        .setSaturationAndRange(100, 5)
                        .setBrightnessAndRange(50, 5)))).getHits().getTotalHits());
    }
}
