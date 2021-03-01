package boonai.archivist

import boonai.archivist.util.JdbcUtils.arrayOverlapClause
import boonai.archivist.util.JdbcUtils.getTsWordVector
import boonai.archivist.util.JdbcUtils.insert
import org.junit.Test
import kotlin.test.assertEquals

class JdbcUtilsTest {

    @Test
    fun testGetTsWordVector() {
        val str = "bing-bong-foo.bar.bing.MasterGenerator"
        assertEquals("bing bong foo bar bing master generator", getTsWordVector(str))
    }

    @Test
    fun testGetTsWordVectorMultiple() {
        assertEquals(
            "master generator foo bar image jpg",
            getTsWordVector("MasterGenerator", "/foo/bar/image.jpg")
        )
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
