package com.zorroa.analyst.scheduler

import com.zorroa.analyst.service.SchedulerService
import com.zorroa.common.domain.Job

class LocalSchedulerServiceImpl: SchedulerService {
    override fun kill(job: Job): Boolean {
        return true
    }

    override fun retry(job: Job): Boolean {
        return true
    }

    override fun schedule() {

    }

    override fun runJob(job: Job): Boolean {
        return true
    }
}
