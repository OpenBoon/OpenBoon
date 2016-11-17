package com.zorroa.archivist.web.cluster;

import com.google.common.collect.ImmutableList;
import com.zorroa.archivist.service.JobExecutorService;
import com.zorroa.archivist.service.JobService;
import com.zorroa.common.domain.*;
import com.zorroa.common.repository.ClusterSettingsDao;
import com.zorroa.sdk.util.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Endpoints for analyst cluster communication.
 */
@RestController
public class ClusterController {

    private static final Logger logger = LoggerFactory.getLogger(ClusterController.class);

    @Autowired
    ClusterSettingsDao clusterSettingsDao;

    @Autowired
    JobExecutorService jobExecutorService;

    @Autowired
    JobService jobService;

    @RequestMapping(value="/cluster/v1/settings", method= RequestMethod.GET)
    public Map<String, Object> getConfig() {
        return clusterSettingsDao.get();
    }

    /**
     * Process a response
     *
     * @return
     */
    @RequestMapping(value="/cluster/v1/task/_response", method=RequestMethod.POST)
    public void respond(@RequestBody ExecuteTaskResponse response) {
        jobExecutorService.handleResponse(response);
    }

    /**
     * Increment task stats
     *
     * @return
     */
    @RequestMapping(value="/cluster/v1/task/_stats", method=RequestMethod.POST)
    public void incrementStats(@RequestBody ExecuteTaskStats stats) {
        logger.info("stats: {}", Json.prettyString(stats));
        jobService.incrementJobStats(stats.getTask().getJobId(), stats.getSuccessCount(),
                stats.getErrorCount(), stats.getWarningCount());
        jobService.incrementTaskStats(stats.getTask().getTaskId(), stats.getSuccessCount(),
                stats.getErrorCount(), stats.getWarningCount());
    }

    /**
     * Process a reaction.
     *
     * @return
     */
    @RequestMapping(value="/cluster/v1/task/_expand", method=RequestMethod.POST)
    public void expand(@RequestBody ExecuteTaskExpand expand) {
        jobService.expand(expand);
    }

    /**
     * Task was started.
     *
     * @return
     */
    @RequestMapping(value="/cluster/v1/task/_running", method=RequestMethod.POST)
    public void running(@RequestBody ExecuteTaskStart task) {
        jobService.setTaskState(task.getTask(), TaskState.Running, TaskState.Queued);
    }

    /**
     * Task was completed.
     *
     * @return
     */
    @RequestMapping(value="/cluster/v1/task/_stopped", method=RequestMethod.POST)
    public void stopped(@RequestBody ExecuteTaskStopped result) {
        jobService.setTaskCompleted(result);
    }

    /**
     * Task was completed.
     *
     * @return
     */
    @RequestMapping(value="/cluster/v1/task/_queue", method=RequestMethod.POST)
    public List<ExecuteTaskStart> queue(@RequestBody ExecuteTaskRequest req) {
        try {
            return jobExecutorService.queueWaitingTasks(req).get();
        } catch (Exception e) {
            return ImmutableList.of();
        }
    }

    /**
     * Task was completed.
     *
     * @return
     */
    @RequestMapping(value="/cluster/v1/task/_reject", method=RequestMethod.POST)
    public void reject(@RequestBody ExecuteTaskStopped result) {
        jobService.setTaskState(result, TaskState.Waiting, TaskState.Queued);
    }
}
