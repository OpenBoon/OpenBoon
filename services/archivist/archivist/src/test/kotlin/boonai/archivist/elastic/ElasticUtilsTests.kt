package boonai.archivist.elastic

import boonai.archivist.util.ElasticSearchErrorTranslator
import boonai.archivist.util.ElasticUtils
import org.elasticsearch.index.query.TermsQueryBuilder
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ElasticUtilsTests {

    @Test
    fun testParse() {
        val query =
            """{"query": { "terms": {"source.type": ["video"]}}}"""
        val qb = ElasticUtils.parse(query)
        assertTrue(qb is TermsQueryBuilder)
    }

    @Test
    fun testElasticSearchErrorTranslator() {
        assertEquals(
            "asset already exists",
            ElasticSearchErrorTranslator.translate("blah blah document already exists blah blah")
        )
        assertEquals(
            "field 'foo.bar' is not allowed, the wrong data type, or format",
            ElasticSearchErrorTranslator.translate("reason=failed to parse [foo.bar]")
        )
        assertEquals(
            "field 'foo.bar' is not allowed, the wrong data type, or format",
            ElasticSearchErrorTranslator.translate("\"term in field=\"foo.bar\"\"")
        )
        assertEquals(
            "field 'foo.bar' is not allowed, the wrong data type, or format",
            ElasticSearchErrorTranslator.translate("mapper [foo.bar] of different type")
        )
        assertTrue(
            "Untranslatable asset error" in
                ElasticSearchErrorTranslator.translate("kirk, spock, and bones")
        )
    }
}
