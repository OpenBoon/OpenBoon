package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import org.junit.Before
import org.junit.Test

class AssetServiceTests : AbstractTest() {

    override fun requiresElasticSearch(): Boolean {
        return true
    }

    override fun requiresFieldSets(): Boolean {
        return true
    }

    @Before
    fun init() {
        addTestAssets("set04/standard")
    }

    fun testBatchCreateOrReplace() {
    }

    @Test
    fun testGet() {
    }
}
