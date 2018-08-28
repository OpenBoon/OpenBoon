package com.zorroa.archivist.service

import com.zorroa.common.clients.AnalystClient
import com.zorroa.common.domain.Job
import com.zorroa.common.domain.JobSpec
import com.zorroa.common.server.NetworkEnvironment
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

interface JobService {
    fun launchJob(spec: JobSpec) : Job
}

@Component
class JobServiceImpl @Autowired constructor(
        private val analystClient: AnalystClient,
        private val networkEnvironment: NetworkEnvironment
): JobService {


    override fun launchJob(spec: JobSpec) : Job {
        // A centralized place to outfit each job with the env it needs
        spec.env.putAll(mapOf(
                "ZORROA_SUPER_ADMIN" to "true",
                "ZORROA_ARCHIVIST_URL" to networkEnvironment.getPublicUrl("zorroa-archivist"),
                "ZORROA_ORGANIZATION_ID" to spec.organizationId.toString()))
        return analystClient.createJob(spec)
    }
}
