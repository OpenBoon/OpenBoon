package com.zorroa.archivist.schema

import com.zorroa.common.schema.Proxy
import com.zorroa.common.schema.ProxySchema
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Created by chambers on 2/26/16.
 */
class ProxySchemaTests {

    private fun defaultProxySchema(): ProxySchema {
        val proxies = mutableListOf<Proxy>()
        proxies.add(Proxy("1", 100, 100))
        proxies.add(Proxy("2", 60, 50))
        proxies.add(Proxy("3", 400, 500))
        proxies.add(Proxy("4", 20, 10))

        val p = ProxySchema()
        p.proxies = proxies
        return p
    }

    @Test
    fun testAtLeastThisSize() {
        val p = defaultProxySchema()
        assertNull(p.atLeastThisSize(1000))
        assertEquals(100, p.atLeastThisSize(100)!!.width)
        assertEquals(60, p.atLeastThisSize(51)!!.width)
        assertEquals(20, p.atLeastThisSize(5)!!.width)
    }

    @Test
    fun testThisSizeOrBelow() {
        val p = defaultProxySchema()
        assertEquals(400, p.thisSizeOrBelow(1000)!!.width)
        assertEquals(400, p.thisSizeOrBelow(400)!!.width)
        assertEquals(100, p.thisSizeOrBelow(399)!!.width)
        assertNull(p.thisSizeOrBelow(9))
    }

    @Test
    fun testGetSmallest() {
        val p = defaultProxySchema()
        assertEquals(20, p.getSmallest()!!.width)
    }

    @Test
    fun testGetLargest() {
        val p = defaultProxySchema()
        assertEquals(400, p.getLargest()!!.width)
    }

    @Test
    fun getMixedType() {
        val proxies = mutableListOf<Proxy>()
        proxies.add(Proxy("1", 100, 100))
        proxies.add(Proxy("2", 200,200))
        proxies.add(Proxy("3", 100, 100, "video/mp4"))
        proxies.add(Proxy("4", 200,200, "video/mp4"))

        val p = ProxySchema()
        p.proxies = proxies

        assertEquals("image/jpeg", p.getClosest(100, 100)!!.mimeType)
        assertEquals("image/jpeg", p.getClosest(149, 150)!!.mimeType)
        assertEquals("image/jpeg", p.getClosest(151, 150)!!.mimeType)
        assertEquals("image/jpeg", p.getClosest(1000, 1000)!!.mimeType)

        assertEquals("video/mp4", p.getClosest(100, 100, "video/mp4")!!.mimeType)
        assertEquals("video/mp4", p.getClosest(149, 150,"video/mp4")!!.mimeType)
        assertEquals("video/mp4", p.getClosest(151, 150,"video/mp4")!!.mimeType)
        assertEquals("video/mp4", p.getClosest(1000, 1000,"video/mp4")!!.mimeType)
    }

    @Test
    fun getClosest() {
        val proxies = mutableListOf<Proxy>()
        proxies.add(Proxy("1", 100, 100))
        proxies.add(Proxy("2", 200,200))

        val p = ProxySchema()
        p.proxies = proxies

        assertEquals(100, p.getClosest(100, 100)!!.width)
        assertEquals(100, p.getClosest(149, 150)!!.width)
        assertEquals(200, p.getClosest(151, 150)!!.width)
        assertEquals(200, p.getClosest(1000, 1000)!!.width)
    }

    @Test
    fun getClosest2() {
        val proxies = mutableListOf<Proxy>()
        proxies.add(Proxy("1",128, 128))
        proxies.add(Proxy("2",640, 400))


        val p = ProxySchema()
        p.proxies = proxies

        assertEquals(128, p.getClosest(256, 256)!!.width)
        assertEquals(640, p.getClosest(400, 400)!!.width)

    }
}
