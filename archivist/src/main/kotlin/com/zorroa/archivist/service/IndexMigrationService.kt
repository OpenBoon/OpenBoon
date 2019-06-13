package com.zorroa.archivist.service

import com.zorroa.archivist.domain.IndexMigrationSpec
import com.zorroa.archivist.domain.IndexRoute
import com.zorroa.archivist.domain.Organization
import com.zorroa.archivist.domain.OrganizationUpdateSpec
import com.zorroa.archivist.domain.PipelineType
import com.zorroa.archivist.domain.ProcessorRef
import com.zorroa.archivist.domain.ZpsScript
import com.zorroa.archivist.repository.IndexRouteDao
import com.zorroa.common.domain.Job
import com.zorroa.common.domain.JobPriority
import com.zorroa.common.domain.JobSpec
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

interface IndexMigrationService {

    fun migrate(org: Organization, migration: IndexMigrationSpec): Job
}

@Service
@Transactional
class IndexMigrationServiceImpl constructor(
    val indexRoutingService: IndexRoutingService,
    val jobService: JobService,
    val indexRouteDao: IndexRouteDao,
    val organizationService: OrganizationService
) : IndexMigrationService {

    override fun migrate(org: Organization, mig: IndexMigrationSpec): Job {
        val srcRoute = indexRouteDao.getOrgRoute()
        val dstRoute = indexRouteDao.get(mig.dstRouteId)
        val job =  launchMigrationJob(mig, org, srcRoute, dstRoute)

        if (mig.swapRoutes) {
            val updateOrgSpec = OrganizationUpdateSpec(org)
            updateOrgSpec.indexRouteId = mig.dstRouteId
            organizationService.update(org, updateOrgSpec)
        }

        return job
    }

    private fun launchMigrationJob(
        mig: IndexMigrationSpec,
        org: Organization,
        srcRoute: IndexRoute,
        dstRoute: IndexRoute
    ): Job {
        val name = "migration--${org.id}--${org.name}--${dstRoute.indexUrl}"
        val script = ZpsScript(
            name,
            type = PipelineType.Batch,
            settings = mutableMapOf("inline" to true),
            over = listOf(),
            execute = getProcessors(mig),
            generate = listOf(
                ProcessorRef(
                    "zplugins.core.generators.AssetSearchGenerator",
                    mapOf("search" to mapOf<String, Any>()),
                    env = mutableMapOf("ZORROA_INDEX_ROUTE_ID" to srcRoute.id.toString())
                )
            )
        )

        val spec = JobSpec(
            name,
            script,
            priority = JobPriority.Reindex,
            replace = true)

        return jobService.create(spec, PipelineType.Batch)
    }

    private fun getProcessors(mig: IndexMigrationSpec): MutableList<ProcessorRef> {

        val result = mutableListOf<ProcessorRef>()

        val args = mutableMapOf<String, Any>()
        mig.setAttrs?.let {
            args["attrs"] = it
        }
        mig.removeAttrs?.let {
            args["removeAttrs"] = it
        }

        if (args.isNotEmpty()) {
            result.add(
                ProcessorRef(
                    "zplugins.core.processors.SetAttributesProcessor", args
                )
            )
        }
        result.add(
            ProcessorRef(
                "zplugins.core.collectors.ImportCollector",
                env = mutableMapOf("ZORROA_INDEX_ROUTE_ID" to mig.dstRouteId.toString())
            )
        )

        return result
    }
}