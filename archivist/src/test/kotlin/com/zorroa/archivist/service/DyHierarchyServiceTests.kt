package com.zorroa.archivist.service

import com.google.common.collect.ImmutableList
import com.google.common.collect.Lists
import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.Acl
import com.zorroa.archivist.domain.BatchCreateAssetsRequest
import com.zorroa.archivist.domain.DyHierarchy
import com.zorroa.archivist.domain.DyHierarchyLevel
import com.zorroa.archivist.domain.DyHierarchyLevelType
import com.zorroa.archivist.domain.DyHierarchySpec
import com.zorroa.archivist.domain.Folder
import com.zorroa.archivist.domain.FolderSpec
import com.zorroa.archivist.domain.Pager
import com.zorroa.archivist.domain.Source
import com.zorroa.archivist.search.AssetFilter
import com.zorroa.archivist.search.AssetSearch
import com.zorroa.archivist.util.FileUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import java.io.File
import java.text.ParseException
import java.text.SimpleDateFormat

/**
 * Created by chambers on 7/14/16.
 */
class DyHierarchyServiceTests : AbstractTest() {

    @Autowired
    lateinit var dyhiService: DyHierarchyService

    override fun requiresElasticSearch(): Boolean {
        return true
    }

    @Before
    fun init() {

        for (path in getTestPaths("/set01/")) {
            val ab = Source(path)
            ab.setAttr("source.date",
                    SimpleDateFormat("dd-MM-yyyy HH:mm:ss").parse("04-07-2014 11:22:33"))
            ab.setAttr("tree.path", ImmutableList.of("/foo/bar/", "/bing/bang/", "/foo/shoe/"))
            logger.info("adding test asset {}", ab.path)
            assetService.createOrReplaceAssets(BatchCreateAssetsRequest(ab))
        }
        for (path in getTestPaths("/office/")) {
            val ab = Source(path)
            ab.setAttr("source.date",
                    SimpleDateFormat("dd-MM-yyyy HH:mm:ss").parse("03-05-2013 09:11:14"))
            logger.info("adding test asset {}", ab.path)
            assetService.createOrReplaceAssets(BatchCreateAssetsRequest(ab))
        }
        for (path in getTestPaths("/video/")) {
            val ab = Source(path)
            ab.setAttr("source.date",
                    SimpleDateFormat("dd-MM-yyyy HH:mm:ss").parse("11-12-2015 06:14:10"))
            logger.info("adding test asset {}", ab.path)
            assetService.createOrReplaceAssets(BatchCreateAssetsRequest(ab))
        }
        refreshIndex()
    }

    @Test
    fun testGenerateWithMultiplePaths() {
        val (id) = folderService.create(FolderSpec("foo"), false)
        val agg = DyHierarchy()
        agg.folderId = id
        agg.levels = ImmutableList.of(
                DyHierarchyLevel("tree.path", DyHierarchyLevelType.Path)
        )
        val result = dyhiService.generate(agg)

        var folder = folderService.get("/foo/foo/bar")
        assertEquals(5, searchService.count(folder!!.search!!))

        folder = folderService.get("/foo/bing/bang")
        assertEquals(5, searchService.count(folder!!.search!!))

        folder = folderService.get("/foo/foo/shoe")
        assertEquals(5, searchService.count(folder!!.search!!))

        folder = folderService.get("/foo/foo")
        assertEquals(5, searchService.count(folder!!.search!!))
    }

    @Test
    fun testGenerateAttrWithSlash() {
        val (id) = folderService.create(FolderSpec("foo"), false)
        val agg = DyHierarchy()
        agg.folderId = id
        agg.levels = ImmutableList.of(
                DyHierarchyLevel("source.directory"))
        val result = dyhiService.generate(agg)
        assertTrue(result > 0)

        val testDataPath = "_tmp"
        val folder1 = folderService.get("/foo/" + testDataPath + "_video")
        folderService.get("/foo/" + testDataPath + "_office")
        folderService.get("/foo/" + testDataPath + "_images_set01")
        assertEquals(8, searchService.count(folder1!!.search!!))
    }

    @Test
    fun testGenerateWithPath() {
        var folder: Folder? = folderService.create(FolderSpec("foo"), false)
        val agg = DyHierarchy()
        agg.folderId = folder!!.id
        agg.levels = ImmutableList.of(
                DyHierarchyLevel("source.type"),
                DyHierarchyLevel("source.directory", DyHierarchyLevelType.Path),
                DyHierarchyLevel("source.extension"))
        val result = dyhiService.generate(agg)

        val testDataPath = "/tmp"
        // Video aggs
        folder = folderService.get("/foo/video$testDataPath/video/m4v")
        assertEquals(1, searchService.count(folder!!.search!!))

        folder = folderService.get("/foo/video$testDataPath/video")
        assertEquals(8, searchService.count(folder!!.search!!))

        folder = folderService.get("/foo/video$testDataPath")

        assertEquals(8, searchService.count(folder!!.search!!))

        folder = folderService.get("/foo/video")
        assertEquals(8, searchService.count(folder!!.search!!))
    }

    @Test
    fun testGenerate() {
        val (id) = folderService.create(FolderSpec("foo"), false)
        val agg = DyHierarchy()
        agg.folderId = id
        agg.levels = ImmutableList.of(
                DyHierarchyLevel("source.date", DyHierarchyLevelType.Year),
                DyHierarchyLevel("source.type"),
                DyHierarchyLevel("source.extension"),
                DyHierarchyLevel("source.filename"))
        assertTrue(dyhiService.generate(agg) > 0)
    }

    @Test
    fun testGenerateWithPermissions() {
        val (id) = folderService.create(FolderSpec("foo"), false)
        val agg = DyHierarchy()
        agg.folderId = id
        agg.levels = ImmutableList.of(
                DyHierarchyLevel("source.extension")
                        .setAcl(Acl().addEntry("zorroa::foo", 3)))

        dyhiService.generate(agg)
        assertEquals("zorroa::foo",
                folderService.get("/foo/jpg")!!.acl!![0].permission)
        assertEquals(1,
                folderService.get("/foo/jpg")!!.acl!!.size.toLong())
    }

    @Test
    fun testGenerateWithDynamicPermissions() {
        val (id) = folderService.create(FolderSpec("foo"), false)
        val agg = DyHierarchy()
        agg.folderId = id
        agg.levels = ImmutableList.of(
                DyHierarchyLevel("source.extension")
                        .setAcl(Acl().addEntry("zorroa::%{name}", 3)))

        assertTrue(dyhiService.generate(agg) > 0)
        assertEquals("zorroa::jpg",
                folderService.get("/foo/jpg")!!.acl!![0].permission)
        assertEquals(1,
                folderService.get("/foo/jpg")!!.acl!!.size.toLong())
    }

    @Test
    fun testDeleteEmptyFolders() {
        refreshIndex()
        val (id) = folderService.create(FolderSpec("foo"), false)
        val agg = DyHierarchy()
        agg.folderId = id
        agg.levels = ImmutableList.of(
                DyHierarchyLevel("source.type"),
                DyHierarchyLevel("source.extension"))

        val result = dyhiService.generate(agg)
        assertEquals(13, result)

        val assets = indexService.getAll(Pager.first(100))
        assertTrue(assets.size() > 0)
        for (asset in assets) {
            asset.setAttr("source.extension", "abc")
        }
        assetService.updateAssets(assets.list)
        refreshIndex()
        assertEquals(6, dyhiService.generate(agg))
    }

    @Test
    fun testGenerateDateHierarchy() {
        val (id) = folderService.create(FolderSpec("foo"), false)
        val agg = DyHierarchy()
        agg.folderId = id
        agg.levels = ImmutableList.of(
                DyHierarchyLevel("source.date", DyHierarchyLevelType.Year),
                DyHierarchyLevel("source.date", DyHierarchyLevelType.Month),
                DyHierarchyLevel("source.date", DyHierarchyLevelType.Day))

        dyhiService.generate(agg)

        var year = folderService.get(id, "2014")
        assertEquals(5, searchService.search(year.search!!).hits.getTotalHits())

        val (id1, _, _, _, _, _, _, _, _, _, _, _, _, search) = folderService.get(year.id, "July")
        assertEquals(5, searchService.search(search!!).hits.getTotalHits())

        val (_, _, _, _, _, _, _, _, _, _, _, _, _, search1) = folderService.get(id1, "4")
        assertEquals(5, searchService.search(search1!!).hits.getTotalHits())

        year = folderService.get(id, "2013")
        assertEquals(6, searchService.search(year.search!!).hits.getTotalHits())
    }

    @Test
    fun testGenerateDateHierarchyWithSmartFolder() {
        val spec = FolderSpec("foo")
        spec.search = AssetSearch().setFilter(AssetFilter().addToTerms("tree.path.paths",
                Lists.newArrayList<Any>("/foo/bar")))
        val (id) = folderService.create(spec, false)

        val agg = DyHierarchy()
        agg.folderId = id
        agg.levels = ImmutableList.of(
                DyHierarchyLevel("source.date", DyHierarchyLevelType.Year),
                DyHierarchyLevel("source.date", DyHierarchyLevelType.Month),
                DyHierarchyLevel("source.date", DyHierarchyLevelType.Day))

        dyhiService.generate(agg)

        val (id1, _, _, _, _, _, _, _, _, _, _, _, _, search) = folderService.get(id, "2014")
        assertEquals(5, searchService.search(search!!).hits.getTotalHits())

        val (id2, _, _, _, _, _, _, _, _, _, _, _, _, search1) = folderService.get(id1, "July")
        assertEquals(5, searchService.search(search1!!).hits.getTotalHits())

        val (_, _, _, _, _, _, _, _, _, _, _, _, _, search2) = folderService.get(id2, "4")
        assertEquals(5, searchService.search(search2!!).hits.getTotalHits())

        assertFalse(folderService.exists("/foo/2013"))
    }

    @Test
    fun testGenerateAndUpdate() {

        val (id) = folderService.create(FolderSpec("foo"), false)
        val agg = DyHierarchy()
        agg.folderId = id
        agg.levels = ImmutableList.of(
                DyHierarchyLevel("source.date", DyHierarchyLevelType.Year),
                DyHierarchyLevel("source.type"),
                DyHierarchyLevel("source.extension"),
                DyHierarchyLevel("source.filename"))

        dyhiService.generate(agg)

        for (f in getTestImagePath("set02")) {
            val ab = Source(f)
            indexService.index(ab)
        }

        for (f in getTestImagePath("set03")) {
            val ab = Source(f)
            indexService.index(ab)
        }

        refreshIndex()
        dyhiService.generate(agg)
    }

    @Test
    fun testDyhiChildCounts() {
        var folder = folderService.create(FolderSpec("foo"), false)
        val spec = DyHierarchySpec(folder.id, listOf(
                DyHierarchyLevel("source.date", DyHierarchyLevelType.Day)))

        val dyhi = dyhiService.create(spec)
        folder = folderService.get(folder.id)
        assertTrue(folder.search!!.filter.exists.contains("source.date"))

        folder = folderService.get(folder.id)
        assertEquals(3, folder.childCount.toLong())
    }

    @Test
    fun testDeleteDyhi() {
        var folder = folderService.create(FolderSpec("foo"), false)
        val spec = DyHierarchySpec(folder.id, listOf(
                DyHierarchyLevel("source.date", DyHierarchyLevelType.Day)))

        val dyhi = dyhiService.create(spec)
        folder = folderService.get(folder.id)
        dyhiService.delete(dyhi)
        folder = folderService.get(folder.id)
        assertEquals(0, folder.childCount.toLong())
    }

    @Test
    fun testUpdate() {
        var folder = folderService.create(FolderSpec("foo"), false)
        val spec = DyHierarchySpec(folder.id, listOf(
                DyHierarchyLevel("source.date", DyHierarchyLevelType.Day),
                DyHierarchyLevel("source.type"),
                DyHierarchyLevel("source.extension"),
                DyHierarchyLevel("source.filename")))
        val dyhi = dyhiService.create(spec)
        folder = folderService.get(folder.id)
        assertTrue(folder.search!!.filter.exists.contains("source.date"))

        dyhi.levels = ImmutableList.of(
                DyHierarchyLevel("source.type"),
                DyHierarchyLevel("source.extension"),
                DyHierarchyLevel("source.filename"))
        dyhiService.update(dyhi.id, dyhi)
        folder = folderService.get(folder.id)
        assertTrue(folder.search!!.filter.exists.contains("source.type"))
    }

    @Test
    fun testUpdateWithSmartQuery() {
        val fspec = FolderSpec("foo")
        fspec.search = AssetSearch("beer")
        var folder = folderService.create(fspec, false)
        val spec = DyHierarchySpec(folder.id,
                listOf(DyHierarchyLevel("source.date", DyHierarchyLevelType.Day),
                DyHierarchyLevel("source.type"),
                DyHierarchyLevel("source.extension"),
                DyHierarchyLevel("source.filename")))
        val dyhi = dyhiService.create(spec)

        folder = folderService.get(folder.id)
        assertTrue(folder.search!!.filter.exists.contains("source.date"))
        assertEquals("beer", folder.search!!.query)
        assertEquals(1, folder.search!!.filter.exists.size.toLong())

        dyhi.levels = ImmutableList.of(
                DyHierarchyLevel("source.type"),
                DyHierarchyLevel("source.extension"),
                DyHierarchyLevel("source.filename"))
        dyhiService.update(dyhi.id, dyhi)
        folder = folderService.get(folder.id)
        assertTrue(folder.search!!.filter.exists.contains("source.type"))
        assertEquals(1, folder.search!!.filter.exists.size.toLong())
        assertEquals("beer", folder.search!!.query)
    }
}
