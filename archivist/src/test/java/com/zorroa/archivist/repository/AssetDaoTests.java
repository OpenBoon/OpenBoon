package com.zorroa.archivist.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.zorroa.archivist.AbstractTest;
import com.zorroa.archivist.elastic.SearchBuilder;
import com.zorroa.sdk.domain.*;
import com.zorroa.sdk.processor.Source;
import com.zorroa.sdk.util.Json;
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

import static org.junit.Assert.*;

public class AssetDaoTests extends AbstractTest {

    @Autowired
    AssetDao assetDao;

    Document asset1;

    @Before
    public void init() {
        Source builder = new Source(getTestImagePath("set04/standard/beer_kettle_01.jpg"));
        builder.setAttr("foo.str", "ass");
        builder.setAttr("foo.int", 155);
        asset1 = assetDao.index(builder);
        refreshIndex();
    }

    @Test
    public void testGetFieldValue() throws IOException {
        assertEquals("ass", assetDao.getFieldValue(asset1.getId(), "foo.str"));
        assertEquals(155, (int) assetDao.getFieldValue(asset1.getId(), "foo.int"));
    }

    @Test
    public void testGetById() {
        Document asset2 = assetDao.get(asset1.getId());
        assertEquals(asset1.getId(), asset2.getId());
    }

    @Test
    public void testGetByPath() {
        Path p = getTestImagePath("set04/standard/beer_kettle_01.jpg");
        Document asset2 = assetDao.get(p);
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
        PagedList<Document> assets = assetDao.getAll(Pager.first(10));
        assertEquals(1, assets.getList().size());
    }

    @Test
    public void testGetAllBySearchRequest() {
        SearchBuilder sb = new SearchBuilder();
        sb.getSource().query(QueryBuilders.matchAllQuery());
        PagedList<Document> assets = assetDao.getAll(Pager.first(10), sb);
        assertEquals(1, assets.getList().size());
    }

    @Test
    public void testGetAllBySearchRequestIntoStream() throws IOException {
        assetDao.index(new Source(getTestImagePath("set01/standard/faces.jpg")));
        refreshIndex();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        SearchBuilder sb = new SearchBuilder();
        sb.getSource().query(QueryBuilders.matchAllQuery());
        sb.getSource().aggregation(AggregationBuilders.terms("path").field("source.path"));

        assetDao.getAll(Pager.first(10), sb, stream);
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

        SearchBuilder sb = new SearchBuilder();
        sb.getSource().query(QueryBuilders.matchAllQuery());
        sb.getRequest().scroll("1m");

        PagedList<Document> assets = assetDao.getAll(Pager.first(1), sb);
        assertEquals(1, assets.getList().size());
        assertEquals(6, (long) assets.getPage().getTotalCount());
        assertNotNull(assets.getScroll());
        Document asset = assets.get(0);

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
        assertTrue(assetDao.appendLink("folder", "100",
                ImmutableList.of(asset1.getId())).get("success").contains(asset1.getId()));
        assertTrue(assetDao.appendLink("parent", "foo",
                ImmutableList.of(asset1.getId())).get("success").contains(asset1.getId()));

        Document a = assetDao.get(asset1.getId());
        Collection<Object> folder_links = a.getAttr("zorroa.links.folder");
        Collection<Object> parent_links = a.getAttr("zorroa.links.parent");
        assertEquals(1, folder_links.size());
        assertEquals(1, parent_links.size());
        assertTrue(folder_links.contains("100"));
        assertTrue(parent_links.contains("foo"));
    }

    @Test
    public void testRemoveLink() {
        assertTrue(assetDao.appendLink("folder", "100",
                ImmutableList.of(asset1.getId())).get("success").contains(asset1.getId()));

        Document a = assetDao.get(asset1.getId());
        Collection<Object> links = a.getAttr("zorroa.links.folder");
        assertEquals(1, links.size());

        assertTrue(assetDao.removeLink("folder", "100",
                ImmutableList.of(asset1.getId())).get("success").contains(asset1.getId()));

        a = assetDao.get(asset1.getId());
        links = a.getAttr("zorroa.links.folder");
        assertEquals(0, links.size());
    }

    @Test
    public void testUpdate() {
        Map<String, Object> attrs = Maps.newHashMap();
        attrs.put("foo.bar", 100);

        assetDao.update(asset1.getId(), attrs);
        Document asset2 = assetDao.get(asset1.getId());
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

        Document a = assetDao.get(source1.getId());
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
        Map<String, Object> v = assetDao.getManagedFields("a");
        assertNotNull(v);
        v = assetDao.getManagedFields(asset1.getId());
        assertNotNull(v);
    }

    @Test
    public void testRemoveFields() {
        assetDao.removeFields(asset1.getId(), Sets.newHashSet("source"), true);
        Document a = assetDao.get(asset1.getId());
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
