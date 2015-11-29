package com.zorroa.archivist.repository;

import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.zorroa.archivist.ArchivistApplicationTests;
import com.zorroa.archivist.sdk.domain.*;
import com.zorroa.archivist.sdk.processor.ProcessorFactory;
import com.zorroa.archivist.sdk.processor.export.ExportProcessor;
import com.zorroa.archivist.sdk.schema.SourceSchema;
import com.zorroa.archivist.sdk.service.ExportService;
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

    @Autowired
    ExportService exportService;

    @Test
    public void testCreateAndGet() throws IOException {

        String filename = UUID.randomUUID().toString() + ".jpg";
        String filepath = "/tmp/" + filename;
        Files.touch(new File(filepath));

        AssetBuilder builder = new AssetBuilder(filepath);
        Asset asset1 = assetDao.create(builder);
        refreshIndex(100);

        Asset asset2 = assetDao.get(asset1.getId());
        SourceSchema source = asset2.getValue("source", SourceSchema.class);
        assertEquals("jpg", source.getExtension());
        assertEquals("/tmp", source.getDirectory());
        assertEquals(filepath, source.getPath());
        assertEquals(filename, source.getFilename());
    }

    @Test
    public void testGetAll() {
        AssetBuilder builder = new AssetBuilder(getTestImage("beer_kettle_01.jpg"));
        assetDao.create(builder);
        refreshIndex(100);
        assertEquals(1, assetDao.getAll().size());
    }

    @Test
    public void testReplace() {
        AssetBuilder builder = new AssetBuilder(getTestImage("beer_kettle_01.jpg"));
        assetDao.replace(builder);
        assetDao.replace(builder);
    }

    @Test
    public void testExistsByPath() {
        assertFalse(assetDao.existsByPath(getTestImage("beer_kettle_01.jpg").toString()));
        AssetBuilder builder = new AssetBuilder(getTestImage("beer_kettle_01.jpg"));
        assetDao.replace(builder);
        refreshIndex(100);
        assertTrue(assetDao.existsByPath(getTestImage("beer_kettle_01.jpg").toString()));
    }

    @Test
    public void testUpdate() {
        AssetBuilder builder = new AssetBuilder(getTestImage("beer_kettle_01.jpg"));
        Asset asset = assetDao.create(builder);
        refreshIndex(100);
        AssetUpdateBuilder updateBuilder = new AssetUpdateBuilder();
        updateBuilder.put("Xmp", "Rating", new Integer(3));
        assetDao.update(asset.getId(), updateBuilder);
        Asset updatedAsset = assetDao.get(asset.getId());
        assertEquals(new Integer(3), updatedAsset.getValue("Xmp.Rating"));
    }

    @Test
    public void testAddAssetToExport() {
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

        AssetBuilder assetBuilder = new AssetBuilder(getTestImage("beer_kettle_01.jpg"));
        Asset asset = assetDao.create(assetBuilder);

        assetDao.addToExport(asset, export);

        asset = assetDao.get(asset.getId());
        assertTrue(((List)asset.getValue("exports")).contains(export.getId()));
    }
}
