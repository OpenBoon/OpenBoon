package com.zorroa.archivist.web;

import com.zorroa.archivist.HttpUtils;
import com.zorroa.archivist.repository.EventLogDao;
import com.zorroa.archivist.sdk.domain.EventLogSearch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by chambers on 12/29/15.
 */
@RestController
public class EventLogController {

    @Autowired
    EventLogDao eventLogDao;

    @RequestMapping(value="/api/v1/eventlog/_search", method= RequestMethod.POST)
    public void getAll(@RequestBody(required=false) EventLogSearch search, HttpServletResponse httpResponse) throws IOException {
        if (search == null) {
            search = new EventLogSearch();
        }
        HttpUtils.writeElasticResponse(eventLogDao.getAll(search), httpResponse);
    }

    @RequestMapping(value="/api/v1/eventlog/_count", method= RequestMethod.POST, produces= MediaType.APPLICATION_JSON_VALUE)
    public String getCount(@RequestBody(required=false) EventLogSearch search) throws IOException {
        if (search == null) {
            search = new EventLogSearch();
        }
        return HttpUtils.countResponse(eventLogDao.getCount(search));
    }
}
