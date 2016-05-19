package com.zorroa.archivist.repository;

import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.zorroa.archivist.AbstractTest;
import com.zorroa.common.repository.AssetDao;
import com.zorroa.sdk.domain.*;
import com.zorroa.sdk.processor.ProcessorFactory;
import com.zorroa.sdk.processor.export.ExportProcessor;
import com.zorroa.sdk.schema.SourceSchema;
import com.zorroa.sdk.util.Json;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

public class AssetDaoTests extends AbstractTest {

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
        SourceSchema source = asset2.getAttr("source", SourceSchema.class);
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
        assetBuilder.addKeywords("source", assetBuilder.getFilename());
        Asset asset = assetDao.upsert(assetBuilder);

        refreshIndex(100);

        ExportOptions options = new ExportOptions();
        options.getImages().setFormat("jpg");
        options.getImages().setScale(.5);

        AssetSearch search = new AssetSearch();
        search.setQuery("beer");

        ProcessorFactory<ExportProcessor> outputFactory = new ProcessorFactory<>();
        outputFactory.setKlass("com.zorroa.sdk.processor.export.ZipFileExport");

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
        assetBuilder.setAttr("foo.bomb", 1.0);
        assetDao.bulkUpsert(Lists.newArrayList(assetBuilder));
        refreshIndex();

        assetBuilder = new AssetBuilder(getTestImage("new_zealand_wellington_harbour.jpg"));
        assetBuilder.setAttr("foo.bomb", "bing");

        AnalyzeResult result = assetDao.bulkUpsert(Lists.newArrayList(assetBuilder));
        logger.info("{}", result);
        logger.info("{}", result.logs);
        assertEquals(1, result.created);
        assertEquals(1, result.retries);
    }
}
