package com.zorroa.archivist.repository

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.Document
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AssetDaoTests : AbstractTest() {

    @Autowired
    lateinit var assetDao: AssetDao


    @Test
    fun testCreateOrReplace() {
        val doc = Document(UUID.randomUUID())
        doc.setAttr("foo", "bar")
        assetDao.createOrReplace(doc)
    }

    @Test
    fun testBatchCreateOrReplace() {
        val docs = mutableListOf<Document>()
        for (i in 1 .. 10) {
            val doc = Document(UUID.randomUUID())
            doc.setAttr("foo", "bar")
            docs.add(doc)
        }

        val res = assetDao.batchCreateOrReplace(docs)
        assertEquals(10, res)
    }

    @Test
    fun testGet() {
        val doc = Document(UUID.randomUUID())
        doc.setAttr("foo", "bar")
        assetDao.createOrReplace(doc)

        val doc2 = assetDao.get(doc.id)
        assertEquals(doc.id, doc2.id)
        assertEquals(doc.getAttr("foo", String::class.java), doc2.getAttr("foo", String::class.java))
    }

    @Test
    fun testGetMap() {
        val docs = mutableListOf<Document>()
        for (i in 1 .. 10) {
            val doc = Document(UUID.randomUUID())
            doc.setAttr("foo", "bar")
            docs.add(doc)
        }
        assetDao.batchCreateOrReplace(docs)
        val map = assetDao.getMap(docs.map{it.id})
        assertEquals(10, map.size)
        docs.forEach {
            assertTrue(it.id in map.keys)
        }
    }
}