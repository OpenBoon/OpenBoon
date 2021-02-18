package boonai.archivist.domain

import org.junit.Test
import kotlin.test.assertEquals

class ZpsTests {

    @Test
    fun testSetSetting() {
        val zps = ZpsScript("foo", null, null, null)
        zps.setSettting("bilbo", "baggins")
        assertEquals("baggins", zps.settings!!["bilbo"])
    }

    @Test
    fun testSetGlobalArg() {
        val zps = ZpsScript("foo", null, null, null)
        zps.setGlobalArg("bilbo", "baggins")
        assertEquals("baggins", zps.globalArgs!!["bilbo"])
    }
}
