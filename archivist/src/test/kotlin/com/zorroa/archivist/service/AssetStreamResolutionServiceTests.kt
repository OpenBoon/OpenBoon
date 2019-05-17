package com.zorroa.archivist.service

import com.zorroa.archivist.domain.Document
import com.zorroa.archivist.domain.FileStorage
import com.zorroa.archivist.util.StaticUtils
import com.zorroa.common.schema.Proxy
import com.zorroa.common.schema.ProxySchema
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.springframework.http.MediaType
import java.net.URI
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 *  Tests for {@link AssetStreamResolutionService}.
 */
class AssetStreamResolutionServiceTests {

    @Rule
    @JvmField
    var expectedException = ExpectedException.none()!!

    @Mock
    lateinit var mockIndexService: IndexService

    @Mock
    lateinit var mockFileStorageService: FileStorageService

    @Mock
    lateinit var mockFileServerProvider: FileServerProvider

    @Mock
    lateinit var mockFileServerService: FileServerService

    lateinit var service: AssetStreamResolutionService

    private val sourceFileUri = URI("file://foo/file")

    private lateinit var id: String
    private lateinit var imageProxyId: String
    private lateinit var videoProxyId: String
    private lateinit var imageProxyUri: URI
    private lateinit var videoProxyUri: URI

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)

        id = UUID.randomUUID().toString()

        imageProxyId = "proxy___${id}_image.jpg"
        videoProxyId = "proxy___${id}_transcode.mp4"

        imageProxyUri = URI("file://foo/$imageProxyId")
        videoProxyUri = URI("file://foo/$videoProxyId")

        val document = testDocument(id)

        given(mockIndexService.get(id)).willReturn(document)

        given(mockFileServerService.storedLocally).willReturn(true)
        given(mockFileServerService.objectExists(sourceFileUri)).willReturn(true)

        given(mockFileStorageService.get(imageProxyId))
            .willReturn(FileStorage(id, imageProxyUri, "file", "image/jpg", mockFileServerProvider))

        given(mockFileStorageService.get(videoProxyId))
            .willReturn(FileStorage(id, videoProxyUri, "file", "video/mp4", mockFileServerProvider))

        given(mockFileServerProvider.getServableFile(document))
            .willReturn(ServableFile(mockFileServerService, sourceFileUri))

        given(mockFileServerProvider.getServableFile(videoProxyUri))
            .willReturn(ServableFile(mockFileServerService, videoProxyUri))

        given(mockFileServerProvider.getServableFile(imageProxyUri))
            .willReturn(ServableFile(mockFileServerService, imageProxyUri))

        service = AssetStreamResolutionService(mockIndexService, mockFileServerProvider, mockFileStorageService)
    }

    /**
     * Access to source is allowed, and source is online.
     */
    @Test
    fun getServableFileAllowedSource() {
        val servableFile = service.getServableFile(id, listOf())
        assertThat(servableFile).isNotNull
        assertThat(servableFile!!.uri).isEqualTo(sourceFileUri)
    }

    @Test
    fun getServableFileForceProxyByClip() {
        val asset = mockIndexService.get(id)
        asset.setAttr("media.clip.parent", "foo")

        val servableFile = service.getServableFile(id, listOf())
        assertThat(servableFile).isNotNull
        assertThat(servableFile!!.uri).isEqualTo(videoProxyUri)
    }

    /**
     * If source doesn't exist then fallback to proxy of same overall type.
     */
    @Test
    fun getServableFileSourceNotExist() {
        given(mockFileServerService.objectExists(sourceFileUri)).willReturn(false)

        val servableFile = service.getServableFile(id, listOf())
        assertThat(servableFile).isNotNull
        assertThat(servableFile!!.uri).isEqualTo(videoProxyUri)
    }

    @Test
    fun testCanDisplaySource() {
        val asset = mockIndexService.get(id)
        assertFalse(service.canDisplaySource(asset, listOf(MediaType.parseMediaType("video/mp4"))))
        assertTrue(service.canDisplaySource(asset, listOf(MediaType.parseMediaType("video/mov"))))
        assertTrue(service.canDisplaySource(asset, listOf(MediaType.ALL)))
        assertTrue(service.canDisplaySource(asset, listOf()))
    }

    @Test
    fun testGetProxyAnyType() {
        val asset = mockIndexService.get(id)
        val proxy = service.getProxy(asset, listOf())
        assertNotNull(proxy)
        assertEquals(videoProxyUri, proxy?.uri)
    }

    @Test
    fun testGetProxyVideoType() {
        val asset = mockIndexService.get(id)
        val proxy = service.getProxy(asset, listOf(MediaType.parseMediaType("video/mp4")))
        assertNotNull(proxy)
        assertEquals(videoProxyUri, proxy?.uri)
    }

    @Test
    fun testGetProxyImageType() {
        val asset = mockIndexService.get(id)
        val proxy = service.getProxy(asset, listOf(MediaType.IMAGE_JPEG))
        assertNotNull(proxy)
        assertEquals(imageProxyUri, proxy?.uri)
    }

    private fun testDocument(id: String): Document {
        val document = Document()
        document.setAttr("source.mediaType", "video/mov")
        document.setAttr("source.type", "video")
        val proxies = mutableListOf<Proxy>()

        proxies.add(Proxy(width = 100, height = 100, id = imageProxyId, mimetype = "image/jpeg"))
        proxies.add(Proxy(width = 1920, height = 1080, id = videoProxyId, mimetype = "video/mp4"))

        val proxySchema = ProxySchema()
        proxySchema.proxies = proxies
        document.setAttr("proxies", proxySchema)
        document.setAttr("proxy_id", id)
        return document
    }
}
