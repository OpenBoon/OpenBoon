package com.zorroa.archivist.repository;

import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.zorroa.archivist.ArchivistApplicationTests;
import com.zorroa.archivist.domain.Asset;
import com.zorroa.archivist.domain.AssetBuilder;

public class AssetDaoTests extends ArchivistApplicationTests {

    @Autowired
    AssetDao assetDao;

    @Test
    public void testCreateAndGet() {
        AssetBuilder builder = new AssetBuilder(getTestImage("beer_kettle_01.jpg"));
        String id = assetDao.create(builder);
        refreshIndex(100);

        Asset asset = assetDao.get(id);
        assertEquals("jpg", asset.getString("source.extension"));
        assertEquals(getStaticImagePath(), asset.getString("source.directory"));
        assertEquals(getStaticImagePath() + "/beer_kettle_01.jpg", asset.getString("source.path"));
        assertEquals("beer_kettle_01.jpg", asset.getString("source.filename"));

    }

    @Test
    public void testGetAll() {
        AssetBuilder builder = new AssetBuilder(getTestImage("beer_kettle_01.jpg"));
        assetDao.create(builder);
        refreshIndex(100);
        assertEquals(1, assetDao.getAll().size());
    }

}
