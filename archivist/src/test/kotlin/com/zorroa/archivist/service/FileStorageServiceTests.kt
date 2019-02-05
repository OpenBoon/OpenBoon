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

    init {
        val shared = Paths.get("unittest/shared")
        layout =  LocalLayoutProvider(shared, UUIDFileSystem(shared.resolve("ofs")))
    }

    @Test
    fun testBuildIdFromSpec() {
        val spec = FileStorageSpec(
                "asset",
                "e415845c-e2f5-441b-a36e-36103d231169",
                "proxy/100x200.jpg")

        assertEquals("asset___e415845c-e2f5-441b-a36e-36103d231169___proxy___100x200.jpg", layout.buildId(spec))
    }

    @Test
    fun testBuildUriFromSpec() {
        val spec = FileStorageSpec(
                "job",
                "e415845c-e2f5-441b-a36e-36103d231169",
                "export/exported_files.zip")

        val proxyUri = layout.buildUri(spec)
        println(proxyUri)
        assertTrue(proxyUri.startsWith("file:///"))
        assertTrue(proxyUri.endsWith(
                "shared/orgs/00000000-9998-8888-7777-666666666666/job/e/4/1/5/e415845c-e2f5-441b-a36e-36103d231169/export/exported_files.zip"))
    }

    @Test
    fun testBuildUriFromId() {
        val id = "asset___e415845c-e2f5-441b-a36e-36103d231169___proxy___100x200.jpg"

        val proxyUri = layout.buildUri(id)
        println(proxyUri)
        assertTrue(proxyUri.startsWith("file:///"))
        assertTrue(proxyUri.endsWith(
                "shared/orgs/00000000-9998-8888-7777-666666666666/asset/e/4/1/5/e415845c-e2f5-441b-a36e-36103d231169/proxy/100x200.jpg"))
    }

    @Test
    fun testBuildUriFromShortId() {
        val id = "job___e415845c-e2f5-441b-a36e-36103d231169"
        val uri = layout.buildUri(id)
        assertTrue(uri.endsWith("job/e/4/1/5/e415845c-e2f5-441b-a36e-36103d231169"))
    }

    @Test
    fun testBuildUriFrom39SlugId() {
        val id = "proxy/c0eaa63b-36a2-5962-9dc5-3b560800251c_1024x768.jpg"
        val uri = layout.buildUri(id)
        assertTrue(uri.endsWith("shared/ofs/proxy/c/0/e/a/a/6/3/b/c0eaa63b-36a2-5962-9dc5-3b560800251c_1024x768.jpg"))
    }
}


class GcsLayoutProviderTests : AbstractTest() {

    val layout = GcsLayoutProvider("foo")

    @Test
    fun testBuildIdFromSpec() {
        val pid = UUID.randomUUID()
        val spec = FileStorageSpec(
                "asset",
                pid,
                "so_urgent_x_100_y_100.jpg")

        val id = layout.buildId(spec)
        assertEquals("asset___${pid}___so_urgent_x_100_y_100.jpg", id)
    }

    @Test
    fun testBuildUriFromId() {
        val pid = UUID.randomUUID()
        val spec = FileStorageSpec(
                "asset",
                pid,
                "proxy/500x500.jpg")

        val id1 = layout.buildId(spec)
        val uri = layout.buildUri(id1)
        assertEquals(uri, layout.buildUri(id1))
    }

    @Test
    fun testBuildUriFromSpec() {
        val pid = UUID.randomUUID()
        val spec = FileStorageSpec(
                "asset",
                pid,
                "proxy/500x500.jpg")

        val uri = layout.buildUri(spec)
        val id1 = layout.buildId(spec)
        assertEquals(uri, layout.buildUri(id1))
        assertTrue(uri.startsWith("gs://"))
    }


    @Test
    fun testBuildUriFromDeprecatedIdStyle() {
        val id = "proxy___098c296c-33dd-594a-827c-26118ff62882___098c296c-33dd-594a-827c-26118ff62882_256x144.jpg"
        val uri = layout.buildUri(id)
        val expected = "gs://foo/orgs/00000000-9998-8888-7777-666666666666/ofs/proxy/098c296c-33dd-594a-827c-26118ff62882/098c296c-33dd-594a-827c-26118ff62882_256x144.jpg"
        assertEquals(expected, uri)
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
        val pid = UUID.randomUUID()
        val spec = FileStorageSpec("asset", pid, "foo_bar.jpg")
        val uri = fileStorage.dlp.buildUri(spec)

        val expectedPath = "/orgs/00000000-9998-8888-7777-666666666666/asset/${pid}/foo_bar.jpg"
        val auri = URI.create(uri)

        assertEquals(bucketName,  auri.authority)
        assertEquals(expectedPath, auri.path)
        assertEquals("gs", auri.scheme)
    }

    @Test
    fun testGetId() {
        val pid = UUID.randomUUID()
        val spec = FileStorageSpec("asset", pid, "foo_bar.jpg")
        val id = fileStorage.dlp.buildId(spec)
        println(id)
        assertTrue(id.startsWith("asset_"))
        assertTrue(id.endsWith("_foo_bar.jpg"))
    }

    @Test
    fun testSignUrl() {
        val spec = FileStorageSpec("asset", UUID.randomUUID().toString(), "jpg")
        val id = fileStorage.dlp.buildId(spec)
        val url = fileStorage.getSignedUrl(id, HttpMethod.PUT)
        assertTrue(url.startsWith("https://storage.googleapis.com/"))
    }

    @Test
    fun testSignUrlGet() {
        val spec = FileStorageSpec("asset", UUID.randomUUID().toString(), "jpg")
        val id = fileStorage.dlp.buildId(spec)
        val url = fileStorage.getSignedUrl(id, HttpMethod.GET)
        assertTrue(url.startsWith("https://storage.googleapis.com/"))
    }

    @Test
    fun testGetGetBySpec() {
        val pid = UUID.randomUUID()
        val spec = FileStorageSpec("asset", pid, "foo_bar.jpg")
        val storage = fileStorage.get(spec)
        assertEquals(storage.mediaType, "image/jpeg")
    }

    @Test
    fun testGetGetById() {
        val spec = FileStorageSpec("asset", UUID.randomUUID().toString(), "jpg")
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
    fun testCreateWithDirectory() {
        val pid = UUID.randomUUID()
        val spec = FileStorageSpec("asset", pid, "thing/abc123.jpg")
        val fs = fileStorage.get(spec)

        assertTrue(FileUtils.filename(fs.uri.toString()).endsWith("abc123.jpg"))
        assertEquals("image/jpeg", fs.mediaType)
    }

    @Test
    fun testGetById() {
        val spec = FileStorageSpec("asset", UUID.randomUUID(), "jpg")
        val fs = fileStorage.get(spec)
        println(fs.id)
    }

    @Test
    fun testCreate() {
        val spec = FileStorageSpec("asset", UUID.randomUUID(), "foo.jpg")
        val fs = fileStorage.get(spec)
        assertTrue(FileUtils.filename(fs.uri.toString()).endsWith(".jpg"))
        assertEquals("image/jpeg", fs.mediaType)
        assertEquals("file", fs.scheme)
    }

    @Test
    fun testWrite() {
        val spec = FileStorageSpec("asset", UUID.randomUUID(), "foo.jpg")
        val fs = fileStorage.get(spec)
        fileStorage.write(fs.id, "Foo".toByteArray())
        assertTrue(Files.exists(Paths.get(fs.uri)))
    }
}