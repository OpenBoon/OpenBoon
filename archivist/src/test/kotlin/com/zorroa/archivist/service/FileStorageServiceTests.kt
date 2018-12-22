package com.zorroa.archivist.service

import com.google.cloud.storage.HttpMethod
import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.FileStorageSpec
import com.zorroa.archivist.filesystem.UUIDFileSystem
import com.zorroa.archivist.util.FileUtils
import org.junit.Before
import org.junit.Test
import java.net.URI
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue


class DefaultLocalLayoutProviderTests : AbstractTest() {

    val layout : LayoutProvider

    val pspec = FileStorageSpec(
            "proxy",
            "a_proxy",
            "jpg",
            listOf("x", "100", "y", "100"),
            assetId=UUID.fromString("E415845C-E2F5-441B-A36E-36103D231169"))

    val espec = FileStorageSpec(
            "export",
            "some_export",
            "zip",
            jobId=UUID.fromString("E415845C-E2F5-441B-A36E-36103D231169"))

    init {
        val shared = Paths.get("unittest/shared")
        layout =  LocalLayoutProvider(shared, UUIDFileSystem(shared.resolve("ofs")))
    }

    @Test
    fun testBuildIdFromSpec() {
        assertEquals("proxy___e415845c-e2f5-441b-a36e-36103d231169_x_100_y_100.jpg",
                layout.buildId(pspec))
        assertEquals("export___e415845c-e2f5-441b-a36e-36103d231169___some_export.zip",
                layout.buildId(espec))
    }

    @Test
    fun testProxyBuildFrom39Spec() {
        val spec = FileStorageSpec(
                "proxy",
                "testing123",
                "jpg")

        assertEquals("proxy___c0eaa63b-36a2-5962-9dc5-3b560800251c.jpg", layout.buildId(spec))
        val uri = layout.buildUri(spec)
        assertTrue(uri.startsWith("file:///"))
        assertTrue(uri.endsWith(
                "archivist/unittest/shared/ofs/proxy/c/0/e/a/a/6/3/b/c0eaa63b-36a2-5962-9dc5-3b560800251c.jpg"))
    }

    @Test
    fun testProxyBuildFromUUIDName() {
        val spec = FileStorageSpec(
                "proxy",
                "c0eaa63b-36a2-5962-9dc5-3b560800251c",
                "jpg")

        assertEquals("proxy___c0eaa63b-36a2-5962-9dc5-3b560800251c.jpg", layout.buildId(spec))
        val uri = layout.buildUri(spec)
        assertTrue(uri.startsWith("file:///"))
        assertTrue(uri.endsWith(
                "archivist/unittest/shared/ofs/proxy/c/0/e/a/a/6/3/b/c0eaa63b-36a2-5962-9dc5-3b560800251c.jpg"))
    }

    @Test
    fun testBuildUriFromSpec() {
        val proxyUri = layout.buildUri(pspec)
        assertTrue(proxyUri.startsWith("file:///"))
        assertTrue(proxyUri.endsWith(
                "archivist/unittest/shared/ofs/proxy/e/4/1/5/8/4/5/c/e415845c-e2f5-441b-a36e-36103d231169_x_100_y_100.jpg"))

        val exportUri = layout.buildUri(espec)
        assertTrue(exportUri.startsWith("file:///"))
        assertTrue(exportUri.endsWith(
                "archivist/unittest/shared/exports/e415845c-e2f5-441b-a36e-36103d231169/some_export.zip"))
    }

    @Test
    fun testBuildUriFromId() {
        val pid = "proxy___4df4d729-f3fc-5008-8373-0438c1968470_x_100_y_100.jpg"
        val export = "export___e415845c-e2f5-441b-a36e-36103d231169___some_export.zip"

        val proxyUri = layout.buildUri(pid)
        assertTrue(proxyUri.startsWith("file:///"))
        assertTrue(proxyUri.endsWith(
                "archivist/unittest/shared/ofs/proxy/4/d/f/4/d/7/2/9/4df4d729-f3fc-5008-8373-0438c1968470_x_100_y_100.jpg"))

        val exportUri = layout.buildUri(export)
        assertTrue(exportUri.startsWith("file:///"))
        assertTrue(exportUri.endsWith(
                "archivist/unittest/shared/exports/e415845c-e2f5-441b-a36e-36103d231169/some_export.zip"))
    }
}


class DefaultGcsLayoutProviderTests : AbstractTest() {

    val layout = GcsLayoutProvider("foo")

    @Test
    fun testAnalystUser() {
        authenticateAsAnalyst()
        val spec = FileStorageSpec(
                "proxy",
        "so_urgent",
        "jpg")
        val id1 = layout.buildId(spec)
        val uri = layout.buildUri(id1)
    }

    @Test
    fun testGenerateId() {
        val spec = FileStorageSpec(
                "proxy",
                "so_urgent",
                "jpg",
                listOf("x", "100", "y", "100"))

        val id = layout.buildId(spec)
        assertEquals("proxy___f920837e-9d15-5963-998b-38bd44b17468___so_urgent_x_100_y_100.jpg", id)
    }

    @Test
    fun testGenerateIdNoVariants() {
        val spec = FileStorageSpec(
                "proxy",
                "so_urgent",
                "jpg")

        val id = layout.buildId(spec)
        assertEquals("proxy___f920837e-9d15-5963-998b-38bd44b17468___so_urgent.jpg", id)
    }

    @Test
    fun testGetUri() {
        val spec = FileStorageSpec(
                "proxy",
                "so_urgent",
                "jpg")

        val id1 = layout.buildId(spec)
        val uri = layout.buildUri(id1)
        println(id1)
        println(uri)
    }

    @Test
    fun testUseAssetId() {
        val assetId = UUID.fromString("16C13597-CE2F-4CB2-8EB0-F01AEA1702D0")
        val spec = FileStorageSpec(
                "proxy",
                "so_urgent",
                "jpg",
                listOf("x", "200", "y", "200"),assetId=assetId)

        val id = layout.buildId(spec)
        assertEquals("proxy___16c13597-ce2f-4cb2-8eb0-f01aea1702d0___so_urgent_x_200_y_200.jpg", id)
    }

    @Test
    fun getUri() {
        val assetId = UUID.fromString("16C13597-CE2F-4CB2-8EB0-F01AEA1702D0")
        val spec = FileStorageSpec(
                "proxy",
                "so_urgent",
                "jpg",
                listOf("x", "200", "y", "200"),assetId=assetId)

        val uri = layout.buildUri(spec)
        println(uri)
    }

    @Test
    fun getIdFromPath() {
        val assetId = UUID.fromString("16C13597-CE2F-4CB2-8EB0-F01AEA1702D0")
        val spec = FileStorageSpec(
                "proxy",
                "so_urgent",
                "jpg",
                listOf("x", "200", "y", "200"),assetId=assetId)

        val uri = layout.buildUri(spec)

        println(uri)
    }

}

class GcsFileStorageServiceTests : AbstractTest() {

    val bucketName = "rmaas-dit-2-zorroa-data"
    lateinit var fileStorage: GcsFileStorageService

    @Before
    fun init() {
        fileStorage = GcsFileStorageService(bucketName,
                Paths.get("unittest/config/data-credentials.json"))
        fileStorage.fileServerProvider = fileServerProvider
    }

    @Test
    fun testGetUri() {
        val spec = FileStorageSpec("proxy", "foo_bar", "jpg")
        val uri = fileStorage.dlp.buildUri(spec)

        val expectedPath = "/orgs/00000000-9998-8888-7777-666666666666/ofs/proxy/38f154c7-e214-551b-9374-075c2bac63a1/foo_bar.jpg"
        val auri = URI.create(uri)

        assertEquals(bucketName,  auri.authority)
        assertEquals(expectedPath, auri.path)
        assertEquals("gs", auri.scheme)
    }

    @Test
    fun testGetId() {
        val spec = FileStorageSpec("proxy", "foo_bar", "jpg")
        val id = fileStorage.dlp.buildId(spec)
        assertTrue(id.startsWith("proxy_"))
        assertTrue(id.endsWith("_foo_bar.jpg"))
    }

    @Test
    fun testSignUrl() {
        val spec = FileStorageSpec("proxy", "foo_bar", "jpg")
        val id = fileStorage.dlp.buildId(spec)
        println(fileStorage.getSignedUrl(id, HttpMethod.PUT))
    }

    @Test
    fun testGetGetBySpec() {
        val spec = FileStorageSpec("proxy", "foo_bar", "jpg")
        val storage = fileStorage.get(spec)
        assertEquals(storage.mediaType, "image/jpeg")
    }

    @Test
    fun testGetGetById() {
        val spec = FileStorageSpec("proxy", "foo_bar", "jpg")
        val storage1 = fileStorage.get(spec)
        val storage2 = fileStorage.get(storage1.id)
        assertEquals(storage1.uri, storage2.uri)
        assertEquals(storage1.id, storage2.id)
        assertEquals(storage1.mediaType, storage2.mediaType)
    }

}

class LocalFileStorageServiceTests : AbstractTest() {

    val testShared = Files.createTempDirectory("test")
    val ofs: UUIDFileSystem = UUIDFileSystem(testShared.resolve("ofs"))
    lateinit var fileStorage: LocalFileStorageService

    @Before
    fun init() {
        fileStorage = LocalFileStorageService(testShared, ofs)
        fileStorage.fileServerProvider = fileServerProvider
    }

    @Test
    fun testCreateWithVariants() {
        val spec = FileStorageSpec("proxy", "abc123", "jpg",
                listOf("x", "100", "y", "100"))
        val fs = fileStorage.get(spec)
        assertTrue(FileUtils.filename(fs.uri).endsWith("x_100_y_100.jpg"))
        assertEquals("image/jpeg", fs.mediaType)
    }


    @Test
    fun testGetById() {
        val spec = FileStorageSpec("proxy", "abc123", "jpg",
                listOf("x", "100", "y", "100"))
        val fs = fileStorage.get(spec)
        println(fs.id)
    }

    @Test
    fun testCreate() {
        val spec = FileStorageSpec("proxy", "abc123", "jpg", null)
        val fs = fileStorage.get(spec)
        assertTrue(FileUtils.filename(fs.uri).endsWith(".jpg"))
        assertEquals("image/jpeg", fs.mediaType)
        assertEquals("file", fs.scheme)
    }
}