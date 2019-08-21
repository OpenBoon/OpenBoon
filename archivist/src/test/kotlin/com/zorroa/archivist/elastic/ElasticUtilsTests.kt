package com.zorroa.archivist.elastic

import org.elasticsearch.index.query.TermsQueryBuilder
import org.junit.Test
import kotlin.test.assertTrue

class ElasticUtilsTests {

    @Test
    fun testParse() {
        val query = """{"query": { "terms": {"source.type": ["video"]}}}"""
        val qb = ElasticQueryParser.parse(query)
        assertTrue(qb is TermsQueryBuilder)
    }
}