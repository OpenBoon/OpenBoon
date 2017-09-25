package com.zorroa.archivist.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.*;
import com.zorroa.archivist.AbstractTest;
import com.zorroa.sdk.domain.*;
import com.zorroa.sdk.processor.Element;
import com.zorroa.sdk.processor.Source;
import com.zorroa.sdk.util.Json;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.*;

public class AssetDaoTests extends AbstractTest {

    @Autowired
    AssetDao assetDao;

    Asset asset1;

    @Before
    public void init() {
        Source builder = new Source(getTestImagePath("set04/standard/beer_kettle_01.jpg"));
        asset1 = assetDao.index(builder);
        refreshIndex();
    }

    @Test
    public void testGetById() {
        Asset asset2 = assetDao.get(asset1.getId());
        assertEquals(asset1.getId(), asset2.getId());
    }

    @Test
    public void testGetElements() {
        Asset asset2 = assetDao.get(asset1.getId());

        Element e = new Element(asset2);
        e.setAttr("foo.bar", "bing");
        e.setId(UUID.randomUUID().toString());
        assetService.index(e);
        refreshIndex();

        PagedList<Document> elements = assetDao.getElements(asset2.getId(), Pager.first());
        assertEquals(1, elements.size());
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
    public void testExistsById() {
        assertTrue(assetDao.exists(asset1.getId()));
        assertFalse(assetDao.exists("abc"));
    }

    @Test
    public void testGetAll() {
        PagedList<Asset> assets = assetDao.getAll(Pager.first(10));
        assertEquals(1, assets.getList().size());
    }

    @Test
    public void testGetAllBySearchRequest() {
        SearchRequestBuilder builder = client.prepareSearch("archivist");
        builder.setQuery(QueryBuilders.matchAllQuery());
        PagedList<Asset> assets = assetDao.getAll(Pager.first(10), builder);
        assertEquals(1, assets.getList().size());
    }

    @Test
    public void testGetAllBySearchRequestIntoStream() throws IOException {
        assetDao.index(new Source(getTestImagePath("set01/standard/faces.jpg")));
        refreshIndex();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        SearchRequestBuilder builder = client.prepareSearch("archivist");
        builder.setQuery(QueryBuilders.matchAllQuery());
        builder.addAggregation(AggregationBuilders.terms("path").field("source.path"));
        assetDao.getAll(Pager.first(10), builder, stream, ImmutableMap.of());
        logger.info(stream.toString());
        PagedList<Asset> result = Json.deserialize(stream.toString(), new TypeReference<PagedList<Asset>>() {});
        assertEquals(2, result.getList().size());
    }

    @Test
    public void testGetAllScroll() {
        assetDao.index(new Source(getTestImagePath("set01/standard/faces.jpg")));
        assetDao.index(new Source(getTestImagePath("set01/standard/hyena.jpg")));
        assetDao.index(new Source(getTestImagePath("set01/standard/toucan.jpg")));
        assetDao.index(new Source(getTestImagePath("set01/standard/visa.jpg")));
        assetDao.index(new Source(getTestImagePath("set01/standard/visa12.jpg")));
        refreshIndex();

        SearchRequestBuilder req = client.prepareSearch("archivist")
            .setQuery(ImmutableMap.of("match_all", ImmutableMap.of()))
            .setScroll("1m");

        PagedList<Asset> assets = assetDao.getAll(Pager.first(1), req);
        assertEquals(1, assets.getList().size());
        assertEquals(6, (long) assets.getPage().getTotalCount());
        assertNotNull(assets.getScroll());
        Asset asset = assets.get(0);

        assets = assetDao.getAll(assets.getScroll().getId(), "1m");
        assertEquals(1, assets.getList().size());
        assertNotNull(assets.getScroll());
        assertNotEquals(asset.getId(), assets.get(0).getId());
    }

    @Test
    public void testBatchUpsert() {
        Source source1 = new Source(getTestImagePath("set04/standard/beer_kettle_01.jpg"));
        Source source2 = new Source(getTestImagePath("set04/standard/new_zealand_wellington_harbour.jpg"));

        AssetIndexResult result = assetDao.index(ImmutableList.of(source1, source2));
        assertEquals(1, result.created);
        assertEquals(1, result.updated);

        result = assetDao.index(ImmutableList.of(source1, source2));
        assertEquals(0, result.created);
        assertEquals(2, result.updated);
    }

    @Test
    public void testAppendLink() {
        assertTrue(assetDao.appendLink("folder", 100,
                ImmutableList.of(asset1.getId())).get("success").contains(asset1.getId()));
        assertTrue(assetDao.appendLink("parent", "foo",
                ImmutableList.of(asset1.getId())).get("success").contains(asset1.getId()));

        Asset a = assetDao.get(asset1.getId());
        Collection<Object> folder_links = a.getAttr("links.folder");
        Collection<Object> parent_links = a.getAttr("links.parent");
        assertEquals(1, folder_links.size());
        assertEquals(1, parent_links.size());
        assertTrue(folder_links.contains(100));
        assertTrue(parent_links.contains("foo"));
    }

    @Test
    public void testRemoveLink() {
        assertTrue(assetDao.appendLink("folder", "100",
                ImmutableList.of(asset1.getId())).get("success").contains(asset1.getId()));

        Asset a = assetDao.get(asset1.getId());
        Collection<Object> links = a.getAttr("links.folder");
        assertEquals(1, links.size());

        assertTrue(assetDao.removeLink("folder", "100",
                ImmutableList.of(asset1.getId())).get("success").contains(asset1.getId()));

        a = assetDao.get(asset1.getId());
        links = a.getAttr("links.folder");
        assertEquals(0, links.size());
    }

    @Test
    public void testUpdate() {
        Map<String, Object> attrs = Maps.newHashMap();
        attrs.put("foo.bar", 100);

        assetDao.update(asset1.getId(), attrs);
        Asset asset2 = assetDao.get(asset1.getId());
        assertEquals(100, (int) asset2.getAttr("foo.bar"));
    }

    @Test
    public void testBatchIndexWithReplace() {

        Source source1 = new Source(getTestImagePath("set04/standard/beer_kettle_01.jpg"));
        assetDao.index(Lists.newArrayList(source1));

        source1.removeAttr("keywords");
        source1.setReplace(true);
        assetDao.index(Lists.newArrayList(source1));
        refreshIndex();

        Asset a = assetDao.get(source1.getId());
        assertFalse(a.attrExists("keywords"));
    }

    @Test
    public void testDelete() {
        assertTrue(assetDao.delete(asset1.getId()));
        refreshIndex();
        assertFalse(assetDao.delete(asset1.getId()));
    }

    @Test
    public void testGetProtectedFields() {
        Map<String, Object> v = assetDao.getProtectedFields("a");
        assertNotNull(v);
        v = assetDao.getProtectedFields(asset1.getId());
        assertNotNull(v);
    }

    @Test
    public void testRemoveFields() {
        assetDao.removeFields(asset1.getId(), Sets.newHashSet("source"), true);
        Asset a = assetDao.get(asset1.getId());
        assertFalse(a.attrExists("source"));
    }

    @Test
    public void testReplace() {
        Source source1 = new Source(getTestImagePath("set04/standard/beer_kettle_01.jpg"));
        source1.setReplace(true);
        AssetIndexResult rsp  = assetDao.index(Lists.newArrayList(source1));
        assertEquals(rsp.replaced, 1);
        assertEquals(rsp.created, 0);

        rsp = assetDao.index(Lists.newArrayList(
                new Source(getTestImagePath("set01/standard/visa12.jpg"))));
        assertEquals(rsp.replaced, 0);
        assertEquals(rsp.created, 1);
    }

    @Test
    public void testRetryBrokenFields() throws InterruptedException { {
        List<Document> assets = ImmutableList.of(
                new Source(getTestImagePath("set01/standard/faces.jpg")));
        assets.get(0).setAttr("foo.bar", "2017/10/10");
        AssetIndexResult result  = assetDao.index(assets);
        refreshIndex();

        List<Document> next = ImmutableList.of(
                new Source(getTestImagePath("set01/standard/hyena.jpg")),
                new Source(getTestImagePath("set01/standard/toucan.jpg")),
                new Source(getTestImagePath("set01/standard/visa.jpg")),
                new Source(getTestImagePath("set01/standard/visa12.jpg")));
        for (Document s: next) {
            s.setAttr("foo.bar", 1000);
        }
        result = assetDao.index(next);

        assertEquals(4, result.created);
        assertEquals(4, result.warnings);
        assertEquals(1, result.retries);
    }}

}
