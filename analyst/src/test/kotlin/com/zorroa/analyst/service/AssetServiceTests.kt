package com.zorroa.analyst.service

import com.zorroa.analyst.repository.IndexDao
import com.zorroa.common.domain.Asset
import com.zorroa.common.domain.Document
import com.zorroa.common.service.CoreDataVaultService
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import java.util.*
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(MockitoJUnitRunner::class)
class AssetServiceTests {

    private val asset = Asset(UUID.randomUUID(), UUID.randomUUID(), mutableMapOf("tmp.download_url" to "https://fake.com/file"))
    private val document = Document(asset.id.toString())

    @InjectMocks
    private lateinit var assetService: AssetServiceImpl

    @Mock
    lateinit var indexDao: IndexDao

    @Mock
    lateinit var cdvService: CoreDataVaultService

    @Test
    fun testRemoveIllegalNamespaces() {
        assetService.removeIllegalNamespaces(document)
        assertNull(document.getAttr("tmp"))
    }

    @Test
    fun storeAndReindex() {
        val result = assetService.storeAndReindex(asset, document)
        assertTrue(result.status["stored"] as Boolean)
        assertTrue(result.status["indexed"] as Boolean)
        assertNull(document.getAttr("tmp"))
    }
}