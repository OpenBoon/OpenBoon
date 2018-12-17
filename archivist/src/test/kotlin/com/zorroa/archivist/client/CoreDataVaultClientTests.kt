package com.zorroa.archivist.client

import com.zorroa.archivist.domain.Document
import com.zorroa.common.clients.CoreDataVaultAssetSpec
import com.zorroa.common.clients.IrmCoreDataVaultClientImpl
import com.zorroa.common.util.Json
import org.junit.Ignore
import org.junit.Test
import java.nio.file.Paths
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue


/**
 * Unless you are configured with the proper service-credentials.json file, these tests
 * will not pass.
 */
@Ignore
class CoreDataVaultClientTests {

    val companyId = 25274

    val docType = "1c494ffd-940b-496a-a7d8-f0bfadc9fa24"

    val client = IrmCoreDataVaultClientImpl("https://cdvapi.dit2.ironmountainconnect.com",
            Paths.get("unittest/config/service-credentials.json"))

    @Test
    fun testAssetExists() {
        val id = UUID.randomUUID().toString()
        val spec = CoreDataVaultAssetSpec(id, docType,"test.pdf")
        val asset1 = client.createAsset(companyId, spec)
        assertTrue(client.assetExists(companyId, asset1["documentGUID"] as String))
        assertFalse(client.assetExists(companyId, "705EF325-E6E4-4DA7-AD26-C610C70261A8"))
    }

    @Test
    fun testGetDocumentTypes() {
        val res = client.getDocumentTypes(companyId)
        assertTrue(res.isNotEmpty())
        assertTrue(res[0].containsKey("documentTypeId"))
    }

    @Test
    fun testGetAsset() {
        val id = UUID.randomUUID().toString()
        val spec = CoreDataVaultAssetSpec(id, docType,"test.pdf")
        val asset1 = client.createAsset(companyId, spec)
        val asset2 = client.getAsset(companyId, asset1["documentGUID"] as String)

        assertEquals(spec.fileName, asset2["fileName"])
        assertEquals(spec.documentTypeId, asset2["documentTypeId"])
    }

    @Test
    fun testCreateAsset() {
        val id = UUID.randomUUID().toString()
        val spec = CoreDataVaultAssetSpec(id, docType,"test.pdf")
        val asset1 = client.createAsset(companyId, spec)

        println(Json.prettyString(asset1))
        assertEquals(spec.fileName, asset1["fileName"])
        assertEquals(spec.documentTypeId, asset1["documentTypeId"])
    }

    @Test
    fun testUpdateAsset() {
        val id = UUID.randomUUID()
        val asset1 = client.createAsset(companyId,
                CoreDataVaultAssetSpec(id, docType, "test.pdf")).toMutableMap()
        asset1["fileName"] = "bob.pdf"
        val result = client.updateAsset(companyId, asset1)
        assertEquals("bob.pdf", result["fileName"])
    }

    @Test(expected = com.zorroa.common.clients.RestClientException::class)
    fun testUpdateAssetDoesNotExist() {
        client.updateAsset(companyId,
                mapOf("documentGUID" to "077F5AB7-614F-4E76-AF4A-B37DA23DCB9E",
                        "fileName" to "bob.jpg"))
    }

    @Test
    fun testGetIndexedMetadata() {
        val id = UUID.randomUUID()
        val asset1 = client.createAsset(companyId,
                CoreDataVaultAssetSpec(id, docType, "test.pdf")).toMutableMap()
        val doc = Document(asset1["documentGUID"] as String)
        doc.setAttr("foo", "bar")
        assertTrue(client.updateIndexedMetadata(companyId, doc))
        val doc2 = client.getIndexedMetadata(companyId, doc.id)
        assertEquals(doc.id, doc2.id)
        assertEquals(doc.getAttr("foo", String::class.java), doc2.getAttr("foo", String::class.java))
    }

    @Test
    fun testUpdateIndexedMetadata() {
        val id = UUID.randomUUID()
        val asset1 = client.createAsset(companyId,
                CoreDataVaultAssetSpec(id, docType, "test.pdf")).toMutableMap()
        val doc = Document(asset1["documentGUID"] as String)
        doc.setAttr("foo", "bar")
        assertTrue(client.updateIndexedMetadata(companyId, doc))
    }

    @Test
    fun testUpdateESMetadataDoesNotExist() {
        val doc = Document("077F5AB7-614F-4E76-AF4A-B37DA23DCB9E")
        val res = client.updateIndexedMetadata(companyId, doc)
        assertFalse(res)
    }

    @Test
    fun testBatchUpdateIndexedMetdata() {
        val id = UUID.randomUUID()
        val asset1 = client.createAsset(companyId,
                CoreDataVaultAssetSpec(id, docType, "test.pdf")).toMutableMap()

        val doc = Document(asset1["documentGUID"] as String)
        doc.setAttr("foo", "bar")

        val result = client.batchUpdateIndexedMetadata(companyId, listOf(doc))
        assertTrue(result[doc.id] ?: false)
    }

    @Test(expected=com.zorroa.common.clients.RestClientException::class)
    fun testDelete() {
        val id = UUID.randomUUID()
        val asset1 = client.createAsset(companyId,
                CoreDataVaultAssetSpec(id, docType, "test.pdf")).toMutableMap()
        val assetId =  asset1["documentGUID"] as String
        assertTrue(client.delete(companyId, assetId))
        client.getAsset(companyId, assetId)
    }

    @Test
    fun testBatchDelete() {
        val id = UUID.randomUUID()
        val asset1 = client.createAsset(companyId,
                CoreDataVaultAssetSpec(id, docType, "test.pdf")).toMutableMap()
        val assetId =  asset1["documentGUID"] as String
        val result = client.batchDelete(companyId, listOf(assetId))
        assertTrue(result[assetId] ?: false)
    }
}