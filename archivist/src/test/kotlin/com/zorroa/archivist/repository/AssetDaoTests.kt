package com.zorroa.archivist.repository

import com.fasterxml.jackson.core.type.TypeReference
import com.google.common.collect.ImmutableList
import com.google.common.collect.Lists
import com.google.common.collect.Maps
import com.google.common.collect.Sets
import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.elastic.SearchBuilder
import com.zorroa.sdk.domain.Asset
import com.zorroa.sdk.domain.Document
import com.zorroa.sdk.domain.PagedList
import com.zorroa.sdk.domain.Pager
import com.zorroa.sdk.processor.Source
import com.zorroa.sdk.util.Json
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.aggregations.AggregationBuilders
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import java.io.ByteArrayOutputStream
import java.io.IOException

class AssetDaoTests : AbstractTest() {

    @Autowired
    internal lateinit var assetDao: AssetDao
    internal lateinit var asset1: Document

    @Before
    fun init() {
        val builder = Source(getTestImagePath("set04/standard/beer_kettle_01.jpg"))
        builder.setAttr("bar.str", "dog")
        builder.setAttr("bar.int", 100)
        asset1 = assetDao.index(builder)
        logger.info("Creating asset: {}", asset1.id)
        refreshIndex()
    }

    @Test
    @Throws(IOException::class)
    fun testGetFieldValue() {
        assertEquals("dog", assetDao.getFieldValue(asset1.id, "bar.str"))
        assertEquals(100, (assetDao.getFieldValue<Any>(asset1.id, "bar.int") as Int).toLong())
    }

    @Test
    fun testGetById() {
        val asset2 = assetDao[asset1.id]
        assertEquals(asset1.id, asset2.id)
    }

    @Test
    fun testGetByPath() {
        val p = getTestImagePath("set04/standard/beer_kettle_01.jpg")
        val asset2 = assetDao[p]
        assertNotNull(asset2)
    }

    @Test
    fun testExistsByPath() {
        val p = getTestImagePath("set04/standard/beer_kettle_01.jpg")
        assertTrue(assetDao.exists(p))
    }

    @Test
    fun testExistsById() {
        assertTrue(assetDao.exists(asset1.id))
        assertFalse(assetDao.exists("abc"))
    }

    @Test
    fun testGetAll() {
        val assets = assetDao.getAll(Pager.first(10))
        assertEquals(1, assets.list.size.toLong())
    }

    @Test
    fun testGetAllBySearchRequest() {
        val sb = SearchBuilder()
        sb.source.query(QueryBuilders.matchAllQuery())
        val assets = assetDao.getAll(Pager.first(10), sb)
        assertEquals(1, assets.list.size.toLong())
    }

    @Test
    @Throws(IOException::class)
    fun testGetAllBySearchRequestIntoStream() {
        assetDao.index(Source(getTestImagePath("set01/standard/faces.jpg")))
        refreshIndex()
        val stream = ByteArrayOutputStream()

        val sb = SearchBuilder()
        sb.source.query(QueryBuilders.matchAllQuery())
        sb.source.aggregation(AggregationBuilders.terms("path").field("source.path.raw"))

        assetDao.getAll(Pager.first(10), sb, stream)
        val result = Json.deserialize(stream.toString(), object : TypeReference<PagedList<Asset>>() {})
        assertEquals(2, result.list.size.toLong())
        assertEquals(1, result.aggregations.entries.size)
    }

    @Test
    fun testGetAllScroll() {
        assetDao.index(Source(getTestImagePath("set01/standard/faces.jpg")))
        assetDao.index(Source(getTestImagePath("set01/standard/hyena.jpg")))
        assetDao.index(Source(getTestImagePath("set01/standard/toucan.jpg")))
        assetDao.index(Source(getTestImagePath("set01/standard/visa.jpg")))
        assetDao.index(Source(getTestImagePath("set01/standard/visa12.jpg")))
        refreshIndex()

        val sb = SearchBuilder()
        sb.source.query(QueryBuilders.matchAllQuery())
        sb.request.scroll("1m")

        var assets = assetDao.getAll(Pager.first(1), sb)
        assertEquals(1, assets.list.size.toLong())
        assertEquals(6, assets.page.totalCount as Long)
        assertNotNull(assets.scroll)
        val asset = assets.get(0)

        assets = assetDao.getAll(assets.scroll.id, "1m")
        assertEquals(1, assets.list.size.toLong())
        assertNotNull(assets.scroll)
        assertNotEquals(asset.id, assets.get(0).id)
    }

    @Test
    fun testBatchUpsert() {
        val source1 = Source(getTestImagePath("set04/standard/beer_kettle_01.jpg"))
        val source2 = Source(getTestImagePath("set04/standard/new_zealand_wellington_harbour.jpg"))

        var result = assetDao.index(ImmutableList.of(source1, source2))
        assertEquals(1, result.created.toLong())
        assertEquals(1, result.updated.toLong())

        result = assetDao.index(ImmutableList.of(source1, source2))
        assertEquals(0, result.created.toLong())
        assertEquals(2, result.updated.toLong())
    }

    @Test
    fun testAppendLink() {
        assertTrue(assetDao.appendLink("folder", "100",
                ImmutableList.of(asset1.id))["success"]!!.contains(asset1.id))
        assertTrue(assetDao.appendLink("parent", "foo",
                ImmutableList.of(asset1.id))["success"]!!.contains(asset1.id))

        val a = assetDao[asset1.id]
        val folder_links = a.getAttr<Collection<Any>>("zorroa.links.folder")
        val parent_links = a.getAttr<Collection<Any>>("zorroa.links.parent")

        assertEquals(1, folder_links.size.toLong())
        assertEquals(1, parent_links.size.toLong())
        assertTrue(folder_links.contains("100"))
        assertTrue(parent_links.contains("foo"))
    }

    @Test
    fun testRemoveLink() {
        assertTrue(assetDao.appendLink("folder", "100",
                ImmutableList.of(asset1.id))["success"]!!.contains(asset1.id))

        var a = assetDao[asset1.id]
        var links = a.getAttr<Collection<Any>>("zorroa.links.folder")
        assertEquals(1, links.size.toLong())

        assertTrue(assetDao.removeLink("folder", "100",
                ImmutableList.of(asset1.id))["success"]!!.contains(asset1.id))

        a = assetDao[asset1.id]
        links = a.getAttr("zorroa.links.folder")
        assertEquals(0, links.size.toLong())
    }

    @Test
    fun testUpdate() {
        val attrs = Maps.newHashMap<String, Any>()
        attrs["foo.bar"] = 100

        assetDao.update(asset1.id, attrs)
        val asset2 = assetDao[asset1.id]
        assertEquals(100, (asset2.getAttr<Any>("foo.bar") as Int).toLong())
    }

    @Test
    fun testBatchIndexWithReplace() {

        val source1 = Source(getTestImagePath("set04/standard/beer_kettle_01.jpg"))
        assetDao.index(Lists.newArrayList(source1))

        source1.removeAttr("keywords")
        source1.isReplace = true
        assetDao.index(Lists.newArrayList(source1))
        refreshIndex()

        val a = assetDao[source1.id]
        assertFalse(a.attrExists("keywords"))
    }

    @Test
    fun testDelete() {
        assertTrue(assetDao.delete(asset1.id))
        refreshIndex()
        assertFalse(assetDao.delete(asset1.id))
    }

    @Test
    fun testGetProtectedFields() {
        var v = assetDao.getManagedFields("a")
        assertNotNull(v)
        v = assetDao.getManagedFields(asset1.id)
        assertNotNull(v)
    }

    @Test
    fun testRemoveFields() {
        assetDao.removeFields(asset1.id, Sets.newHashSet("source"), true)
        val a = assetDao[asset1.id]
        assertFalse(a.attrExists("source"))
    }

    @Test
    fun testReplace() {
        val source1 = Source(getTestImagePath("set04/standard/beer_kettle_01.jpg"))
        source1.isReplace = true
        var rsp = assetDao.index(Lists.newArrayList(source1))
        assertEquals(rsp.replaced.toLong(), 1)
        assertEquals(rsp.created.toLong(), 0)

        rsp = assetDao.index(Lists.newArrayList(
                Source(getTestImagePath("set01/standard/visa12.jpg"))))
        assertEquals(rsp.replaced.toLong(), 0)
        assertEquals(rsp.created.toLong(), 1)
    }

    @Test
    @Throws(InterruptedException::class)
    fun testRetryBrokenFields() {
        run {
            val assets = ImmutableList.of<Document>(
                    Source(getTestImagePath("set01/standard/faces.jpg")))
            assets[0].setAttr("foo.bar", 1000)


            var result = assetDao.index(assets)

            logger.info("{}", result)
            refreshIndex()

            val next = ImmutableList.of<Document>(
                    Source(getTestImagePath("set01/standard/hyena.jpg")),
                    Source(getTestImagePath("set01/standard/toucan.jpg")),
                    Source(getTestImagePath("set01/standard/visa.jpg")),
                    Source(getTestImagePath("set01/standard/visa12.jpg")))
            for (s in next) {
                s.setAttr("foo.bar", "bob")
            }
            result = assetDao.index(next)
            logger.info("{}", result)

            assertEquals(4, result.created.toLong())
            assertEquals(4, result.warnings.toLong())
            assertEquals(1, result.retries.toLong())
        }
    }

}
