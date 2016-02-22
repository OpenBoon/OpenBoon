package com.zorroa.archivist.repository;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.zorroa.archivist.ArchivistApplicationTests;
import com.zorroa.archivist.sdk.domain.*;
import com.zorroa.archivist.sdk.processor.ProcessorFactory;
import com.zorroa.archivist.sdk.processor.export.ExportProcessor;
import com.zorroa.archivist.sdk.schema.PermissionSchema;
import com.zorroa.archivist.sdk.schema.SourceSchema;
import com.zorroa.archivist.sdk.util.Json;
import com.zorroa.common.repository.AssetDao;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

public class AssetDaoTests extends ArchivistApplicationTests {

    @Autowired
    AssetDao assetDao;

    @Test
    public void testUpsertAndGet() throws IOException {

        String filename = UUID.randomUUID().toString() + ".jpg";
        String filepath = "/tmp/" + filename;
        Files.touch(new File(filepath));

        AssetBuilder builder = new AssetBuilder(filepath);
        Asset asset1 = assetDao.upsert(builder);
        refreshIndex();

        Asset asset2 = assetDao.get(asset1.getId());
        SourceSchema source = asset2.getSchema("source", SourceSchema.class);
        assertEquals("jpg", source.getExtension());
        assertEquals("/tmp", source.getDirectory());
        assertEquals(filepath, source.getPath());
        assertEquals(filename, source.getFilename());
    }

    @Test
    public void testGetAll() {
        AssetBuilder builder = new AssetBuilder(getTestImage("beer_kettle_01.jpg"));
        assetDao.upsert(builder);
        refreshIndex(100);
        assertEquals(1, assetDao.getAll().size());
    }

    @Test
    public void testExistsByPath() {
        assertFalse(assetDao.existsByPath(getTestImage("beer_kettle_01.jpg").toString()));
        AssetBuilder builder = new AssetBuilder(getTestImage("beer_kettle_01.jpg"));
        assetDao.upsert(builder);
        refreshIndex(100);
        assertTrue(assetDao.existsByPath(getTestImage("beer_kettle_01.jpg").toString()));
    }

    @Test
    public void testUpdateRating() {
        AssetBuilder builder = new AssetBuilder(getTestImage("beer_kettle_01.jpg"));
        Asset asset = assetDao.upsert(builder);
        refreshIndex();

        AssetUpdateBuilder updateBuilder = new AssetUpdateBuilder();
        updateBuilder.setRating(3);

        assetDao.update(asset.getId(), updateBuilder);
        Asset updatedAsset = assetDao.get(asset.getId());
        assertEquals(new Integer(3), updatedAsset.getAttr("user.rating"));
    }

    @Test
    public void testUpdatePermissions() {
        AssetBuilder builder = new AssetBuilder(getTestImage("beer_kettle_01.jpg"));
        Asset asset = assetDao.upsert(builder);
        refreshIndex();

        AssetUpdateBuilder updateBuilder = new AssetUpdateBuilder();
        updateBuilder.setPermissions(new PermissionSchema()
                .setWrite(Sets.newHashSet(1,2))
                .setSearch(Sets.newHashSet(3,4))
                .setExport(Sets.newHashSet(5)));

        assetDao.update(asset.getId(), updateBuilder);
        Asset updatedAsset = assetDao.get(asset.getId());

        PermissionSchema updatedPermissions = updatedAsset.getSchema("permissions", PermissionSchema.class);
        assertEquals(Sets.newHashSet(1,2), updatedPermissions.getWrite());
        assertEquals(Sets.newHashSet(3,4), updatedPermissions.getSearch());
        assertEquals(Sets.newHashSet(5), updatedPermissions.getExport());
    }

    @Test
    public void testAddAssetToFolder() {

        AssetBuilder builder = new AssetBuilder(getTestImage("beer_kettle_01.jpg"));
        Asset asset = assetDao.upsert(builder);

        Folder folder = folderService.create(new FolderBuilder("foo"));

        assetDao.addToFolder(folder, Lists.newArrayList(asset.getId(), asset.getId()));
        assetDao.addToFolder(folder, Lists.newArrayList(asset.getId(), asset.getId()));

        asset = assetDao.get(asset.getId());
        assertTrue(((List) asset.getAttr("folders")).contains(folder.getId()));
        assertEquals(1, ((List) asset.getAttr("folders")).size());
    }

    @Test
    public void testRemoveFromFolder() {
        AssetBuilder builder = new AssetBuilder(getTestImage("beer_kettle_01.jpg"));
        Asset asset = assetDao.upsert(builder);

        Folder folder = folderService.create(new FolderBuilder("foo"));
        assetDao.addToFolder(folder, Lists.newArrayList(asset.getId()));
        refreshIndex();

        asset = assetDao.get(asset.getId());
        assertTrue(((List) asset.getAttr("folders")).contains(folder.getId()));

        assetDao.removeFromFolder(folder, Lists.newArrayList(asset.getId()));
        refreshIndex();

        asset = assetDao.get(asset.getId());
        logger.info(Json.serializeToString(asset.getAttr("folders")));

        assertFalse(((List) asset.getAttr("folders")).contains(folder.getId()));
    }

    @Test
    public void testAddAssetToExport() {

        AssetBuilder assetBuilder = new AssetBuilder(getTestImage("beer_kettle_01.jpg"));
        Asset asset = assetDao.upsert(assetBuilder);

        refreshIndex(100);

        ExportOptions options = new ExportOptions();
        options.getImages().setFormat("jpg");
        options.getImages().setScale(.5);

        AssetSearch search = new AssetSearch();
        search.setQuery("beer");

        ProcessorFactory<ExportProcessor> outputFactory = new ProcessorFactory<>();
        outputFactory.setKlass("com.zorroa.archivist.sdk.processor.export.ZipFileExport");

        ExportBuilder builder = new ExportBuilder();
        builder.setNote("An export for Bob");
        builder.setOptions(options);
        builder.setSearch(search);
        builder.setOutputs(Lists.newArrayList(outputFactory));

        Export export = exportService.create(builder);
        assetDao.addToExport(asset, export);
        assetDao.addToExport(asset, export);
        refreshIndex(100);

        asset = assetDao.get(asset.getId());
        assertTrue(((List) asset.getAttr("exports")).contains(export.getId()));
        assertEquals(1, ((List) asset.getAttr("exports")).size());
    }

    @Test
    public void testBulkUpsertErrorRecovery() {
        AssetBuilder assetBuilder = new AssetBuilder(getTestImage("beer_kettle_01.jpg"));
        assetBuilder.setAttr("foo", "bar", 1.0);
        assetDao.bulkUpsert(Lists.newArrayList(assetBuilder));
        refreshIndex();

        assetBuilder = new AssetBuilder(getTestImage("new_zealand_wellington_harbour.jpg"));
        assetBuilder.setAttr("foo", "bar", "bing");
        AnalyzeResult result = assetDao.bulkUpsert(Lists.newArrayList(assetBuilder));
        assertEquals(1, result.created);
        assertEquals(1, result.retries);

    }
}
