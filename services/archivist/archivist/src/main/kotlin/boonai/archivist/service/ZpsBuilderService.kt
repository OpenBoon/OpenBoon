package boonai.archivist.service

import boonai.archivist.domain.BuildZpsScriptRequest
import boonai.archivist.domain.Asset
import boonai.archivist.domain.ZpsScript
import boonai.archivist.security.getProjectId
import org.springframework.stereotype.Service

/**
 * A service for building ZPS Scripts which contain pipeline and an asset.,
 */
interface ZpsBuilderService {

    /**
     * Creates a ZPS script for a
     */
    fun buildZpsScript(req: BuildZpsScriptRequest): ZpsScript
}

@Service
class ZpsBuilderServiceImpl constructor(
    val assetService: AssetService,
    val pipelineResolverService: PipelineResolverService,
) : ZpsBuilderService {

    override fun buildZpsScript(req: BuildZpsScriptRequest): ZpsScript {
        val pipeline = pipelineResolverService.resolveModular(req.modules, includeStandard = false)

        // The asset can be null, and if so then we use an empty asset.
        val asset = if (req.assetId != null) {
            assetService.getAsset(req.assetId)
        } else {
            val a = Asset(id = "TRANSIENT")
            a.setAttr("system.projectId", getProjectId().toString())
            a
        }
        return ZpsScript(null, null, assets = listOf(asset), execute = pipeline.execute)
    }
}
