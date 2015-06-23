package com.zorroa.archivist.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.elasticsearch.index.engine.DocumentAlreadyExistsException;
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
        Asset asset1 = assetDao.create(builder);
        refreshIndex(100);

        Asset asset2 = assetDao.get(asset1.getId());
        assertEquals("jpg", asset2.getString("source.extension"));
        assertEquals(getStaticImagePath(), asset2.getString("source.directory"));
        assertEquals(getStaticImagePath() + "/beer_kettle_01.jpg", asset2.getString("source.path"));
        assertEquals("beer_kettle_01.jpg", asset2.getString("source.filename"));

    }

    @Test
    public void testGetAll() {
        AssetBuilder builder = new AssetBuilder(getTestImage("beer_kettle_01.jpg"));
        assetDao.create(builder);
        refreshIndex(100);
        assertEquals(1, assetDao.getAll().size());
    }

    @Test(expected=DocumentAlreadyExistsException.class)
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
