package boonai.archivist.service

import boonai.archivist.AbstractTest
import boonai.archivist.domain.ApplyModulesToAssetRequest
import boonai.archivist.domain.AssetSpec
import boonai.archivist.domain.BatchCreateAssetsRequest
import junit.framework.Assert.assertEquals
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired

class ZpsBuilderServiceTests : AbstractTest() {

    @Autowired
    lateinit var pipelineModuleService: PipelineModService

    @Autowired
    lateinit var zpsBuilderService: ZpsBuilderService

    @Test
    fun testBuildApplyModulesToAssetScript() {
        pipelineModuleService.updateStandardMods()

        val spec = AssetSpec("gs://cats/large-brown-cat.jpg")
        val create = BatchCreateAssetsRequest(
            assets = listOf(spec)
        )

        refreshElastic()
        var asset = assetService.getAsset(assetService.batchCreate(create).created[0])
        val req = ApplyModulesToAssetRequest(
            asset.id,
            modules = listOf("boonai-face-detection")
        )

        val zps = zpsBuilderService.buildApplyModulesToAssetScript(req)
        assertEquals(zps.assets?.get(0)?.id, asset.id)
        assertEquals(zps.execute?.get(0)?.className, "boonai_analysis.boonai.ZviFaceDetectionProcessor")
    }
}
