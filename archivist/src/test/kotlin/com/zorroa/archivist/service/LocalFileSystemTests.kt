package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.LfsRequest
import com.zorroa.archivist.domain.OnlineFileCheckReq
import com.zorroa.sdk.search.AssetSearch
import com.zorroa.sdk.util.Json
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
        addTestAssets("set04/standard")
        refreshIndex()

        val req = OnlineFileCheckReq(AssetSearch("beer"))
        val rsp = lfs.onlineFileCheck(req)
        logger.info(Json.prettyString(rsp))

    }
}
