package com.zorroa.archivist

import com.zorroa.common.util.JdbcUtils.arrayOverlapClause
import com.zorroa.common.util.JdbcUtils.getTsWordVector
import org.junit.Test
import kotlin.test.assertEquals

class JdbcUtilsTest {

    @Test
    fun testGetTsWordVector() {
        val str = "bing-bong-foo.bar.bing.MasterGenerator"
        assertEquals("bing bong foo bar bing master generator", getTsWordVector(str))
    }

    @Test
    fun testArrayOverlapClause() {
        val clause = arrayOverlapClause("list_names", "text", 2)
        assertEquals("list_names && ARRAY[?,?]::text[]", clause)
        assertEquals("", arrayOverlapClause("list_names", "text", 0))
    }
}