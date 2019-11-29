package com.zorroa.archivist.service

import com.google.cloud.storage.HttpMethod
import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.config.ApplicationProperties
import com.zorroa.archivist.domain.FileStorageSpec
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import java.net.URI
import java.nio.file.Paths
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DefaultLocalLayoutProviderTests : AbstractTest() {

    val layout: LayoutProvider

    init {
        val shared = Paths.get("unittest/shared")
        layout = LocalLayoutProvider(shared)
    }

    @Test
    fun testBuildIdFromSpec() {
        val spec = FileStorageSpec(
            "asset",
            "e415845c-e2f5-441b-a36e-36103d231169",
            "proxy/100x200.jpg"
        )

        assertEquals("asset___e415845c-e2f5-441b-a36e-36103d231169___proxy___100x200.jpg", layout.buildId(spec))
    }

    @Test
    fun testBuildUriFromSpec() {
        val spec = FileStorageSpec(
            "job",
            "e415845c-e2f5-441b-a36e-36103d231169",
            "export/exported_files.zip"
        )

        val proxyUri = layout.buildUri(spec)
        println(proxyUri)
        assertTrue(proxyUri.startsWith("file:///"))
        assertTrue(
            proxyUri.endsWith(
                "shared/projects/00000000-0000-0000-0000-000000000000/job/e/4/1/5/e415845c-e2f5-441b-a36e-36103d231169/export/exported_files.zip"
            )
        )
    }

    @Test
    fun testBuildUriFromId() {
        val id = "asset___e415845c-e2f5-441b-a36e-36103d231169___proxy___100x200.jpg"

        val proxyUri = layout.buildUri(id)
        println(proxyUri)
        assertTrue(proxyUri.startsWith("file:///"))
        assertTrue(
            proxyUri.endsWith(
                "shared/projects/00000000-0000-0000-0000-000000000000/asset/e/4/1/5/e415845c-e2f5-441b-a36e-36103d231169/proxy/100x200.jpg"
            )
        )
    }

    @Test
    fun testBuildUriFromShortId() {
        val id = "job___e415845c-e2f5-441b-a36e-36103d231169"
        val uri = layout.buildUri(id)
        assertTrue(uri.endsWith("job/e/4/1/5/e415845c-e2f5-441b-a36e-36103d231169"))
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
            "so_urgent_x_100_y_100.jpg"
        )

        val id = layout.buildId(spec)
        assertEquals("asset___${pid}___so_urgent_x_100_y_100.jpg", id)
    }

    @Test
    fun testBuildUriFromId() {
        val pid = UUID.randomUUID()
        val spec = FileStorageSpec(
            "asset",
            pid,
            "proxy/500x500.jpg"
        )

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
            "proxy/500x500.jpg"
        )

        val uri = layout.buildUri(spec)
        val id1 = layout.buildId(spec)
        assertEquals(uri, layout.buildUri(id1))
        assertTrue(uri.startsWith("gs://"))
    }

    @Test
    fun testBuildUriFromDeprecatedIdStyle() {
        val id = "proxy___098c296c-33dd-594a-827c-26118ff62882___098c296c-33dd-594a-827c-26118ff62882_256x144.jpg"
        val uri = layout.buildUri(id)
        val expected =
            "gs://foo/projects/00000000-0000-0000-0000-000000000000/ofs/proxy/098c296c-33dd-594a-827c-26118ff62882/098c296c-33dd-594a-827c-26118ff62882_256x144.jpg"
        assertEquals(expected, uri)
    }
}

class GcsFileStorageServiceTests : AbstractTest() {

    @Autowired
    lateinit var applicationProperties: ApplicationProperties

    val bucketName = "zorroa-dev-data"
    lateinit var fileStorage: GcsFileStorageService

    @Before
    fun init() {
        fileStorage = GcsFileStorageService(bucketName, applicationProperties, fileServerProvider)

        // fileStorage = GcsFileStorageService(bucketName)
        // fileStorage.fileServerProvider = fileServerProvider
    }

    @Test
    fun testGetUri() {
        val pid = UUID.randomUUID()
        val spec = FileStorageSpec("asset", pid, "foo_bar.jpg")
        val uri = fileStorage.dlp.buildUri(spec)

        val expectedPath = "/projects/00000000-0000-0000-0000-000000000000/asset/$pid/foo_bar.jpg"
        val auri = URI.create(uri)

        assertEquals(bucketName, auri.authority)
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

    @Ignore
    @Test
    fun testSignUrl() {
        val spec = FileStorageSpec("asset", UUID.randomUUID().toString(), "jpg")
        val id = fileStorage.dlp.buildId(spec)
        val url = fileStorage.getSignedUrl(id, HttpMethod.PUT)
        assertTrue(url.startsWith("https://storage.googleapis.com/"))
    }

    @Ignore
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
/*
class LocalFileStorageServiceTests : AbstractTest() {

    val testShared = Files.createTempDirectory("test")
    lateinit var fileStorage: LocalFileStorageService

    @Before
    fun init() {
        fileStorage = LocalFileStorageService(testShared)
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
*/
