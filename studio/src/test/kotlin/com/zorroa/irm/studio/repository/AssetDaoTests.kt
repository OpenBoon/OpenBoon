package com.zorroa.irm.studio.repository

import com.zorroa.common.domain.Document
import com.zorroa.irm.studio.AbstractTest
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AssetDaoTests : AbstractTest() {

    @Autowired
    lateinit var assetDao: AssetDao<CDVAssetSpec>

    @Test
    fun testCreateAndGet() {
        val spec = CDVAssetSpec(
                100,
                UUID.randomUUID().toString() + ".jpg")
        val result = assetDao.create(spec)
        assertEquals("100", result.organizationName)
        assertNotNull(result.id)
    }

    @Test
    fun testGetAndUpdateDocument() {
        val orgId = UUID.randomUUID()
        val spec = CDVAssetSpec(
                100,
                UUID.randomUUID().toString() + ".jpg")
        val asset = assetDao.create(spec)
        val doc = Document(asset.id.toString())
        doc.setAttr("foo.bar","over 9000")

        val updateResult = assetDao.updateDocument(orgId, asset.id, doc)
        val storedDoc = assetDao.getDocument(orgId, asset.id)
        assertEquals(doc.document, storedDoc.document)
        assertEquals(doc.id, storedDoc.id)
    }
}
