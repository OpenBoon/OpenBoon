package com.zorroa.archivist.service

import com.google.common.io.Files
import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.LfsRequest
import com.zorroa.archivist.domain.OnlineFileCheckRequest
import com.zorroa.sdk.search.AssetSearch
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LocalFileSystemTests : AbstractTest() {

    @Autowired
    internal lateinit var lfs: LocalFileSystem

    @Test
    fun testPathSuggest() {
        val req = LfsRequest(resources.resolve("images").toString(), "set")

        val paths = lfs.suggest(req)
        assertTrue(paths.contains("set01/"))
        assertFalse(paths.contains("NOTICE"))
    }

    @Test
    fun testPathSuggestDirsHaveSlashes() {
        val req = LfsRequest(resources.toString(), "im")

        val paths = lfs.suggest(req)
        assertTrue(paths.contains("images/"))
    }

    @Test
    fun testExist() {
        val req = LfsRequest(resources.resolve("images").toString(), null)
        assertTrue(lfs.exists(req))

        val req2 = LfsRequest("/etc", null)
        assertFalse(lfs.exists(req2))
    }

    @Test
    fun testPathSuggestFiltered() {
        val req = LfsRequest("/show", null)

        val paths = lfs.listFiles(req)
        assertTrue(paths.getValue("files").isEmpty())
        assertTrue(paths.getValue("dirs").isEmpty())
    }

    @Test
    fun testPathSuggestByType() {
        val req = LfsRequest(resources.resolve("images/set06").toString(),
                null,
                setOf("cr2"))
        val suggested = lfs.suggest(req)
        assertEquals(1, suggested.size.toLong())
    }

    @Test
    fun testFilesOnline() {
        addTestAssets("set04")
        refreshIndex()

        try {
            Files.move(resources.resolve("images/set04/standard/beer_kettle_01.jpg").toFile(),
                    resources.resolve("images/set04/standard/beer_kettle_01.jpg.lfs_test").toFile())

            val req = OnlineFileCheckRequest(AssetSearch())
            val rsp = lfs.onlineFileCheck(req)
            assertEquals(6, rsp.total.toInt())
            assertEquals(5,rsp.totalOnline.toInt())
            assertEquals(1, rsp.totalOffline.toInt())
            assertEquals(1, rsp.offlineAssetIds.size)
        }
        finally {
            Files.move(resources.resolve("images/set04/standard/beer_kettle_01.jpg.lfs_test").toFile(),
                    resources.resolve("images/set04/standard/beer_kettle_01.jpg").toFile())

        }
    }
}
