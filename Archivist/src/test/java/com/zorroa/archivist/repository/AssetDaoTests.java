package com.zorroa.archivist.repository;

import com.google.common.io.Files;
import com.zorroa.archivist.ArchivistApplicationTests;
import com.zorroa.archivist.domain.Asset;
import com.zorroa.archivist.sdk.AssetBuilder;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import static org.junit.Assert.*;

public class AssetDaoTests extends ArchivistApplicationTests {

    @Autowired
    AssetDao assetDao;

    @Test
    public void testCreateAndGet() throws IOException {

        String filename = UUID.randomUUID().toString() + ".jpg";
        String filepath = "/tmp/" + filename;
        Files.touch(new File(filepath));

        AssetBuilder builder = new AssetBuilder(filepath);
        Asset asset1 = assetDao.create(builder);
        refreshIndex(100);

        Asset asset2 = assetDao.get(asset1.getId());
        assertEquals("jpg", asset2.getValue("source.extension"));
        assertEquals("/tmp", asset2.getValue("source.directory"));
        assertEquals(filepath, asset2.getValue("source.path"));
        assertEquals(filename, asset2.getValue("source.filename"));

    }

    @Test
    public void testGetAll() {
        AssetBuilder builder = new AssetBuilder(getTestImage("beer_kettle_01.jpg"));
        assetDao.create(builder);
        refreshIndex(100);
        assertEquals(1, assetDao.getAll().size());
    }

    @Test
    public void testFastCreate() {
        AssetBuilder builder = new AssetBuilder(getTestImage("beer_kettle_01.jpg"));
        assetDao.fastCreate(builder);
        assetDao.fastCreate(builder);
    }

    @Test
    public void testExistsByPath() {
        assertFalse(assetDao.existsByPath(getTestImage("beer_kettle_01.jpg").toString()));
        AssetBuilder builder = new AssetBuilder(getTestImage("beer_kettle_01.jpg"));
        assetDao.fastCreate(builder);
        refreshIndex(100);
        assertTrue(assetDao.existsByPath(getTestImage("beer_kettle_01.jpg").toString()));
    }
}
