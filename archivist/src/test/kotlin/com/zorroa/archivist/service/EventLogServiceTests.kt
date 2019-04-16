package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.search.AssetFilter
import com.zorroa.archivist.search.AssetSearch
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals

class EventLogServiceTests : AbstractTest() {

    @Autowired
    lateinit var meterRegistry: MeterRegistry

    /**
     * Test that we're getting metrics on the searches we're doing. We're not
     * going to test every possibility here, just that the concept works.
     */
    @Test
    fun testApplyAssetSearchMetricsFilter() {
        val counter = meterRegistry.counter("zorroa.asset.search.filter",
                listOf(Tag.of("type", "exists")))
        val currentValue = counter.count()

        val search = AssetSearch()
        search.filter = AssetFilter()
        search.filter.addToExists("foo")

        applyAssetSearchMetrics(search)
        assertEquals(currentValue + 1, counter.count())
    }
}