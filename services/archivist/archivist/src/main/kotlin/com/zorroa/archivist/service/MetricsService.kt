package com.zorroa.archivist.service

import com.zorroa.zmlp.service.logging.MeterRegistryHolder
import io.micrometer.core.instrument.Gauge
import org.springframework.stereotype.Component

interface MetricsService {
    fun initMicrometerGauges()
}

@Component
class MetricsServiceImpl(
    val dispatcherService: DispatcherService
) : MetricsService {

    init {
        initMicrometerGauges()
    }

    override fun initMicrometerGauges() {
        initPendingTasksStatsGauges()
    }

    private fun initPendingTasksStatsGauges() {
        Gauge
            .builder("tasks.pending") {
                dispatcherService.getPendingTasksStats().pendingTasks
            }
            .register(MeterRegistryHolder.meterRegistry)
        Gauge
            .builder("tasks.running") {
                dispatcherService.getPendingTasksStats().runningTasks
            }
            .register(MeterRegistryHolder.meterRegistry)

        Gauge
            .builder("tasks.active") {
                val stats = dispatcherService.getPendingTasksStats()
                stats.runningTasks + stats.pendingTasks
            }
            .register(MeterRegistryHolder.meterRegistry)

        Gauge
            .builder("tasks.max_running") {
                dispatcherService.getPendingTasksStats().maxRunningTasks
            }
            .register(MeterRegistryHolder.meterRegistry)
    }
}
