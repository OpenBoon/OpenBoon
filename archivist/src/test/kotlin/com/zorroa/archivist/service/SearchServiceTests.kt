package com.zorroa.archivist.service

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.google.common.collect.Maps
import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.Access
import com.zorroa.archivist.domain.BatchCreateAssetsRequest
import com.zorroa.archivist.domain.BatchUpdateAssetLinks
import com.zorroa.archivist.domain.Document
import com.zorroa.archivist.domain.FolderSpec
import com.zorroa.archivist.domain.LinkType
import com.zorroa.archivist.domain.OrganizationSpec
import com.zorroa.archivist.domain.PagedList
import com.zorroa.archivist.domain.Pager
import com.zorroa.archivist.domain.PermissionSpec
import com.zorroa.archivist.domain.Source
import com.zorroa.archivist.schema.LocationSchema
import com.zorroa.archivist.schema.SourceSchema
import com.zorroa.archivist.sdk.security.UserAuthed
import com.zorroa.archivist.search.AssetFilter
import com.zorroa.archivist.search.AssetScript
import com.zorroa.archivist.search.AssetSearch
import com.zorroa.archivist.search.GeoBoundingBox
import com.zorroa.archivist.search.KwConfFilter
import com.zorroa.archivist.search.RangeQuery
import com.zorroa.archivist.search.Scroll
import com.zorroa.archivist.search.SimilarityFilter
import com.zorroa.archivist.security.AccessResolver
import com.zorroa.archivist.security.SuperAdminAuthentication
import com.zorroa.archivist.security.UnitTestAuthentication
import com.zorroa.archivist.security.getUser
import com.zorroa.archivist.security.withAuth
import com.zorroa.security.Groups
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.TestPropertySource
import java.io.IOException
import java.util.UUID

/**
 * Created by chambers on 10/30/15.
 */
class SearchServiceTests : AbstractTest() {

    @Autowired
    lateinit var accessResolver: AccessResolver

    override fun requiresElasticSearch(): Boolean {
        return true
    }

    override fun requiresFieldSets(): Boolean {
        return true
    }

    @Before
    fun init() {
        fieldService.invalidateFields()
    }

    fun testScanAndScrollClamp() {

        addTestAssets("set04")

        var count = 0
        val search = AssetSearch()
        for (doc in searchService.scanAndScroll(search, 2, true)) {
            count++
        }
        assertEquals(2, count.toLong())
    }

    @Test(expected = java.lang.IllegalArgumentException::class)
    fun testScanAndScrollError() {

        addTestAssets("set04")

        val count = 0
        val search = AssetSearch()
        searchService.scanAndScroll(search, 2, false)
    }

    @Test
    fun testClearScroll() {
       addTestAssets("set04")
        val result1 = searchService.search(
            Pager.first(1),
            AssetSearch().setScroll(Scroll().setTimeout("1m"))
        )
        assertTrue(searchService.clearScroll(result1.scroll.id))
        assertFalse(searchService.clearScroll(result1.scroll.id))
    }

    @Test
    fun testSearchExportPermissionsMiss() {
        authenticate("user")
        val perm = permissionService.createPermission(PermissionSpec("group", "test"))
        val source = Source(getTestImagePath("beer_kettle_01.jpg"))
        source.addToPermissions(perm.authority, 1)
        source.addToPermissions(Groups.EVERYONE, 0)
        assetService.createOrReplaceAssets(BatchCreateAssetsRequest(source))

        val search = AssetSearch().setQuery("beer").setAccess(Access.Export)
        assertEquals(0, searchService.search(search).hits.getTotalHits())
    }

    /**
     * A user with zorroa::export can export anything.
     *
     * @throws IOException
     */
    @Test
    fun testSearchExportPermissionOverrideHit() {
        val user = userService.get("user")
        userService.addPermissions(
            user, ImmutableList.of(
                permissionService.getPermission(Groups.EXPORT)
            )
        )
        authenticate("user")
        val source = Source(getTestImagePath("beer_kettle_01.jpg"))
        assetService.createOrReplaceAssets(BatchCreateAssetsRequest(source))

        val search = AssetSearch().setQuery("source.filename:beer").setAccess(Access.Export)
        assertEquals(1, searchService.search(search).hits.getTotalHits())
        assertNull(accessResolver.getAssetPermissionsFilter(search.access))
    }

    @Test
    fun testSearchPermissionsExportHit() {
        val perm = permissionService.createPermission(PermissionSpec("group", "test"))
        val user = userService.get("user")
        userService.addPermissions(user, ImmutableList.of(perm))
        authenticate("user")

        val source = Source(getTestImagePath("beer_kettle_01.jpg"))
        source.addToPermissions(perm.authority, Access.Export.value)
        assetService.createOrReplaceAssets(BatchCreateAssetsRequest(source))

        val search = AssetSearch()
            .setAccess(Access.Export)
            .setQuery("source.filename:beer")
        assertNotNull(accessResolver.getAssetPermissionsFilter(search.access))
        assertEquals(1, searchService.search(search).hits.getTotalHits())
    }

    @Test
    fun testSearchPermissionsReadMiss() {

        authenticate("user")
        val perm = permissionService.createPermission(PermissionSpec("group", "test"))
        val source = Source(getTestImagePath("beer_kettle_01.jpg"))
        source.addToPermissions(perm.authority, 1)
        source.addToPermissions(Groups.EVERYONE, 0)
        assetService.createOrReplaceAssets(BatchCreateAssetsRequest(source))

        val search = AssetSearch().setQuery("beer")
        assertEquals(0, searchService.search(search).hits.getTotalHits())
    }

    @Test
    fun testSearchPermissionsReadHit() {
        authenticate("user")
        val source = Source(getTestImagePath("beer_kettle_01.jpg"))
        source.setAttr("media.keywords", ImmutableList.of("captain"))
        source.addToPermissions(Groups.EVERYONE, 1)
        assetService.createOrReplaceAssets(BatchCreateAssetsRequest(source))

        val search = AssetSearch().setQuery("captain")
        assertEquals(1, searchService.search(search).hits.getTotalHits())
    }

    @Test
    fun testFolderSearch() {

        val builder = FolderSpec("Avengers")
        val folder1 = folderService.create(builder)

        val source = Source(getTestImagePath("beer_kettle_01.jpg"))
        source.addToKeywords("media", source.getAttr("source.filename", String::class.java))
        assetService.createOrReplaceAssets(BatchCreateAssetsRequest(source))
        folderService.addAssets(folder1, listOf(source.id))

        val filter = AssetFilter().addToLinks("folder", folder1.id)
        val search = AssetSearch().setFilter(filter)
        assertEquals(1, searchService.search(search).hits.getTotalHits())
    }

    @Test
    fun testFolderCount() {

        val builder = FolderSpec("Beer")
        val folder1 = folderService.create(builder)

        val source = Source(getTestImagePath("beer_kettle_01.jpg"))
        source.addToKeywords("media", source.getAttr("source.filename", String::class.java))
        assetService.createOrReplaceAssets(BatchCreateAssetsRequest(source))
        folderService.addAssets(folder1, listOf(source.id))

        assertEquals(1, searchService.count(folder1))
    }

    @Test
    fun testRecursiveFolderSearch() {

        var builder = FolderSpec("Avengers")
        val folder1 = folderService.create(builder)

        builder = FolderSpec("Age Of Ultron", folder1)
        val folder2 = folderService.create(builder)

        builder = FolderSpec("Characters", folder2)
        val folder3 = folderService.create(builder)

        val source = Source(getTestImagePath("beer_kettle_01.jpg"))
        assetService.createOrReplaceAssets(BatchCreateAssetsRequest(source))
        folderService.addAssets(folder3, listOf(source.id))

        val filter = AssetFilter().addToLinks("folder", folder1.id)
        val search = AssetSearch().setFilter(filter)
        assertEquals(1, searchService.search(search).hits.getTotalHits())
    }

    @Test
    fun testNonRecursiveFolderSearch() {

        var builder = FolderSpec("Avengers")
        val folder1 = folderService.create(builder)

        builder = FolderSpec("Age Of Ultron", folder1)
        builder.recursive = false
        val folder2 = folderService.create(builder)

        builder = FolderSpec("Characters", folder2)
        val folder3 = folderService.create(builder)

        val source1 = Source(getTestImagePath("beer_kettle_01.jpg"))
        source1.addToKeywords("media", source1.getAttr("source", SourceSchema::class.java)!!.filename)

        val source2 = Source(getTestImagePath("new_zealand_wellington_harbour.jpg"))
        source2.addToKeywords("media", source2.getAttr("source", SourceSchema::class.java)!!.filename)

        assetService.createOrReplaceAssets(BatchCreateAssetsRequest(source1))
        assetService.createOrReplaceAssets(BatchCreateAssetsRequest(source2))

        folderService.addAssets(folder2, listOf(source1.id))
        folderService.addAssets(folder3, listOf(source2.id))
        refreshIndex()

        val filter = AssetFilter().addToLinks("folder", folder1.id)
        val search = AssetSearch().setFilter(filter)

        assertEquals(1, searchService.search(search).hits.getTotalHits())
    }

    @Test
    fun testSmartFolderSearch() {

        var builder = FolderSpec("Avengers")
        val folder1 = folderService.create(builder)

        builder = FolderSpec("Age Of Ultron", folder1)
        val folder2 = folderService.create(builder)

        builder = FolderSpec("Characters", folder2)
        builder.search = AssetSearch("captain america")
        folderService.create(builder)

        val source = Source(getTestImagePath("beer_kettle_01.jpg"))
        source.setAttr("media.keywords", ImmutableList.of("captain"))

        val a = assetService.createOrReplaceAssets(BatchCreateAssetsRequest(source))

        val filter = AssetFilter().addToLinks("folder", folder1.id)
        val search = AssetSearch().setFilter(filter)
        assertEquals(1, searchService.search(search).hits.getTotalHits())
    }

    @Test
    fun testTermSearch() {

        val source = Source(getTestImagePath("beer_kettle_01.jpg"))
        source.setAttr("media.keywords", ImmutableList.of("captain", "america"))

        assetService.createOrReplaceAssets(BatchCreateAssetsRequest(source))

        var count = 0
        for (a in searchService.search(
            Pager.first(), AssetSearch().setFilter(
                AssetFilter().addToTerms("media.keywords", "captain")
            )
        )) {
            count++
        }
        assertTrue(count > 0)
    }

    @Test
    fun testSmartFolderAndStaticFolderMixture() {

        var builder = FolderSpec("Avengers")
        val folder1 = folderService.create(builder)

        builder = FolderSpec("Age Of Ultron", folder1)
        val folder2 = folderService.create(builder)

        builder = FolderSpec("Characters", folder2)
        builder.search = AssetSearch("captain america")
        val (id, name, parentId, organizationId, dyhiId, user, timeCreated, timeModified, recursive, dyhiRoot, dyhiField, childCount, acl, search1, taxonomyRoot, attrs) = folderService.create(
            builder
        )

        val source1 = Source(getTestImagePath("beer_kettle_01.jpg"))
        source1.setAttr("media.keywords", ImmutableList.of("captain"))

        val source2 = Source(getTestImagePath("new_zealand_wellington_harbour.jpg"))
        source2.setAttr("media.keywords", source2.getAttr("source", SourceSchema::class.java)!!.filename)

        assetService.createOrReplaceAssets(BatchCreateAssetsRequest(listOf(source1, source2)))
        assetService.batchUpdateLinks(
            LinkType.Folder, listOf(folder2.id), BatchUpdateAssetLinks(
                ImmutableList.of(source2.id)
            )
        )

        val filter = AssetFilter().addToLinks("folder", folder1.id)
        val search = AssetSearch().setFilter(filter)
        assertEquals(2, searchService.search(search).hits.getTotalHits())
    }

    @Test
    fun testLotsOfSmartFolders() {

        var builder = FolderSpec("people")
        val folder1 = folderService.create(builder)

        for (i in 0..99) {
            builder = FolderSpec("person$i", folder1)
            builder.search = AssetSearch("beer")
            folderService.create(builder)
        }

        refreshIndex()

        val source = Source(getTestImagePath("beer_kettle_01.jpg"))
        source.setAttr("media.keywords", source.getAttr("source", SourceSchema::class.java)!!.filename)

        assetService.createOrReplaceAssets(BatchCreateAssetsRequest(source))

        val filter = AssetFilter().addToLinks("folder", folder1.id)
        val search = AssetSearch().setFilter(filter)
        assertEquals(1, searchService.search(search).hits.getTotalHits())
    }

    @Test
    fun testQueryWithCustomFields() {

        val source = Source(getTestImagePath("beer_kettle_01.jpg"))
        source.setAttr("foo.bar1", "captain kirk")
        source.setAttr("foo.bar2", "bilbo baggins")
        source.setAttr("foo.bar3", "pirate pete")

        assetService.createOrReplaceAssets(BatchCreateAssetsRequest(source))

        assertEquals(
            1, searchService.search(
                AssetSearch("captain").setQueryFields(ImmutableMap.of("foo.bar1", 1.0f))
            ).hits.getTotalHits()
        )
        assertEquals(
            0, searchService.search(
                AssetSearch("captain").setQueryFields(ImmutableMap.of("foo.bar2", 1.0f))
            ).hits.getTotalHits()
        )

        assertEquals(
            1, searchService.search(
                AssetSearch("captain baggins").setQueryFields(
                    ImmutableMap.of("foo.bar1", 1.0f, "foo.bar2", 2.0f)
                )
            ).hits.getTotalHits()
        )
    }

    @Test
    fun testQueryWithGeoBBoxFields() {
        addTestAssets("set04")

        val bbox = GeoBoundingBox("37.031377, -109.083887", "36.968862, -109.000840")
        val filter = AssetFilter()
        filter.geo_bounding_box = mapOf("location.point" to bbox)
        val search = AssetSearch().setFilter(filter)
        assertEquals(6, searchService.search(search).hits.getTotalHits())
    }

    @Test
    fun testHighConfidenceSearch() {

        val source = Source(getTestImagePath("beer_kettle_01.jpg"))
        source.setAttr("media.keywords", ImmutableList.of("zipzoom"))
        assetService.createOrReplaceAssets(BatchCreateAssetsRequest(source))

        /*
         * High confidence words are found at every level.
         */
        assertEquals(
            1, searchService.search(
                AssetSearch("zipzoom")
            ).hits.getTotalHits()
        )
        assertEquals(
            1, searchService.search(
                AssetSearch("zipzoom")
            ).hits.getTotalHits()
        )
        assertEquals(
            1, searchService.search(
                AssetSearch("zipzoom")
            ).hits.getTotalHits()
        )
    }

    @Test
    fun testNoConfidenceSearch() {

        val source = Source(getTestImagePath("beer_kettle_01.jpg"))
        source.setAttr("media.keywords", ImmutableList.of("zipzoom"))
        assetService.createOrReplaceAssets(BatchCreateAssetsRequest(source))

        assertEquals(
            1, searchService.search(
                AssetSearch("zipzoom")
            ).hits.getTotalHits()
        )
    }

    @Test
    fun testSearchResponseFields() {

        val source = Source(getTestImagePath("beer_kettle_01.jpg"))
        source.setAttr("media.keywords", ImmutableList.of("zooland"))
        source.addToLinks("folder", UUID.randomUUID())
        source.addToLinks("folder", UUID.randomUUID())
        assetService.createOrReplaceAssets(BatchCreateAssetsRequest(source))

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
    fun testQueryWildcard() {

        val source = Source(getTestImagePath("beer_kettle_01.jpg"))
        source.setAttr("media.keywords", ImmutableList.of("source", "zoolander"))
        assetService.createOrReplaceAssets(BatchCreateAssetsRequest(source))

        assertEquals(
            1, searchService.search(
                AssetSearch("zoo*")
            ).hits.getTotalHits()
        )
    }

    @Test
    fun testQueryExactWithQuotes() {

        val source = Source(getTestImagePath("beer_kettle_01.jpg"))
        source.setAttr("media.keywords", "ironMan17313.jpg")
        assetService.createOrReplaceAssets(BatchCreateAssetsRequest(source))

        val source2 = Source(getTestImagePath("new_zealand_wellington_harbour.jpg"))
        source2.setAttr("media.keywords", "ironMan17314.jpg")
        assetService.createOrReplaceAssets(BatchCreateAssetsRequest(source2))

        assertEquals(
            1, searchService.search(
                AssetSearch("\"ironMan17313.jpg\"")
            ).hits.getTotalHits()
        )
    }

    @Test
    fun testQueryMultipleExactWithAnd() {

        val source = Source(getTestImagePath("beer_kettle_01.jpg"))
        source.setAttr("media.keywords", listOf("RA", "pencil", "O'Connor"))
        assetService.createOrReplaceAssets(BatchCreateAssetsRequest(source))

        val source2 = Source(getTestImagePath("new_zealand_wellington_harbour.jpg"))
        source2.setAttr("media.keywords", listOf("RA", "Cock O'the Walk"))
        assetService.createOrReplaceAssets(BatchCreateAssetsRequest(source2))

        assertEquals(
            1, searchService.search(
                AssetSearch("\"Cock O'the Walk\" AND \"RA\"")
            ).hits.getTotalHits()
        )
    }

    @Test
    fun testQueryExactTermWithSpaces() {

        val source = Source(getTestImagePath("beer_kettle_01.jpg"))
        source.setAttr("media.title", listOf("RA", "pencil", "O'Connor"))
        assetService.createOrReplaceAssets(BatchCreateAssetsRequest(source))

        val source2 = Source(getTestImagePath("new_zealand_wellington_harbour.jpg"))
        source2.setAttr("media.title", listOf("RA", "Cock O'the Walk"))
        assetService.createOrReplaceAssets(BatchCreateAssetsRequest(source2))

        assertEquals(
            1, searchService.search(
                AssetSearch("\"Cock O'the Walk\"")
            ).hits.getTotalHits()
        )
    }

    @Test
    fun testQueryMinusTerm() {

        val source = Source(getTestImagePath("beer_kettle_01.jpg"))
        source.addToKeywords("media", "zoolander", "beer")
        assetService.createOrReplaceAssets(BatchCreateAssetsRequest(source))

        assertEquals(
            0, searchService.search(
                AssetSearch("zoo* -beer")
            ).hits.getTotalHits()
        )
    }

    @Test
    fun testQueryPlusTerm() {

        val source = Source(getTestImagePath("beer_kettle_01.jpg"))
        source.setAttr("media.title", "Zoolander")
        assetService.createOrReplaceAssets(BatchCreateAssetsRequest(source))

        assertEquals(
            1, searchService.search(
                AssetSearch("zoolander OR cat")
            ).hits.getTotalHits()
        )
    }

    @Test
    fun testQueryExact() {

        val source = Source(getTestImagePath("beer_kettle_01.jpg"))
        source.setAttr("media.keywords", listOf("Dog In the Street"))
        assetService.createOrReplaceAssets(BatchCreateAssetsRequest(source))

        assertEquals(
            0, searchService.search(
                AssetSearch("Dog In the").setExactQuery(true)
            ).hits.getTotalHits()
        )

        assertEquals(
            0, searchService.search(
                AssetSearch("dog in the street").setExactQuery(true)
            ).hits.getTotalHits()
        )

        assertEquals(
            1, searchService.search(
                AssetSearch("Dog In the Street").setExactQuery(true)
            ).hits.getTotalHits()
        )
    }

    @Test
    fun testQueryFuzzyTerm() {
        val source = Source(getTestImagePath("beer_kettle_01.jpg"))
        source.setAttr("media.keywords", ImmutableList.of("zooland"))
        assetService.createOrReplaceAssets(BatchCreateAssetsRequest(source))

        assertEquals(
            1, searchService.search(
                AssetSearch("zoolind~")
            ).hits.getTotalHits()
        )
    }

    @Test
    fun getFields() {

        val source = Source(getTestImagePath("beer_kettle_01.jpg"))
        source.setAttr("location", LocationSchema(doubleArrayOf(1.0, 2.0)).setCountry("USA"))
        source.setAttr("foo.keywords", ImmutableList.of("joe", "dog"))
        source.setAttr("foo.shash", "AAFFGG")
        source.setAttr("media.clip.parent", "abc123")

        assetService.createOrReplaceAssets(BatchCreateAssetsRequest(source))

        val fields = fieldService.getFields("asset")
        assertTrue(fields["date"]!!.size > 0)
        assertTrue(fields["string"]!!.size > 0)
        assertTrue(fields["point"]!!.size > 0)
        assertTrue(fields["similarity"]!!.size > 0)
        assertTrue(fields["id"]!!.size > 0)
        assertTrue(fields["keywords"]!!.contains("foo.keywords"))
    }

    @Test
    fun testScrollSearch() {
        val source1 = Source(getTestImagePath("beer_kettle_01.jpg"))
        val source2 = Source(getTestImagePath("new_zealand_wellington_harbour.jpg"))
        assetService.createOrReplaceAssets(BatchCreateAssetsRequest(listOf(source1, source2)))

        val result1 = searchService.search(
            Pager.first(1),
            AssetSearch().setScroll(Scroll().setTimeout("1m"))
        )
        assertNotNull(result1.scroll)
        assertEquals(1, result1.size().toLong())

        val result2 = searchService.search(
            Pager.first(1), AssetSearch().setScroll(
                result1.scroll
                    .setTimeout("1m")
            )
        )
        assertNotNull(result2.scroll)
        assertEquals(1, result2.size().toLong())
    }

    @Test
    fun testAggregationSearch() {
        val source1 = Source(getTestImagePath("beer_kettle_01.jpg"))
        val source2 = Source(getTestImagePath("new_zealand_wellington_harbour.jpg"))
        assetService.createOrReplaceAssets(BatchCreateAssetsRequest(listOf(source1, source2)))

        val page = searchService.search(
            Pager.first(1),
            AssetSearch().addToAggs(
                "foo",
                ImmutableMap.of<String, Any>(
                    "max",
                    ImmutableMap.of("field", "source.fileSize")
                )
            )
        )
        assertEquals(1, page.aggregations.size.toLong())
    }

    @Test
    fun testAggregationSearchEmptyFilter() {
        val source1 = Source(getTestImagePath("beer_kettle_01.jpg"))
        val source2 = Source(getTestImagePath("new_zealand_wellington_harbour.jpg"))
        assetService.createOrReplaceAssets(BatchCreateAssetsRequest(listOf(source1, source2)))

        val req = Maps.newHashMap<String, Any>()
        req["filter"] = ImmutableMap.of<Any, Any>()
        req["aggs"] = ImmutableMap.of(
            "foo", ImmutableMap.of(
                "terms",
                ImmutableMap.of("field", "source.fileSize")
            )
        )

        val page = searchService.search(
            Pager.first(1),
            AssetSearch().addToAggs("facet", req)
        )
        assertEquals(1, page.aggregations.size.toLong())
    }

    @Test
    fun testMustSearch() {
        val source1 = Source(getTestImagePath("beer_kettle_01.jpg"))
        source1.setAttr("superhero", "captain")

        val source2 = Source(getTestImagePath("new_zealand_wellington_harbour.jpg"))
        source2.setAttr("superhero", "loki")

        assetService.createOrReplaceAssets(BatchCreateAssetsRequest(listOf(source1, source2)))

        val filter = AssetFilter().setMust(ImmutableList.of(AssetFilter().addToTerms("superhero", "captain")))
        val search = AssetSearch().setFilter(filter)
        assertEquals(1, searchService.search(search).hits.getTotalHits())
    }

    @Test
    fun testMustNotSearch() {
        val source1 = Source(getTestImagePath("beer_kettle_01.jpg"))
        source1.setAttr("superhero", "captain")

        val source2 = Source(getTestImagePath("new_zealand_wellington_harbour.jpg"))
        source2.setAttr("superhero", "loki")

        assetService.createOrReplaceAssets(BatchCreateAssetsRequest(listOf(source1, source2)))

        val filter = AssetFilter().setMustNot(ImmutableList.of(AssetFilter().addToTerms("superhero", "captain")))
        val search = AssetSearch().setFilter(filter)
        assertEquals(1, searchService.search(search).hits.getTotalHits())
    }

    @Test
    fun testShouldSearch() {
        val source1 = Source(getTestImagePath("beer_kettle_01.jpg"))
        source1.setAttr("superhero", "captain")

        val source2 = Source(getTestImagePath("new_zealand_wellington_harbour.jpg"))
        source2.setAttr("superhero", "loki")

        assetService.createOrReplaceAssets(BatchCreateAssetsRequest(listOf(source1, source2)))

        val filter = AssetFilter().setShould(ImmutableList.of(AssetFilter().addToTerms("superhero", "captain")))
        val search = AssetSearch().setFilter(filter)
        assertEquals(1, searchService.search(search).hits.getTotalHits())
    }

    @Test

    fun testKwConfSearch() {
        val source1 = Source(getTestImagePath("beer_kettle_01.jpg"))
        source1.setAttr(
            "kw_with_conf", listOf(
                mapOf("keyword" to "dog", "confidence" to 0.5),
                mapOf("keyword" to "cat", "confidence" to 0.1),
                mapOf("keyword" to "bilboAngry", "confidence" to 0.7)
            )
        )

        assetService.createOrReplaceAssets(BatchCreateAssetsRequest(source1))

        val filter = AssetFilter()
            .addToKwConf("kw_with_conf", KwConfFilter(listOf("bilboAngry"), listOf(0.25, 0.70)))

        assertEquals(1, searchService.search(AssetSearch(filter)).hits.getTotalHits())
    }

    @Test

    fun testHammingDistanceFilterWithEmptyValue() {
        val source1 = Source(getTestImagePath("beer_kettle_01.jpg"))
        source1.setAttr("superhero", "captain")
        source1.setAttr("test.jimbo.shash", "")

        val source2 = Source(getTestImagePath("new_zealand_wellington_harbour.jpg"))
        source2.setAttr("superhero", "loki")

        assetService.createOrReplaceAssets(BatchCreateAssetsRequest(listOf(source2, source1)))

        val search = AssetSearch(
            AssetFilter().addToSimilarity(
                "test.jimbo.shash",
                SimilarityFilter("AFAFAFAF", 100)
            )
        )
        assertEquals(0, searchService.search(search).hits.getTotalHits())
    }

    @Test
    fun testHammingDistanceFilterWithRaw() {
        val source1 = Source(getTestImagePath("beer_kettle_01.jpg"))
        source1.setAttr("superhero", "captain")
        source1.setAttr("test.hash1.jimbo", "AFAFAFAF")

        val source2 = Source(getTestImagePath("new_zealand_wellington_harbour.jpg"))
        source2.setAttr("superhero", "loki")
        source2.setAttr("test.hash1.jimbo", "ADADADAD")

        assetService.createOrReplaceAssets(BatchCreateAssetsRequest(listOf(source2, source1)))

        val search = AssetSearch(
            AssetFilter().addToSimilarity(
                "test.hash1.jimbo.raw",
                SimilarityFilter("AFAFAFAF", 100)
            )
        )
        assertEquals(1, searchService.search(search).hits.getTotalHits())
    }

    @Test
    fun testHammingDistanceFilter() {
        val source1 = Source(getTestImagePath("beer_kettle_01.jpg"))
        source1.setAttr("superhero", "captain")
        source1.setAttr("test.hash1.shash", "AFAFAFAF")

        val source2 = Source(getTestImagePath("new_zealand_wellington_harbour.jpg"))
        source2.setAttr("superhero", "loki")
        source2.setAttr("test.hash1.shash", "ADADADAD")

        assetService.createOrReplaceAssets(BatchCreateAssetsRequest(listOf(source2, source1)))

        var search = AssetSearch(
            AssetFilter().addToSimilarity(
                "test.hash1.shash",
                SimilarityFilter("AFAFAFAF", 100)
            )
        )
        assertEquals(1, searchService.search(search).hits.getTotalHits())

        search = AssetSearch(
            AssetFilter().addToSimilarity(
                "test.hash1.shash",
                SimilarityFilter("AFAFAFAF", 50)
            )
        )
        assertEquals(2, searchService.search(search).hits.getTotalHits())

        search = AssetSearch(
            AssetFilter().addToSimilarity(
                "test.hash1.shash",
                SimilarityFilter("APAPAPAP", 20)
            )
        )

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
    fun testHammingDistanceFilterArray() {
        val source1 = Source(getTestImagePath("beer_kettle_01.jpg"))
        source1.setAttr("superhero", "captain")
        source1.setAttr("test.hash1.shash", ImmutableList.of("AFAFAFAF", "AFAFAFA1"))

        val source2 = Source(getTestImagePath("new_zealand_wellington_harbour.jpg"))
        source2.setAttr("superhero", "loki")
        source2.setAttr("test.hash1.shash", ImmutableList.of("ADADADAD"))

        assetService.createOrReplaceAssets(BatchCreateAssetsRequest(listOf(source2, source1)))

        val search = AssetSearch(
            AssetFilter().addToSimilarity(
                "test.hash1.shash",
                SimilarityFilter("AFAFAFAF", 100)
            )
        )
        val hits = searchService.search(search).hits
        assertEquals(1, hits.getTotalHits())
        val doc = Document(hits.getAt(0).sourceAsMap)
        assertEquals(ImmutableList.of("AFAFAFAF", "AFAFAFA1"), doc.getAttr("test.hash1.shash"))
    }

    @Test
    fun testHammingDistanceFilterWithQuery() {
        val source1 = Source(getTestImagePath("beer_kettle_01.jpg"))
        source1.setAttr("media.keywords", listOf("beer_kettle_01.jpg"))
        source1.setAttr("superhero", "captain")
        source1.setAttr("test.hash1.shash", "afafafaf")

        val source2 = Source(getTestImagePath("new_zealand_wellington_harbour.jpg"))
        source2.setAttr("superhero", "loki")
        source2.setAttr("test.hash1.shash", "adadadad")

        assetService.createOrReplaceAssets(BatchCreateAssetsRequest(listOf(source2, source1)))

        val search = AssetSearch("beer")
        search.filter = AssetFilter().addToSimilarity(
            "test.hash1.shash",
            SimilarityFilter("afafafaf", 1)
        )

        /**
         * The score from the hamming distance is combined with the query
         * score, to result in a score higher than the hamming score.
         */
        val score = searchService.search(search).hits.getAt(0).score
        assertTrue(score >= .5)
    }

    @Test
    fun testHammingDistanceFilterWithAssetId() {
        val source1 = Source(getTestImagePath("beer_kettle_01.jpg"))
        source1.setAttr("media.keywords", listOf("beer"))
        source1.setAttr("superhero", "captain")
        source1.setAttr("test.hash1.shash", "afafafaf")

        val source2 = Source(getTestImagePath("new_zealand_wellington_harbour.jpg"))
        source2.setAttr("superhero", "loki")
        source2.setAttr("test.hash1.shash", "adadadad")

        assetService.createOrReplaceAssets(BatchCreateAssetsRequest(listOf(source2, source1)))

        val search = AssetSearch("beer")
        search.filter = AssetFilter().addToSimilarity(
            "test.hash1.shash",
            SimilarityFilter(source1.id, 8)
        )

        /**
         * The score from the hamming distance is combined with the query
         * score, to result in a score higher than the hamming score.
         */
        val score = searchService.search(search).hits.getAt(0).score
        assertTrue(score >= .5)
    }

    @Test
    fun testSuggest() {
        val source = Source(getTestImagePath("beer_kettle_01.jpg"))
        source.setAttr("media.keywords", listOf("zoolander"))

        assetService.createOrReplaceAssets(BatchCreateAssetsRequest(source))
        assertEquals(ImmutableList.of("zoolander"), searchService.getSuggestTerms("zoo"))
    }

    @Test
    fun testSuggestLeadingNumbersWithSpaces() {
        val source = Source(getTestImagePath("beer_kettle_01.jpg"))
        source.setAttr("media.keywords", listOf("8990 1234 AbC"))

        assetService.createOrReplaceAssets(BatchCreateAssetsRequest(source))
        assertEquals(listOf("8990 1234 AbC"), searchService.getSuggestTerms("89"))
    }

    @Test
    fun testSuggestWithLeadingPunctuation() {
        val source = Source(getTestImagePath("beer_kettle_01.jpg"))
        source.setAttr("media.keywords", listOf("-8990-1234@abc"))

        assetService.createOrReplaceAssets(BatchCreateAssetsRequest(source))
        assertEquals(listOf("-8990-1234@abc"), searchService.getSuggestTerms("-89"))
    }

    @Test
    fun testOrgFilter() {
        addTestAssets("set01")
        val org = organizationService.create(OrganizationSpec("Test"))
        withAuth(SuperAdminAuthentication(org.id)) {
            refreshIndex()
            assertEquals(0, searchService.search(Pager.first(), AssetSearch()).size().toLong())
        }
    }

    @Test
    fun testEmptySearch() {
        addTestAssets("set01")
        assertEquals(5, searchService.search(Pager.first(), AssetSearch()).size().toLong())
    }

    @Test
    fun testFromSize() {
        addTestAssets("set01")
        assertEquals(
            2,
            searchService.search(Pager(2, 2), AssetSearch()).size().toLong()
        )
    }

    @Test
    fun testFilterExists() {
        val source = Source(getTestImagePath("beer_kettle_01.jpg"))
        assetService.createOrReplaceAssets(BatchCreateAssetsRequest(source))

        var asb = AssetSearch(AssetFilter().addToExists("source.path"))
        var result: PagedList<*> = searchService.search(Pager.first(), asb)
        assertEquals(1, result.size().toLong())

        asb = AssetSearch(AssetFilter().addToExists("source.dsdsdsds"))
        result = searchService.search(Pager.first(), asb)
        assertEquals(0, result.size().toLong())
    }

    @Test
    fun testFilterMissing() {
        val source = Source(getTestImagePath("beer_kettle_01.jpg"))
        assetService.createOrReplaceAssets(BatchCreateAssetsRequest(source))

        assertEquals(1, searchService.count(AssetSearch()))

        var asb = AssetSearch(AssetFilter().addToMissing("source.path"))
        var result: PagedList<*> = searchService.search(Pager.first(), asb)
        assertEquals(0, result.size().toLong())

        asb = AssetSearch(AssetFilter().addToMissing("source.dsdsdsds"))
        result = searchService.search(Pager.first(), asb)
        assertEquals(1, result.size().toLong())
    }

    @Ignore
    @Test
    fun testFilterRange() {
        addTestAssets("set01")

        val source1 = Source(getTestImagePath("toucan.jpg"))
        source1.setAttr("source.fileSize", 100001)

        val source2 = Source(getTestImagePath("faces.jpg"))
        source2.setAttr("source.fileSize", 200000)

        assetService.createOrReplaceAssets(BatchCreateAssetsRequest(listOf(source1, source2)))

        val asb = AssetSearch(
            AssetFilter()
                .addRange("source.fileSize", RangeQuery().setGt(100000))
        )

        val result = searchService.search(Pager.first(), asb)
        assertEquals(2, result.size().toLong())
    }

    @Test
    fun testFilterScript() {
        addTestAssets("set01")

        val source1 = Source(getTestImagePath("faces.jpg"))
        source1.setAttr("source.fileSize", 113333)

        assetService.createOrReplaceAssets(BatchCreateAssetsRequest(listOf(source1)))

        val text = "doc['source.fileSize'].value == params.size"
        val script = AssetScript(
            text,
            ImmutableMap.of<String, Any>("size", 113333)
        )

        val scripts = ArrayList<AssetScript>()
        scripts.add(script)
        val asb = AssetSearch(AssetFilter().setScripts(scripts))
        val result = searchService.search(Pager.first(), asb)
        assertEquals(1, result.size().toLong())
    }

    @Test
    fun testCollapse() {
        val source1 = Source(getTestImagePath("beer_kettle_01.jpg"))
        val source2 = Source(getTestImagePath("new_zealand_wellington_harbour.jpg"))
        source1.setAttr("media.clip.parent", "ABC")
        source2.setAttr("media.clip.parent", "ABC")
        assetService.createOrReplaceAssets(BatchCreateAssetsRequest(listOf(source1, source2)))

        val search = AssetSearch()
        search.collapse = mapOf("field" to "media.clip.parent")

        val result = searchService.search(Pager.first(), search)
        assertEquals(1, result.list.size)
    }

    @Test
    fun testPostFilter() {
        val source1 = Source(getTestImagePath("beer_kettle_01.jpg"))
        source1.setAttr("post_filter_check", "cat")
        val source2 = Source(getTestImagePath("new_zealand_wellington_harbour.jpg"))
        assetService.createOrReplaceAssets(BatchCreateAssetsRequest(listOf(source1, source2)))

        val search = AssetSearch()
        search.postFilter = AssetFilter()
        search.postFilter.addToTerms("post_filter_check", "cat")

        val result = searchService.search(Pager.first(), search)
        assertEquals(1, result.list.size)
    }

    @Test
    fun testPostFilterWithQuery() {
        val source1 = Source(getTestImagePath("beer_kettle_01.jpg"))
        val source2 = Source(getTestImagePath("new_zealand_wellington_harbour.jpg"))
        source1.setAttr("post_filter_check", "cat")
        assetService.createOrReplaceAssets(BatchCreateAssetsRequest(listOf(source1, source2)))

        val search = AssetSearch()
        search.query = "beer"
        search.postFilter = AssetFilter()
        search.postFilter.addToTerms("post_filter_check", "cat")

        val result = searchService.search(Pager.first(), search)
        assertEquals(1, result.list.size)
    }
}

@TestPropertySource(locations = ["classpath:test.properties", "classpath:jwt.properties"])
class JwtTokenSecuritySearchServiceTests : AbstractTest() {

    @Test(expected = AccessDeniedException::class)
    fun testSearchWithJwtWriteAccessFailure() {
        authenticate("user", "source.filename:beer_kettle_01.jpg")
        val source = Source(getTestImagePath("beer_kettle_01.jpg"))
        source.setAttr("media.keywords", ImmutableList.of("captain"))
        assetService.createOrReplaceAssets(BatchCreateAssetsRequest(source))

        val search = AssetSearch().setQuery("captain")
        search.access = Access.Write
        assertEquals(1, searchService.search(search).hits.getTotalHits())
    }

    @Test
    fun testSearchWithJwtReadAccessAll() {
        authenticate("user", "source.filename:*", listOf(Groups.READ))
        val source = Source(getTestImagePath("beer_kettle_01.jpg"))
        source.setAttr("media.keywords", ImmutableList.of("captain"))
        assetService.createOrReplaceAssets(BatchCreateAssetsRequest(source))

        val search = AssetSearch().setQuery("captain")
        assertEquals(1, searchService.search(search).hits.getTotalHits())
    }

    @Test
    fun testSearchJwtReadAccessFilter() {
        authenticate("user", "source.filename:beer_kettle_01.jpg", listOf(Groups.READ))
        val source = Source(getTestImagePath("beer_kettle_01.jpg"))
        source.setAttr("media.keywords", ImmutableList.of("captain"))
        assetService.createOrReplaceAssets(BatchCreateAssetsRequest(source))

        val search = AssetSearch().setQuery("captain")
        assertEquals(1, searchService.search(search).hits.getTotalHits())
    }

    @Test(expected = AccessDeniedException::class)
    fun testSearchTokenPermissionsFailure() {
        authenticate("user", "source.filename:bilbo.mov")
        val source = Source(getTestImagePath("beer_kettle_01.jpg"))
        source.setAttr("media.keywords", ImmutableList.of("captain"))
        assetService.createOrReplaceAssets(BatchCreateAssetsRequest(source))

        val search = AssetSearch().setQuery("captain")
        searchService.search(search).hits.getTotalHits()
    }

    @Test
    fun testSearchWithJwtQStringFilter() {
        val source1 = Source(getTestImagePath("beer_kettle_01.jpg"))
        val source2 = Source(getTestImagePath("new_zealand_wellington_harbour.jpg"))
        assetService.createOrReplaceAssets(BatchCreateAssetsRequest(listOf(source1, source2)))

        authenticate("user")
        val currentUser = getUser()
        var perms = setOf(permissionService.getPermission(Groups.READ))

        // Build a new UserAuthed object
        val authedUser = UserAuthed(
            currentUser.id,
            currentUser.organizationId,
            currentUser.username,
            perms,
            currentUser.attrs,
            "source.filename:beer_kettle_01.jpg"
        )

        SecurityContextHolder.getContext().authentication =
            UnitTestAuthentication(authedUser, authedUser.authorities)

        val result = searchService.search(Pager.first(), AssetSearch())
        assertEquals(1, result.list.size)
    }

    @Test
    fun testSearchWithJwtQStringFilterAdminUser() {
        val source1 = Source(getTestImagePath("beer_kettle_01.jpg"))
        val source2 = Source(getTestImagePath("new_zealand_wellington_harbour.jpg"))
        assetService.createOrReplaceAssets(BatchCreateAssetsRequest(listOf(source1, source2)))

        val currentUser = getUser()

        // Build a new UserAuthed object
        // the filter doesn't matter for admins
        val authedUser = UserAuthed(
            currentUser.id,
            currentUser.organizationId,
            currentUser.username,
            currentUser.authorities.toSet(),
            currentUser.attrs,
            "source.filename:beer_kettle_01.jpg"
        )

        SecurityContextHolder.getContext().authentication =
            UnitTestAuthentication(authedUser, authedUser.authorities)

        val result = searchService.search(Pager.first(), AssetSearch())
        assertEquals(2, result.list.size)
    }

    override fun requiresElasticSearch(): Boolean {
        return true
    }

    override fun requiresFieldSets(): Boolean {
        return true
    }

    @Before
    fun init() {
        fieldService.invalidateFields()
    }
}
