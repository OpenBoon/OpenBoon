package com.zorroa.archivist.web.cluster;

import com.zorroa.common.repository.ClusterSettingsDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoints for analyst cluster communication.
 */
@RestController
public class ClusterController {

    @Autowired
    ClusterSettingsDao clusterSettingsDao;

    @RequestMapping(value="/cluster/v1/settings", method= RequestMethod.GET)
    public Map<String, Object> getConfig() {
        return clusterSettingsDao.get();
    }

    @RequestMapping(value="/cluster/v1/task/{id}/_update", method= RequestMethod.PUT)
    public Object updateTask() {
        return null;
    }

    @RequestMapping(value="/cluster/v1/job/{id}/_expand", method= RequestMethod.PUT)
    public Object expandJob() {
        return null;
    }

    @RequestMapping(value="/cluster/v1/job/{id}/_reduce", method= RequestMethod.PUT)
    public Map<String, Object> reduceJob() {
        return null;
    }


}
