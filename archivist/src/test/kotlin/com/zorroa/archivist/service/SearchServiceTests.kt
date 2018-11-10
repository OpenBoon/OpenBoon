package com.zorroa.archivist.service

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.google.common.collect.Lists
import com.google.common.collect.Maps
import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.*
import com.zorroa.archivist.repository.FieldDao
import com.zorroa.archivist.schema.LocationSchema
import com.zorroa.archivist.schema.SourceSchema
import com.zorroa.archivist.search.*
import com.zorroa.common.util.Json
import com.zorroa.security.Groups
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired

import java.io.IOException
import java.util.ArrayList

import com.zorroa.archivist.security.getPermissionsFilter
import org.junit.Assert.*

/**
 * Created by chambers on 10/30/15.
 */
class SearchServiceTests : AbstractTest() {

    @Autowired
    internal var fieldDao: FieldDao? = null

    @Before
    fun init() {
        cleanElastic()
        fieldService.invalidateFields()
    }

    @Throws(IOException::class)
    fun testScanAndScrollClamp() {

        addTestAssets("set04")
        refreshIndex()

        var count = 0
        val search = AssetSearch()
        for (doc in searchService.scanAndScroll(search, 2, true)) {
            count++
        }
        assertEquals(2, count.toLong())
    }

    @Test(expected = java.lang.IllegalArgumentException::class)
    @Throws(IOException::class)
    fun testScanAndScrollError() {

        addTestAssets("set04")
        refreshIndex()

        val count = 0
        val search = AssetSearch()
        searchService.scanAndScroll(search, 2, false)
    }

    @Test
    @Throws(IOException::class)
    fun testSearchExportPermissionsMiss() {

        authenticate("user")
        val perm = permissionService.createPermission(PermissionSpec("group", "test"))
        val source = Source(getTestImagePath().resolve("beer_kettle_01.jpg"))
        source.addToPermissions(perm.authority, 1)
        val doc = assetService.createOrReplace(source)

        refreshIndex()

        val search = AssetSearch().setQuery("beer").setAccess(Access.Export)
        assertEquals(0, searchService.search(search).hits.getTotalHits())
    }

    /**
     * A user with zorroa::export can export anything.
     *
     * @throws IOException
     */
    @Test
    @Throws(IOException::class)
    fun testSearchExportPermissionOverrideHit() {
        val user = userService.get("user")
        userService.addPermissions(user, ImmutableList.of(
                permissionService.getPermission("zorroa::export")))
        authenticate("user")
        val source = Source(getTestImagePath().resolve("beer_kettle_01.jpg"))
        assetService.createOrReplace(source)
        refreshIndex()
        val search = AssetSearch().setQuery("source.filename:beer").setAccess(Access.Export)
        assertEquals(1, searchService.search(search).hits.getTotalHits())
        assertNull(getPermissionsFilter(search.access))
    }

    @Test
    @Throws(IOException::class)
    fun testSearchPermissionsExportHit() {
        val perm = permissionService.createPermission(PermissionSpec("group", "test"))
        val user = userService.get("user")
        userService.addPermissions(user, ImmutableList.of(perm))
        authenticate("user")

        val source = Source(getTestImagePath().resolve("beer_kettle_01.jpg"))
        source.addToPermissions(perm.authority, Access.Export.value)
        assetService.createOrReplace(source)
        refreshIndex()

        val search = AssetSearch()
                .setAccess(Access.Export)
                .setQuery("source.filename:beer")
        assertNotNull(getPermissionsFilter(search.access))
        assertEquals(1, searchService.search(search).hits.getTotalHits())

    }

    @Test
    @Throws(IOException::class)
    fun testSearchPermissionsReadMiss() {

        authenticate("user")
        val perm = permissionService.createPermission(PermissionSpec("group", "test"))
        val source = Source(getTestImagePath().resolve("beer_kettle_01.jpg"))
        source.addToPermissions(perm.authority, 1)
        val doc = assetService.createOrReplace(source)

        refreshIndex()

        val search = AssetSearch().setQuery("beer")
        assertEquals(0, searchService.search(search).hits.getTotalHits())
    }

    @Test
    @Throws(IOException::class)
    fun testSearchPermissionsReadHit() {
        authenticate("user")
        val source = Source(getTestImagePath().resolve("beer_kettle_01.jpg"))
        source.setAttr("media.keywords", ImmutableList.of("captain"))
        source.addToPermissions(Groups.EVERYONE, 1)
        assetService.createOrReplace(source)
        refreshIndex()

        val search = AssetSearch().setQuery("captain")
        assertEquals(1, searchService.search(search).hits.getTotalHits())
    }

    @Test
    @Throws(IOException::class)
    fun testFolderSearch() {

        val builder = FolderSpec("Avengers")
        val folder1 = folderService.create(builder)

        val source = Source(getTestImagePath().resolve("beer_kettle_01.jpg"))
        source.addToKeywords("media", source.getAttr("source.filename", String::class.java))
        val asset1 = assetService.createOrReplace(source)
        refreshIndex(100)

        folderService.addAssets(folder1, Lists.newArrayList(asset1.id))
        refreshIndex(100)

        val filter = AssetFilter().addToLinks("folder", folder1.id)
        val search = AssetSearch().setFilter(filter)
        assertEquals(1, searchService.search(search).hits.getTotalHits())
    }

    @Test
    @Throws(IOException::class)
    fun testFolderCount() {

        val builder = FolderSpec("Beer")
        val folder1 = folderService.create(builder)

        val source = Source(getTestImagePath().resolve("beer_kettle_01.jpg"))
        source.addToKeywords("media", source.getAttr("source.filename", String::class.java))
        val asset1 = assetService.createOrReplace(source)
        refreshIndex(100)

        folderService.addAssets(folder1, Lists.newArrayList(asset1.id))
        refreshIndex(100)

        assertEquals(1, searchService.count(folder1))
    }


    @Test
    @Throws(IOException::class)
    fun testRecursiveFolderSearch() {

        var builder = FolderSpec("Avengers")
        val folder1 = folderService.create(builder)

        builder = FolderSpec("Age Of Ultron", folder1)
        val folder2 = folderService.create(builder)

        builder = FolderSpec("Characters", folder2)
        val folder3 = folderService.create(builder)

        val source = Source(getTestImagePath().resolve("beer_kettle_01.jpg"))
        val asset1 = assetService.createOrReplace(source)
        refreshIndex(100)

        folderService.addAssets(folder3, Lists.newArrayList(asset1.id))
        refreshIndex(100)

        val filter = AssetFilter().addToLinks("folder", folder1.id)
        val search = AssetSearch().setFilter(filter)
        assertEquals(1, searchService.search(search).hits.getTotalHits())
    }

    @Test
    @Throws(IOException::class)
    fun testNonRecursiveFolderSearch() {

        var builder = FolderSpec("Avengers")
        val folder1 = folderService.create(builder)

        builder = FolderSpec("Age Of Ultron", folder1)
        builder.recursive = false
        val folder2 = folderService.create(builder)

        builder = FolderSpec("Characters", folder2)
        val folder3 = folderService.create(builder)

        val source1 = Source(getTestImagePath().resolve("beer_kettle_01.jpg"))
        source1.addToKeywords("media", source1.getAttr("source", SourceSchema::class.java).filename)

        val source2 = Source(getTestImagePath().resolve("new_zealand_wellington_harbour.jpg"))
        source2.addToKeywords("media", source2.getAttr("source", SourceSchema::class.java).filename)

        val asset1 = assetService.createOrReplace(source1)
        val asset2 = assetService.createOrReplace(source2)
        refreshIndex()

        folderService.addAssets(folder2, Lists.newArrayList(asset2.id))
        folderService.addAssets(folder3, Lists.newArrayList(asset1.id))
        refreshIndex(100)

        val filter = AssetFilter().addToLinks("folder", folder1.id)
        val search = AssetSearch().setFilter(filter)

        assertEquals(1, searchService.search(search).hits.getTotalHits())
    }

    @Test
    @Throws(IOException::class)
    fun testSmartFolderSearch() {

        var builder = FolderSpec("Avengers")
        val folder1 = folderService.create(builder)

        builder = FolderSpec("Age Of Ultron", folder1)
        val folder2 = folderService.create(builder)

        builder = FolderSpec("Characters", folder2)
        builder.search = AssetSearch("captain america")
        val (id, name, parentId, organizationId, dyhiId, user, timeCreated, timeModified, recursive, dyhiRoot, dyhiField, childCount, acl, search1, taxonomyRoot, attrs) = folderService.create(builder)

        val source = Source(getTestImagePath().resolve("beer_kettle_01.jpg"))
        source.setAttr("media.keywords", ImmutableList.of("captain"))

        val a = assetService.createOrReplace(source)
        refreshIndex()

        val filter = AssetFilter().addToLinks("folder", folder1.id)
        val search = AssetSearch().setFilter(filter)
        assertEquals(1, searchService.search(search).hits.getTotalHits())
    }

    @Test
    @Throws(IOException::class)
    fun testTermSearch() {

        val source = Source(getTestImagePath().resolve("beer_kettle_01.jpg"))
        source.setAttr("media.keywords", ImmutableList.of("captain", "america"))

        assetService.createOrReplace(source)
        refreshIndex()

        var count = 0
        for (a in searchService.search(Pager.first(), AssetSearch().setFilter(
                AssetFilter().addToTerms("media.keywords", "captain")))) {
            count++
        }
        assertTrue(count > 0)
    }

    @Test
    @Throws(IOException::class)
    fun testSmartFolderAndStaticFolderMixture() {

        var builder = FolderSpec("Avengers")
        val folder1 = folderService.create(builder)

        builder = FolderSpec("Age Of Ultron", folder1)
        val folder2 = folderService.create(builder)

        builder = FolderSpec("Characters", folder2)
        builder.search = AssetSearch("captain america")
        val (id, name, parentId, organizationId, dyhiId, user, timeCreated, timeModified, recursive, dyhiRoot, dyhiField, childCount, acl, search1, taxonomyRoot, attrs) = folderService.create(builder)

        val source1 = Source(getTestImagePath().resolve("beer_kettle_01.jpg"))
        source1.setAttr("media.keywords", ImmutableList.of("captain"))

        val source2 = Source(getTestImagePath().resolve("new_zealand_wellington_harbour.jpg"))
        source2.setAttr("media.keywords", source2.getAttr("source", SourceSchema::class.java).filename)

        assetService.createOrReplace(source1)
        assetService.createOrReplace(source2)
        refreshIndex()

        indexService.appendLink("folder", folder2.id.toString(), ImmutableList.of(source2.id))

        val filter = AssetFilter().addToLinks("folder", folder1.id)
        val search = AssetSearch().setFilter(filter)
        assertEquals(2, searchService.search(search).hits.getTotalHits())
    }

    @Test
    @Throws(IOException::class)
    fun testLotsOfSmartFolders() {

        var builder = FolderSpec("people")
        val folder1 = folderService.create(builder)

        for (i in 0..99) {
            builder = FolderSpec("person$i", folder1)
            builder.search = AssetSearch("beer")
            folderService.create(builder)
        }

        refreshIndex()

        val source = Source(getTestImagePath().resolve("beer_kettle_01.jpg"))
        source.setAttr("media.keywords", source.getAttr("source", SourceSchema::class.java).filename)

        assetService.createOrReplace(source)
        refreshIndex()

        val filter = AssetFilter().addToLinks("folder", folder1.id)
        val search = AssetSearch().setFilter(filter)
        assertEquals(1, searchService.search(search).hits.getTotalHits())
    }

    @Test
    @Throws(IOException::class)
    fun testQueryWithCustomFields() {

        val source = Source(getTestImagePath().resolve("beer_kettle_01.jpg"))
        source.setAttr("foo.bar1", "captain kirk")
        source.setAttr("foo.bar2", "bilbo baggins")
        source.setAttr("foo.bar3", "pirate pete")

        assetService.createOrReplace(source)
        refreshIndex()

        assertEquals(1, searchService.search(
                AssetSearch("captain").setQueryFields(ImmutableMap.of("foo.bar1", 1.0f))).hits.getTotalHits())
        assertEquals(0, searchService.search(
                AssetSearch("captain").setQueryFields(ImmutableMap.of("foo.bar2", 1.0f))).hits.getTotalHits())

        assertEquals(1, searchService.search(
                AssetSearch("captain baggins").setQueryFields(
                        ImmutableMap.of("foo.bar1", 1.0f, "foo.bar2", 2.0f))).hits.getTotalHits())


    }

    @Test
    @Throws(IOException::class)
    fun testHighConfidenceSearch() {

        val source = Source(getTestImagePath().resolve("beer_kettle_01.jpg"))
        source.setAttr("media.keywords", ImmutableList.of("zipzoom"))
        assetService.createOrReplace(source)
        refreshIndex()

        /*
         * High confidence words are found at every level.
         */
        assertEquals(1, searchService.search(
                AssetSearch("zipzoom")).hits.getTotalHits())
        assertEquals(1, searchService.search(
                AssetSearch("zipzoom")).hits.getTotalHits())
        assertEquals(1, searchService.search(
                AssetSearch("zipzoom")).hits.getTotalHits())
    }

    @Test
    @Throws(IOException::class)
    fun testNoConfidenceSearch() {

        val source = Source(getTestImagePath().resolve("beer_kettle_01.jpg"))
        source.setAttr("media.keywords", ImmutableList.of("zipzoom"))
        assetService.createOrReplace(source)
        refreshIndex()

        assertEquals(1, searchService.search(
                AssetSearch("zipzoom")).hits.getTotalHits())
    }

    @Test
    @Throws(IOException::class)
    fun testSearchResponseFields() {

        val source = Source(getTestImagePath().resolve("beer_kettle_01.jpg"))
        source.setAttr("media.keywords", ImmutableList.of("zooland"))
        source.addToLinks("folder", "abc123")
        source.addToLinks("folder", "abc456")
        assetService.createOrReplace(source)
        refreshIndex()

        var response = searchService.search(AssetSearch("zoolander"))
        assertEquals(1, response.hits.getTotalHits())
        var doc = response.hits.getAt(0).sourceAsMap
        var _doc = Document(doc)

        var folders: List<String>? = _doc.getAttr("system.links.folder")
        assertEquals(2, folders!!.size.toLong())

        response = searchService.search(AssetSearch("zoolander").setFields(arrayOf("keywords*")))
        assertEquals(1, response.hits.getTotalHits())
        doc = response.hits.getAt(0).sourceAsMap
        assertNull(doc["system.links"])

        response = searchService.search(AssetSearch("zoolander").setFields(arrayOf("system.links.folder")))
        assertEquals(1, response.hits.getTotalHits())
        doc = response.hits.getAt(0).sourceAsMap
        _doc = Document(doc)
        folders = _doc.getAttr("system.links.folder")
        folders = folders
        assertEquals(2, folders!!.size.toLong())

        response = searchService.search(AssetSearch("zoolander").setFields(arrayOf("system.links*")))
        assertEquals(1, response.hits.getTotalHits())
        doc = response.hits.getAt(0).sourceAsMap
        _doc = Document(doc)
        folders = _doc.getAttr("system.links.folder")
        folders = folders
        assertEquals(2, folders!!.size.toLong())
    }

    @Test
    @Throws(IOException::class)
    fun testQueryWildcard() {

        val source = Source(getTestImagePath().resolve("beer_kettle_01.jpg"))
        source.setAttr("media.keywords", ImmutableList.of("source", "zoolander"))
        assetService.createOrReplace(source)
        refreshIndex()

        assertEquals(1, searchService.search(
                AssetSearch("zoo*")).hits.getTotalHits())
    }

    @Test
    @Throws(IOException::class)
    fun testQueryExactWithQuotes() {

        val source = Source(getTestImagePath().resolve("beer_kettle_01.jpg"))
        source.setAttr("test.keywords", "ironMan17313.jpg")
        assetService.createOrReplace(source)

        val source2 = Source(getTestImagePath().resolve("new_zealand_wellington_harbour.jpg"))
        source2.setAttr("test.keywords", "ironMan17314.jpg")
        assetService.createOrReplace(source2)

        refreshIndex()

        assertEquals(1, searchService.search(
                AssetSearch("\"ironMan17313.jpg\"")).hits.getTotalHits())
    }

    @Test
    @Throws(IOException::class)
    fun testQueryMultipleExactWithAnd() {

        val source = Source(getTestImagePath().resolve("beer_kettle_01.jpg"))
        source.setAttr("test.keywords", Lists.newArrayList("RA", "pencil", "O'Connor"))
        assetService.createOrReplace(source)

        val source2 = Source(getTestImagePath().resolve("new_zealand_wellington_harbour.jpg"))
        source2.setAttr("test.keywords", Lists.newArrayList("RA", "Cock O'the Walk"))
        assetService.createOrReplace(source2)

        refreshIndex()

        assertEquals(1, searchService.search(
                AssetSearch("\"Cock O'the Walk\" AND \"RA\"")).hits.getTotalHits())
    }

    @Test
    @Throws(IOException::class)
    fun testQueryExactTermWithSpaces() {

        val source = Source(getTestImagePath().resolve("beer_kettle_01.jpg"))
        source.setAttr("test.keywords", Lists.newArrayList("RA", "pencil", "O'Connor"))
        assetService.createOrReplace(source)

        val source2 = Source(getTestImagePath().resolve("new_zealand_wellington_harbour.jpg"))
        source2.setAttr("test.keywords", Lists.newArrayList("RA", "Cock O'the Walk"))
        assetService.createOrReplace(source2)

        refreshIndex()

        assertEquals(1, searchService.search(
                AssetSearch("\"Cock O'the Walk\"")).hits.getTotalHits())

    }

    @Test
    @Throws(IOException::class)
    fun testQueryMinusTerm() {

        val source = Source(getTestImagePath().resolve("beer_kettle_01.jpg"))
        source.addToKeywords("media", "zoolander", "beer")
        assetService.createOrReplace(source)
        refreshIndex()

        assertEquals(0, searchService.search(
                AssetSearch("zoo* -beer")).hits.getTotalHits())
    }

    @Test
    @Throws(IOException::class)
    fun testQueryPlusTerm() {

        val source = Source(getTestImagePath().resolve("beer_kettle_01.jpg"))
        source.setAttr("media.keywords", ImmutableList.of("zoolander", "beer"))
        assetService.createOrReplace(source)
        refreshIndex()

        assertEquals(1, searchService.search(
                AssetSearch("zoolander OR cat")).hits.getTotalHits())
    }

    @Test
    @Throws(IOException::class)
    fun testQueryExactTerm() {

        val source = Source(getTestImagePath().resolve("beer_kettle_01.jpg"))
        source.setAttr("media.keywords", ImmutableList.of("zooland"))
        assetService.createOrReplace(source)
        refreshIndex()

        assertEquals(0, searchService.search(
                AssetSearch("zoolandar")).hits.getTotalHits())
        assertEquals(1, searchService.search(
                AssetSearch("zoolander")).hits.getTotalHits())
    }

    @Test
    @Throws(IOException::class)
    fun testQueryFuzzyTerm() {
        val source = Source(getTestImagePath().resolve("beer_kettle_01.jpg"))
        source.setAttr("media.keywords", ImmutableList.of("zooland"))
        assetService.createOrReplace(source)
        logger.info("{}", Json.prettyString(source))
        refreshIndex()

        assertEquals(1, searchService.search(
                AssetSearch("zoolind~")).hits.getTotalHits())
    }

    @Test
    fun testBoostFields() {
        try {
            settingsService.set("archivist.search.keywords.boost", "foo:1,bar:2")
            fieldService.invalidateFields()
            val fields = fieldService.getFields("asset")
            assertTrue(fields["keywords-boost"]!!.contains("foo:1"))
            assertTrue(fields["keywords-boost"]!!.contains("bar:2"))
        } finally {
            settingsService.set("archivist.search.keywords.boost", null)
        }
    }

    @Test
    fun getFields() {

        val source = Source(getTestImagePath().resolve("beer_kettle_01.jpg"))
        source.setAttr("location", LocationSchema(doubleArrayOf(1.0, 2.0)).setCountry("USA"))
        source.setAttr("foo.keywords", ImmutableList.of("joe", "dog"))
        source.setAttr("foo.shash", "AAFFGG")
        source.setAttr("media.clip.parent", "abc123")

        assetService.createOrReplace(source)
        refreshIndex()

        val fields = fieldService.getFields("asset")
        assertTrue(fields["date"]!!.size > 0)
        assertTrue(fields["string"]!!.size > 0)
        assertTrue(fields["point"]!!.size > 0)
        assertTrue(fields["similarity"]!!.size > 0)
        assertTrue(fields["id"]!!.size > 0)
        assertTrue(fields["keywords"]!!.contains("foo.keywords"))
    }

    @Test
    fun getFieldsWithHidden() {

        val source = Source(getTestImagePath().resolve("beer_kettle_01.jpg"))
        source.setAttr("location", LocationSchema(doubleArrayOf(1.0, 2.0)).setCountry("USA"))
        source.setAttr("foo.keywords", ImmutableList.of("joe", "dog"))
        source.setAttr("foo.shash", "AAFFGG")

        assetService.createOrReplace(source)
        refreshIndex()
        fieldService.updateField(HideField("foo.keywords", true))

        val fields = fieldService.getFields("asset")
        assertFalse(fields["keywords-boost"]!!.contains("foo.keywords"))
        assertFalse(fields["string"]!!.contains("foo.keywords"))
    }

    @Test
    fun getFieldsWithHiddenNameSpace() {

        val source = Source(getTestImagePath().resolve("beer_kettle_01.jpg"))
        source.setAttr("location", LocationSchema(doubleArrayOf(1.0, 2.0)).setCountry("USA"))
        source.setAttr("foo.keywords", ImmutableList.of("joe", "dog"))
        source.setAttr("foo.shash", "AAFFGG")

        assetService.createOrReplace(source)
        refreshIndex()
        fieldService.updateField(HideField("foo.", true))

        val fields = fieldService.getFields("asset")
        assertFalse(fields["keywords-boost"]!!.contains("foo.keywords"))
        assertFalse(fields["string"]!!.contains("foo.keywords"))
    }

    @Test
    @Throws(IOException::class)
    fun testScrollSearch() {
        assetService.createOrReplace(Source(getTestImagePath().resolve("beer_kettle_01.jpg")))
        assetService.createOrReplace(Source(getTestImagePath().resolve("new_zealand_wellington_harbour.jpg")))
        refreshIndex()

        val result1 = searchService.search(Pager.first(1),
                AssetSearch().setScroll(Scroll().setTimeout("1m")))
        assertNotNull(result1.scroll)
        assertEquals(1, result1.size().toLong())

        val result2 = searchService.search(Pager.first(1), AssetSearch().setScroll(result1.scroll
                .setTimeout("1m")))
        assertNotNull(result2.scroll)
        assertEquals(1, result2.size().toLong())
    }

    @Test
    @Throws(IOException::class)
    fun testAggregationSearch() {
        assetService.createOrReplace(Source(getTestImagePath().resolve("beer_kettle_01.jpg")))
        assetService.createOrReplace(Source(getTestImagePath().resolve("new_zealand_wellington_harbour.jpg")))
        refreshIndex()

        val page = searchService.search(Pager.first(1),
                AssetSearch().addToAggs("foo",
                        ImmutableMap.of<String, Any>("max",
                                ImmutableMap.of("field", "source.fileSize"))))
        assertEquals(1, page.aggregations.size.toLong())
    }

    @Test
    @Throws(IOException::class)
    fun testAggregationSearchEmptyFilter() {
        assetService.createOrReplace(Source(getTestImagePath().resolve("beer_kettle_01.jpg")))
        assetService.createOrReplace(Source(getTestImagePath().resolve("new_zealand_wellington_harbour.jpg")))
        refreshIndex()

        val req = Maps.newHashMap<String, Any>()
        req["filter"] = ImmutableMap.of<Any, Any>()
        req["aggs"] = ImmutableMap.of("foo", ImmutableMap.of("terms",
                ImmutableMap.of("field", "source.fileSize")))

        val page = searchService.search(Pager.first(1),
                AssetSearch().addToAggs("facet", req))
        assertEquals(1, page.aggregations.size.toLong())
    }

    @Test
    @Throws(IOException::class)
    fun testMustSearch() {
        val source1 = Source(getTestImagePath().resolve("beer_kettle_01.jpg"))
        source1.setAttr("superhero", "captain")

        val source2 = Source(getTestImagePath().resolve("new_zealand_wellington_harbour.jpg"))
        source2.setAttr("superhero", "loki")

        assetService.createOrReplace(source1)
        assetService.createOrReplace(source2)
        refreshIndex()

        val filter = AssetFilter().setMust(ImmutableList.of(AssetFilter().addToTerms("superhero", "captain")))
        val search = AssetSearch().setFilter(filter)
        assertEquals(1, searchService.search(search).hits.getTotalHits())
    }

    @Test
    @Throws(IOException::class)
    fun testMustNotSearch() {
        val source1 = Source(getTestImagePath().resolve("beer_kettle_01.jpg"))
        source1.setAttr("superhero", "captain")

        val source2 = Source(getTestImagePath().resolve("new_zealand_wellington_harbour.jpg"))
        source2.setAttr("superhero", "loki")

        assetService.createOrReplace(source1)
        assetService.createOrReplace(source2)
        refreshIndex()

        val filter = AssetFilter().setMustNot(ImmutableList.of(AssetFilter().addToTerms("superhero", "captain")))
        val search = AssetSearch().setFilter(filter)
        assertEquals(1, searchService.search(search).hits.getTotalHits())
    }

    @Test
    @Throws(IOException::class)
    fun testShouldSearch() {
        val source1 = Source(getTestImagePath().resolve("beer_kettle_01.jpg"))
        source1.setAttr("superhero", "captain")

        val source2 = Source(getTestImagePath().resolve("new_zealand_wellington_harbour.jpg"))
        source2.setAttr("superhero", "loki")

        assetService.createOrReplace(source1)
        assetService.createOrReplace(source2)
        refreshIndex()

        val filter = AssetFilter().setShould(ImmutableList.of(AssetFilter().addToTerms("superhero", "captain")))
        val search = AssetSearch().setFilter(filter)
        assertEquals(1, searchService.search(search).hits.getTotalHits())
    }

    @Test
    @Throws(IOException::class, InterruptedException::class)
    fun testKwConfSearch() {
        val source1 = Source(getTestImagePath().resolve("beer_kettle_01.jpg"))
        source1.setAttr("kw_with_conf", listOf(
                mapOf("keyword" to "dog", "confidence" to 0.5),
                mapOf("keyword" to "cat", "confidence" to 0.1),
                mapOf("keyword" to "bilboAngry", "confidence" to 0.7)))

        assetService.createOrReplace(source1)
        refreshIndex()

        val filter = AssetFilter()
                .addToKwConf("kw_with_conf", KwConfFilter(listOf("bilboAngry"), listOf(0.25, 0.70)))

        assertEquals(1, searchService.search(AssetSearch(filter)).hits.getTotalHits())
    }

    @Test
    @Throws(IOException::class, InterruptedException::class)
    fun testHammingDistanceFilterWithEmptyValue() {
        val source1 = Source(getTestImagePath().resolve("beer_kettle_01.jpg"))
        source1.setAttr("superhero", "captain")
        source1.setAttr("test.hash1.jimbo", "")

        val source2 = Source(getTestImagePath().resolve("new_zealand_wellington_harbour.jpg"))
        source2.setAttr("superhero", "loki")

        assetService.batchCreateOrReplace(BatchCreateAssetsRequest(listOf(source2, source1)))
        refreshIndex()

        val search: AssetSearch

        search = AssetSearch(
                AssetFilter().addToSimilarity("test.hash1.jimbo",
                        SimilarityFilter("AFAFAFAF", 100)))
        assertEquals(0, searchService.search(search).hits.getTotalHits())
    }

    @Test
    @Throws(IOException::class, InterruptedException::class)
    fun testHammingDistanceFilterWithRaw() {
        val source1 = Source(getTestImagePath().resolve("beer_kettle_01.jpg"))
        source1.setAttr("superhero", "captain")
        source1.setAttr("test.hash1.jimbo", "AFAFAFAF")

        val source2 = Source(getTestImagePath().resolve("new_zealand_wellington_harbour.jpg"))
        source2.setAttr("superhero", "loki")
        source2.setAttr("test.hash1.jimbo", "ADADADAD")

        assetService.batchCreateOrReplace(BatchCreateAssetsRequest(listOf(source2, source1)))
        refreshIndex()

        val search: AssetSearch

        search = AssetSearch(
                AssetFilter().addToSimilarity("test.hash1.jimbo.raw",
                        SimilarityFilter("AFAFAFAF", 100)))
        assertEquals(1, searchService.search(search).hits.getTotalHits())
    }

    @Test
    @Throws(IOException::class, InterruptedException::class)
    fun testHammingDistanceFilter() {
        val source1 = Source(getTestImagePath().resolve("beer_kettle_01.jpg"))
        source1.setAttr("superhero", "captain")
        source1.setAttr("test.hash1.shash", "AFAFAFAF")

        val source2 = Source(getTestImagePath().resolve("new_zealand_wellington_harbour.jpg"))
        source2.setAttr("superhero", "loki")
        source2.setAttr("test.hash1.shash", "ADADADAD")

        assetService.batchCreateOrReplace(BatchCreateAssetsRequest(listOf(source2, source1)))
        refreshIndex()

        var search: AssetSearch

        search = AssetSearch(
                AssetFilter().addToSimilarity("test.hash1.shash",
                        SimilarityFilter("AFAFAFAF", 100)))
        assertEquals(1, searchService.search(search).hits.getTotalHits())

        search = AssetSearch(
                AssetFilter().addToSimilarity("test.hash1.shash",
                        SimilarityFilter("AFAFAFAF", 50)))
        assertEquals(2, searchService.search(search).hits.getTotalHits())

        search = AssetSearch(
                AssetFilter().addToSimilarity("test.hash1.shash",
                        SimilarityFilter("APAPAPAP", 20)))

        assertEquals(2, searchService.search(search).hits.getTotalHits())

    }

    /**
     * The array handling code needs to be updated in es-similarity.
     *
     * @throws IOException
     * @throws InterruptedException
     */
    @Ignore
    @Test
    @Throws(IOException::class, InterruptedException::class)
    fun testHammingDistanceFilterArray() {
        val source1 = Source(getTestImagePath().resolve("beer_kettle_01.jpg"))
        source1.setAttr("superhero", "captain")
        source1.setAttr("test.hash1.shash", ImmutableList.of("AFAFAFAF", "AFAFAFA1"))

        val source2 = Source(getTestImagePath().resolve("new_zealand_wellington_harbour.jpg"))
        source2.setAttr("superhero", "loki")
        source2.setAttr("test.hash1.shash", ImmutableList.of("ADADADAD"))

        assetService.batchCreateOrReplace(BatchCreateAssetsRequest(listOf(source2, source1)))
        refreshIndex()

        val search: AssetSearch

        search = AssetSearch(
                AssetFilter().addToSimilarity("test.hash1.shash",
                        SimilarityFilter("AFAFAFAF", 100)))
        val hits = searchService.search(search).hits
        assertEquals(1, hits.getTotalHits())
        val doc = Document(hits.getAt(0).sourceAsMap)
        assertEquals(ImmutableList.of("AFAFAFAF", "AFAFAFA1"), doc.getAttr("test.hash1.shash"))

    }

    @Test
    @Throws(IOException::class)
    fun testHammingDistanceFilterWithQuery() {
        val source1 = Source(getTestImagePath().resolve("beer_kettle_01.jpg"))
        source1.setAttr("media.keywords", Lists.newArrayList("beer_kettle_01.jpg"))
        source1.setAttr("superhero", "captain")
        source1.setAttr("test.hash1.shash", "afafafaf")

        val source2 = Source(getTestImagePath().resolve("new_zealand_wellington_harbour.jpg"))
        source2.setAttr("superhero", "loki")
        source2.setAttr("test.hash1.shash", "adadadad")

        assetService.batchCreateOrReplace(BatchCreateAssetsRequest(listOf(source2, source1)))
        refreshIndex()

        val search = AssetSearch("beer")
        search.filter = AssetFilter().addToSimilarity("test.hash1.shash",
                SimilarityFilter("afafafaf", 1))

        /**
         * The score from the hamming distance is combined with the query
         * score, to result in a score higher than the hamming score.
         */
        val score = searchService.search(search).hits.getAt(0).score
        assertTrue(score >= .5)
    }

    @Test
    @Throws(IOException::class)
    fun testHammingDistanceFilterWithAssetId() {
        val source1 = Source(getTestImagePath().resolve("beer_kettle_01.jpg"))
        source1.setAttr("media.keywords", Lists.newArrayList("beer"))
        source1.setAttr("superhero", "captain")
        source1.setAttr("test.hash1.shash", "afafafaf")

        val source2 = Source(getTestImagePath().resolve("new_zealand_wellington_harbour.jpg"))
        source2.setAttr("superhero", "loki")
        source2.setAttr("test.hash1.shash", "adadadad")

        assetService.batchCreateOrReplace(BatchCreateAssetsRequest(listOf(source2, source1)))
        refreshIndex()

        val search = AssetSearch("beer")
        search.filter = AssetFilter().addToSimilarity("test.hash1.shash",
                SimilarityFilter(source1.id, 8))

        /**
         * The score from the hamming distance is combined with the query
         * score, to result in a score higher than the hamming score.
         */
        val score = searchService.search(search).hits.getAt(0).score
        assertTrue(score >= .5)
    }

    @Test
    fun testSuggest() {
        val source = Source(getTestImagePath().resolve("beer_kettle_01.jpg"))
        source.setAttr("media.keywords", ImmutableList.of("zoolander"))

        assetService.createOrReplace(source)
        refreshIndex()
        assertEquals(ImmutableList.of("zoolander"), searchService.getSuggestTerms("zoo"))
    }

    @Test
    fun testEmptySearch() {
        addTestAssets("set01")
        refreshIndex()
        assertEquals(5, searchService.search(Pager.first(), AssetSearch()).size().toLong())
    }

    @Test
    fun testFromSize() {
        addTestAssets("set01")
        refreshIndex()
        assertEquals(2,
                searchService.search(Pager(2, 2), AssetSearch()).size().toLong())
    }

    @Test
    fun testFilterExists() {
        val source = Source(getTestImagePath().resolve("beer_kettle_01.jpg"))
        assetService.createOrReplace(source)
        refreshIndex()

        var asb = AssetSearch(AssetFilter().addToExists("source.path"))
        var result: PagedList<*> = searchService.search(Pager.first(), asb)
        assertEquals(1, result.size().toLong())

        asb = AssetSearch(AssetFilter().addToExists("source.dsdsdsds"))
        result = searchService.search(Pager.first(), asb)
        assertEquals(0, result.size().toLong())

    }

    @Test
    fun testFilterMissing() {
        val source = Source(getTestImagePath().resolve("beer_kettle_01.jpg"))
        assetService.createOrReplace(source)

        assertEquals(1, searchService.count(AssetSearch()))

        var asb = AssetSearch(AssetFilter().addToMissing("source.path"))
        var result: PagedList<*> = searchService.search(Pager.first(), asb)
        assertEquals(0, result.size().toLong())

        asb = AssetSearch(AssetFilter().addToMissing("source.dsdsdsds"))
        result = searchService.search(Pager.first(), asb)
        assertEquals(1, result.size().toLong())
    }

    @Test
    fun testFilterRange() {
        addTestAssets("set01")
        val asb = AssetSearch(AssetFilter()
                .addRange("source.fileSize", RangeQuery().setGt(100000)))
        val result = searchService.search(Pager.first(), asb)
        assertEquals(2, result.size().toLong())
    }

    @Test
    fun testFilterScript() {
        addTestAssets("set01")

        val text = "doc['source.fileSize'].value == params.size"
        val script = AssetScript(text,
                ImmutableMap.of<String, Any>("size", 113333))

        val scripts = ArrayList<AssetScript>()
        scripts.add(script)
        val asb = AssetSearch(AssetFilter().setScripts(scripts))
        val result = searchService.search(Pager.first(), asb)
        assertEquals(1, result.size().toLong())
    }

}
