package com.zorroa.common.repository;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.zorroa.common.AbstractTest;
import com.zorroa.sdk.domain.Asset;
import com.zorroa.sdk.domain.BatchAssetUpsertResult;
import com.zorroa.sdk.domain.Folder;
import com.zorroa.sdk.processor.Source;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class AssetDaoTests extends AbstractTest {

    @Autowired
    AssetDao assetDao;

    Asset asset1;

    @Before
    public void init() {
        Source builder = new Source(getTestImagePath("standard/beer_kettle_01.jpg"));
        asset1 = assetDao.upsert(builder);
        refreshIndex();
    }

    @Test
    public void testGetById() {
        Asset asset2 = assetDao.get(asset1.getId());
        assertEquals(asset1.getId(), asset2.getId());
    }

    @Test
    public void testGetByPath() {
        Path p = getTestImagePath("standard/beer_kettle_01.jpg");
        Asset asset2 = assetDao.get(p);
        assertNotNull(asset2);
    }

    @Test
    public void testExistsByPath() {
        Path p = getTestImagePath("standard/beer_kettle_01.jpg");
        assertTrue(assetDao.exists(p));
    }

    @Test
    public void testGetAll() {
        List<Asset> assets = assetDao.getAll();
        assertEquals(1, assets.size());
    }

    @Test
    public void testBatchUpsert() {
        Source source1 = new Source(getTestImagePath("standard/beer_kettle_01.jpg"));
        Source source2 = new Source(getTestImagePath("standard/new_zealand_wellington_harbour.jpg"));

        BatchAssetUpsertResult result = assetDao.upsert(ImmutableList.of(source1, source2));
        refreshIndex();
        assertEquals(1, result.created);
        assertEquals(1, result.updated);
    }

    @Test
    public void testAddToFolder() {
        Folder f = new Folder();
        f.setId(100);
        assertEquals(1, assetDao.addToFolder(f, ImmutableList.of(asset1.getId())));
    }

    @Test
    public void testRemoveFromFolder() {
        Folder f = new Folder();
        f.setId(100);
        assertEquals(1, assetDao.addToFolder(f, ImmutableList.of(asset1.getId())));
        assertEquals(1, assetDao.removeFromFolder(f, ImmutableList.of(asset1.getId())));
    }

    @Test
    public void testUpdate() {
        Map<String, Object> attrs = Maps.newHashMap();
        attrs.put("foo.bar", 100);

        assetDao.update(asset1.getId(), attrs);
        Asset asset2 = assetDao.get(asset1.getId());
        assertEquals(100, (int) asset2.getAttr("foo.bar"));
    }

}
