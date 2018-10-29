package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.Document
import com.zorroa.archivist.search.AssetSearch
import org.junit.Before
import org.junit.Test
import kotlin.test.assertTrue

class KSearchServiceTests : AbstractTest() {

    @Before
    fun init() {
        addTestAssets("set04/standard")
        refreshIndex()
    }

    @Test
    fun testScanAndScrollWithFunction() {
        var files = mutableListOf<String?>()

        searchService.scanAndScroll(AssetSearch(), true) {
            it.hits.forEach { source ->
                val doc = Document(source.sourceAsMap)
                files.add(doc.getAttr("source.filename"))
            }
        }

        assertTrue("beer_kettle_01.jpg" in files)
        assertTrue("new_zealand_wellington_harbour.jpg" in files)
    }
}