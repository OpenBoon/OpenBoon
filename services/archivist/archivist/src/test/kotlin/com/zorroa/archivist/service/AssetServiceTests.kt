package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.AssetSpec
import com.zorroa.archivist.domain.BatchProvisionAssetsRequest
import com.zorroa.archivist.domain.IdGen
import com.zorroa.archivist.util.Json
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class AssetServiceTests : AbstractTest() {

    override fun requiresElasticSearch(): Boolean {
        return true
    }

    override fun requiresFieldSets(): Boolean {
        return true
    }

    @Test
    fun testBatchProvisionGcsAssets() {
        val req = BatchProvisionAssetsRequest(
            assets = listOf(AssetSpec("gs://cats/large-brown-cat.jpg")))

        val response = assetService.batchProvisionAssets(req)
        assertEquals(1, response.status.size)
        assertEquals(1, response.assets.size)

        val asset = response.assets[0]
        logger.info(Json.prettyString(asset))
    }
}
