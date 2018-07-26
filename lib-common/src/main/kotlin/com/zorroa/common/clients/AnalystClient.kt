package com.zorroa.common.clients

import com.zorroa.common.domain.Job
import com.zorroa.common.domain.JobSpec

interface AnalystClient {
    fun createJob(spec: JobSpec) : Job
}

class AnalystClientImpl(val url: String) : AnalystClient {

    val client = RestClient(url)

    override fun createJob(spec: JobSpec) : Job {
        return client.post("/api/v1/jobs", spec, Job::class.java)
    }
}
