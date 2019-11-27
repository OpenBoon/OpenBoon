package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.config.SpringApplicationProperties
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import java.net.URI
import java.nio.file.Paths
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Ignore
class GcpFileServerServiceTests : AbstractTest() {

    val bucketName = "zorroa-evi-dev-integration-tests"
    lateinit var fileStorage: GcsFileStorageService

    /**
     * For this tests we use our own evi dev creds.
     */
    val creds = Paths.get("unittest/config/zorroa-credentials.json")
    val fileServer = GcpFileServerService(creds)

    @Before
    fun init() {
        fileStorage = GcsFileStorageService(bucketName, SpringApplicationProperties(), fileServerProvider, creds)
        //fileStorage.fileServerProvider = fileServerProvider
    }

    @Test
    fun testGetSignedUrl() {
        val u = fileServer.getSignedUrl(
                URI("gs://zorroa-evi-dev-integration-tests/orgs/00000000-9998-8888-7777-666666666666/asset/48230000-f290-474b-82d4-4efcdcad881b/source.png"))

        assertEquals("https", u.protocol)
        assertEquals("storage.googleapis.com", u.authority)
        assertTrue(u.query.contains("GoogleAccessId="))
        assertTrue(u.query.contains("&Signature="))
    }

    @Test
    fun testDeleteDirectory() {
        val id = UUID.randomUUID()
        for (i in 1..5) {
            val fs = fileStorage.get("job___${id}___${i}foo.txt")
            fileStorage.write(fs.id, "Mamba!".toByteArray())
        }

        val fs = fileStorage.get("job___$id")
        assertTrue(fileServer.delete(fs.uri))
    }

    @Test
    fun testDeleteFile() {
        val id = UUID.randomUUID()
        val fs = fileStorage.get("job___${id}___foo.txt")
        fileStorage.write(fs.id, "Mamba!".toByteArray())

        assertTrue(fileServer.objectExists(fs.uri))
        assertTrue(fileServer.delete(fs.uri))
    }
}
/*
class LocalFileServerServiceTests : AbstractTest() {

    val fileServer = LocalFileServerService()
    val testShared = Files.createTempDirectory("test")
    lateinit var fileStorage: LocalFileStorageService

    @Before
    fun init() {
        fileStorage = LocalFileStorageService(testShared)
        fileStorage.fileServerProvider = fileServerProvider
    }

    @Test
    fun testGetStorageUriPathWithSpace() {
        val doc = Asset(randomString())
        doc.setAttr("source.path", "/foo/bar/bing bang.jpg")
        val uri = fileStorage.fileServerProvider.getStorageUri(doc)
        assertEquals("file:///foo/bar/bing%20bang.jpg", uri.toString())
    }

    @Test
    fun testGetStorageUrWithSpace() {
        val doc = Asset(randomString())
        doc.setAttr("source.path", "file:///foo/bar/bing bang.jpg")
        val uri = fileStorage.fileServerProvider.getStorageUri(doc)
        assertEquals("file:///foo/bar/bing%20bang.jpg", uri.toString())
    }

    @Test
    fun testDeleteDirectory() {
        val id = UUID.randomUUID()
        val fs = fileStorage.get("job___$id")
        Paths.get(fs.uri).toFile().mkdirs()
        assertTrue(fileServer.delete(fs.uri))
    }

    @Test
    fun testDeleteFile() {
        val id = UUID.randomUUID()
        val fs = fileStorage.get("job___${id}___foo.txt")
        fileStorage.getSignedUrl(fs.id, HttpMethod.PUT)

        Files.write(fs.getServableFile().getLocalFile(), "a-team".toByteArray())
        fileServer.objectExists(fs.uri)
        assertTrue(fileServer.delete(fs.uri))
    }
}
*/
