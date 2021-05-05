package boonai.archivist.service

import boonai.archivist.domain.ApplyModulesToAssetRequest
import boonai.archivist.domain.ZpsScript
import org.springframework.stereotype.Service

/**
 * A service for building ZPS Scripts which contain pipeline and an asset.,
 */
interface ZpsBuilderService {

    /**
     * Creates a ZPS script for a
     */
    fun buildApplyModulesToAssetScript(req: ApplyModulesToAssetRequest): ZpsScript
}

@Service
class ZpsBuilderServiceImpl constructor(
    val assetService: AssetService,
    val pipelineResolverService: PipelineResolverService,
) : ZpsBuilderService {

    override fun buildApplyModulesToAssetScript(req: ApplyModulesToAssetRequest): ZpsScript {
        val pipeline = pipelineResolverService.resolveModular(req.modules, includeStandard = false)
        val asset = assetService.getAsset(req.assetId)
        return ZpsScript(null, null, assets = listOf(asset), execute = pipeline.execute)
    }
}
