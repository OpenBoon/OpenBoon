package com.zorroa.analyst.service

import com.zorroa.analyst.AbstractTest
import com.zorroa.common.domain.Asset
import com.zorroa.common.domain.Document
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.web.WebAppConfiguration
import java.util.*
import kotlin.test.assertNull

@WebAppConfiguration
class AssetServiceTests : AbstractTest() {

    @Autowired
    lateinit var assetService: AssetService

    @Test
    fun testRemoveIllegalNamespaces() {
        val asset = Asset(UUID.randomUUID(), UUID.randomUUID(), mapOf())
        val document = Document(asset.id.toString())
        assetService.removeIllegalNamespaces(document)
        assertNull(document.getAttr("tmp"))
        assertNull(document.getAttr("zorroa"))
    }
}