package com.zorroa.archivist.web.cluster;

import com.zorroa.archivist.domain.TaskState;
import com.zorroa.archivist.service.JobExecutorService;
import com.zorroa.archivist.service.JobService;
import com.zorroa.common.repository.ClusterSettingsDao;
import com.zorroa.sdk.util.Json;
import com.zorroa.sdk.zps.ZpsResult;
import com.zorroa.sdk.zps.ZpsScript;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

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
     * Process a reaction.
     *
     * @return
     */
    @RequestMapping(value="/cluster/v1/task/_expand", method=RequestMethod.POST)
    public void expand(@RequestBody ZpsScript script) {
        jobExecutorService.expand(script);
    }

    /**
     * Task was started.
     *
     * @return
     */
    @RequestMapping(value="/cluster/v1/task/_running", method=RequestMethod.POST)
    public void running(@RequestBody ZpsScript task) {
        jobService.setTaskState(task, TaskState.Running, TaskState.Queued);
    }

    /**
     * Task was completed.
     *
     * @return
     */
    @RequestMapping(value="/cluster/v1/task/_completed", method=RequestMethod.POST)
    public void completed(@RequestBody ZpsResult result) {
        logger.info("Result: {}", Json.prettyString(result));
        jobService.setTaskCompleted(result, result.getExitStatus());
    }

}
