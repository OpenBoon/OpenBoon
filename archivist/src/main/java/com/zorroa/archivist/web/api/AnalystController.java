package com.zorroa.archivist.web.api;

import com.zorroa.archivist.service.AnalystService;
import com.zorroa.common.domain.Paging;
import com.zorroa.sdk.domain.Analyst;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Created by chambers on 2/9/16.
 */
@RestController
public class AnalystController {

    private static final Logger logger = LoggerFactory.getLogger(AnalystController.class);

    @Autowired
    AnalystService analystService;

    @RequestMapping(value="/api/v1/analyst", method=RequestMethod.GET)
    public List<Analyst> getAll(
            @RequestParam(value="page", required=false) Integer page,
            @RequestParam(value="count", required=false) Integer count) {
        return analystService.getAll(new Paging(page, count));
    }
}
