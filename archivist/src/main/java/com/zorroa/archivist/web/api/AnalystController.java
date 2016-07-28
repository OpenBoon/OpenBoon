package com.zorroa.archivist.web.api;

import com.zorroa.archivist.service.AnalystService;
import com.zorroa.common.domain.PagedList;
import com.zorroa.common.domain.Paging;
import com.zorroa.common.repository.ClusterSettingsDao;
import com.zorroa.sdk.domain.Analyst;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by chambers on 2/9/16.
 */
@RestController
public class AnalystController {

    @Autowired
    AnalystService analystService;

    @Autowired
    ClusterSettingsDao clusterConfigDao;

    @RequestMapping(value="/api/v1/analysts", method=RequestMethod.GET)
    public PagedList<Analyst> getAll(
            @RequestParam(value="page", required=false) Integer page,
            @RequestParam(value="count", required=false) Integer count) {
        return analystService.getAll(new Paging(page, count));
    }
}
