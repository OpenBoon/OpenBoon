package com.zorroa.common.clients

import com.zorroa.common.domain.Job
import com.zorroa.common.domain.JobSpec
import org.slf4j.LoggerFactory

interface AnalystClient {
    fun createJob(spec: JobSpec) : Job
}

class AnalystClientImpl(val url: String, jwtSigner: JwtSigner?) : AnalystClient {

    val client = RestClient(url, jwtSigner)

    init {
        logger.info("Initializing Analyst REST client: {}", url)
    }

    override fun createJob(spec: JobSpec) : Job {
        return client.post("/api/v1/jobs", spec, Job::class.java)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AnalystClient::class.java)
    }
}
