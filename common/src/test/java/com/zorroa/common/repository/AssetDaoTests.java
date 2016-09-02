package com.zorroa.common.repository;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.zorroa.common.AbstractTest;
import com.zorroa.common.domain.PagedList;
import com.zorroa.common.domain.Paging;
import com.zorroa.sdk.domain.Asset;
import com.zorroa.sdk.domain.AssetIndexResult;
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
        Source builder = new Source(getTestImagePath("set04/standard/beer_kettle_01.jpg"));
        asset1 = assetDao.index(builder, null);
        refreshIndex();
    }

    @Test
    public void testGetById() {
        Asset asset2 = assetDao.get(asset1.getId());
        assertEquals(asset1.getId(), asset2.getId());
    }

    @Test
    public void testGetByPath() {
        Path p = getTestImagePath("set04/standard/beer_kettle_01.jpg");
        Asset asset2 = assetDao.get(p);
        assertNotNull(asset2);
    }

    @Test
    public void testExistsByPath() {
        Path p = getTestImagePath("set04/standard/beer_kettle_01.jpg");
        assertTrue(assetDao.exists(p));
    }

    @Test
    public void testGetAll() {
        PagedList<Asset> assets = assetDao.getAll(Paging.first(10));
        assertEquals(1, assets.getList().size());
    }

    @Test
    public void testBatchUpsert() {
        Source source1 = new Source(getTestImagePath("set04/standard/beer_kettle_01.jpg"));
        Source source2 = new Source(getTestImagePath("set04/standard/new_zealand_wellington_harbour.jpg"));

        AssetIndexResult result = assetDao.index(ImmutableList.of(source1, source2), null);
        refreshIndex();
        assertEquals(1, result.created);
        assertEquals(1, result.updated);
    }

    @Test
    public void testAppendLink() {
        assertTrue(assetDao.appendLink("folder", "100",
                ImmutableList.of(asset1.getId())).get(asset1.getId()));
        assertTrue(assetDao.appendLink("parent", "foo",
                ImmutableList.of(asset1.getId())).get(asset1.getId()));

        Asset a = assetDao.get(asset1.getId());
        assertEquals(2, ((List) a.getAttr("links")).size());
    }

    @Test
    public void testRemoveLink() {
        assertTrue(assetDao.appendLink("folder", "100",
                ImmutableList.of(asset1.getId())).get(asset1.getId()));

        Asset a = assetDao.get(asset1.getId());
        assertEquals(1, ((List) a.getAttr("links")).size());

        assertTrue(assetDao.removeLink("folder", "100",
                ImmutableList.of(asset1.getId())).get(asset1.getId()));

        a = assetDao.get(asset1.getId());
        assertEquals(0, ((List) a.getAttr("links")).size());
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
