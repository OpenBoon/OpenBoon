package com.zorroa.archivist.service

import com.google.common.collect.Lists
import com.zorroa.archivist.domain.Document
import com.zorroa.archivist.domain.FileStorage
import com.zorroa.common.schema.Proxy
import com.zorroa.common.schema.ProxySchema
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import java.net.URI
import java.util.*

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

    @Test
    fun getServableFileDirectlyFromFileServerService() {
        val servableFile = service.getServableFile(id)
        assertThat(servableFile).isNotNull
        assertThat(servableFile!!.uri).isEqualTo(sourceFileUri)
    }

    @Test
    fun getServableFileWithAcceptVideo_GetsVideoProxy() {
        val acceptHeader = "video/webm,video/ogg,video/*;q=0.9,application/ogg;q=0.7,audio/*;q=0.6,*/*;q=0.5"
        val servableFile = service.getServableFile(id, acceptHeader)

        assertThat(servableFile).isNotNull
        assertThat(servableFile!!.uri).isEqualTo(videoProxyUri)
    }

    @Test
    fun getServableFileWithAcceptOther_GetsSourceAsset() {
        val acceptHeader = "application/other;q=0.9,application/ogg;q=0.7,audio/*;q=0.6,*/*;q=0.5"
        val servableFile = service.getServableFile(id, acceptHeader)

        assertThat(servableFile).isNotNull
        assertThat(servableFile!!.uri).isEqualTo(sourceFileUri)
    }

    @Test
    fun getServableFileWithBadType_ReturnsNull() {
        val acceptHeader = null
        val type = "foo"
        val servableFile = service.getServableFile(id, acceptHeader, type)

        assertThat(servableFile).isNull()
    }

    @Test
    fun requestedType_ReturnsVideo() {
        val acceptHeader = "video/webm,video/ogg,video/*;q=0.9,application/ogg;q=0.7,audio/*;q=0.6,*/*;q=0.5"
        val type = null
        Assertions.assertThat(service.requestedType(acceptHeader, type)).isEqualTo("video")
    }

    @Test
    fun requestedType_PrioritizesAcceptHeader() {
        val acceptHeader = "video/webm,video/ogg,video/*;q=0.9,application/ogg;q=0.7,audio/*;q=0.6,*/*;q=0.5"
        val type = "image"
        Assertions.assertThat(service.requestedType(acceptHeader, type)).isEqualTo("video")
    }

    @Test
    fun requestedTypeWithoutAccept_ReturnsTypeParameter() {
        val acceptHeader = null
        val type = "foo"
        Assertions.assertThat(service.requestedType(acceptHeader, type)).isEqualTo("foo")
    }

    @Test
    fun requestedType_ShouldReturnNullWhenAcceptIsNotValidForProxy() {
        var acceptHeader:String? = "application/json, text/html"
        var type: String? = null
        Assertions.assertThat(service.requestedType(acceptHeader, type)).isNull()
    }

    @Test
    fun requestedType_ShouldReturnVideoIfAnywhereInAcceptHeader() {
        val acceptHeader = "application/json, text/html, video/mp4"
        val type = null
        Assertions.assertThat(service.requestedType(acceptHeader, type)).isEqualTo("video")
    }

    @Test
    fun requestedType_ShouldReturnImageIfAnywhereInAcceptHeader() {
        val acceptHeader = "application/json, text/html, image/jpg"
        val type = null
        Assertions.assertThat(service.requestedType(acceptHeader, type)).isEqualTo("image")
    }

    @Test
    fun requestedType_ShouldReturnFirstTypeFoundInAcceptHeader() {
        val type = null
        var acceptHeader = "application/json,video/webm,text/html,image/jpg"
        Assertions.assertThat(service.requestedType(acceptHeader, type)).isEqualTo("video")

        acceptHeader = "application/json,image/png,video/webm,text/html,image/jpg"
        Assertions.assertThat(service.requestedType(acceptHeader, type)).isEqualTo("image")
    }


    private fun testDocument(id: String): Document {
        val document = Document()
        val proxies = Lists.newArrayList<Proxy>()

        proxies.add(Proxy(width = 100, height = 100, id = imageProxyId, mimetype = "image/jpeg"))
        proxies.add(Proxy(width = 1920, height = 1080, id = videoProxyId, mimetype = "video/mp4"))

        val proxySchema = ProxySchema()
        proxySchema.proxies = proxies
        document.setAttr("proxies", proxySchema)
        document.setAttr("proxy_id", id)
        return document
    }


}
