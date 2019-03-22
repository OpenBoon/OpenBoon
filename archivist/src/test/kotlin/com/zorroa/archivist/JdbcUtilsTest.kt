package com.zorroa.archivist

import com.zorroa.common.util.JdbcUtils.arrayOverlapClause
import com.zorroa.common.util.JdbcUtils.getTsWordVector
import com.zorroa.common.util.JdbcUtils.insert
import org.junit.Test
import kotlin.test.assertEquals

class JdbcUtilsTest {

    @Test
    fun testGetTsWordVector() {
        val str = "bing-bong-foo.bar.bing.MasterGenerator"
        assertEquals("bing bong foo bar bing master generator", getTsWordVector(str))
    }

    @Test
    fun testInsertWithFunction() {
        val query = insert("table1", "words@to_tsvector")
        assertEquals("INSERT INTO table1(words) VALUES (to_tsvector(?))", query)
    }

    @Test
    fun testInsertWithCast() {
        val query = insert("table1", "words::jsonb")
        assertEquals("INSERT INTO table1(words) VALUES (?::jsonb)", query)
    }

    @Test
    fun testArrayOverlapClause() {
        val clause = arrayOverlapClause("list_names", "text", 2)
        assertEquals("list_names && ARRAY[?,?]::text[]", clause)
        assertEquals("", arrayOverlapClause("list_names", "text", 0))
    }
}