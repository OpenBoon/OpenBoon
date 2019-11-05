package com.zorroa.archivist.service

import com.zorroa.archivist.domain.IndexMigrationSpec
import com.zorroa.archivist.domain.IndexRoute
import com.zorroa.archivist.domain.Job
import com.zorroa.archivist.domain.JobPriority
import com.zorroa.archivist.domain.JobSpec
import com.zorroa.archivist.domain.JobType
import com.zorroa.archivist.domain.ProcessorRef
import com.zorroa.archivist.domain.ZpsScript
import com.zorroa.archivist.repository.IndexRouteDao
import com.zorroa.archivist.security.getApiKey
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

interface IndexMigrationService {

    fun migrate(migration: IndexMigrationSpec): Job
}

@Service
@Transactional
class IndexMigrationServiceImpl constructor(
    val indexRoutingService: IndexRoutingService,
    val jobService: JobService,
    val indexRouteDao: IndexRouteDao
) : IndexMigrationService {

    override fun migrate(mig: IndexMigrationSpec): Job {
        val srcRoute = indexRouteDao.getProjectRoute()
        val dstRoute = indexRouteDao.get(mig.dstRouteId)
        val job = launchMigrationJob(mig, srcRoute, dstRoute)

        // TODO: implement swap routes if still needed
        return job
    }

    private fun launchMigrationJob(
        mig: IndexMigrationSpec,
        srcRoute: IndexRoute,
        dstRoute: IndexRoute
    ): Job {
        val apiKey = getApiKey()
        val name = "migration--${apiKey.projectId}-${dstRoute.indexUrl}"
        val script = ZpsScript(
            name,
            type = JobType.Import,
            settings = mutableMapOf("inline" to true),
            over = listOf(),
            execute = getProcessors(mig),
            generate = listOf(
                ProcessorRef(
                    "zplugins.core.generators.AssetSearchGenerator",
                    "zorroa-py3-core",
                    mapOf("search" to mapOf<String, Any>()),
                    env = mutableMapOf("ZORROA_INDEX_ROUTE_ID" to srcRoute.id.toString())
                )
            )
        )

        val spec = JobSpec(
            name,
            script,
            priority = JobPriority.Reindex,
            replace = true
        )

        return jobService.create(spec, JobType.Batch)
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
                    "zplugins.core.processors.SetAttributesProcessor",
                    "zorroa-py3-core", args
                )
            )
        }

        return result
    }
}