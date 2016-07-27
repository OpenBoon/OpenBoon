package com.zorroa.archivist.web.api;

import com.google.common.collect.ImmutableMap;
import com.zorroa.common.domain.EventSearch;
import com.zorroa.common.domain.Paging;
import com.zorroa.common.repository.EventLogDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * Created by chambers on 12/29/15.
 */
@RestController
public class EventLogController {

    @Autowired
    EventLogDao eventLogDao;

    @RequestMapping(value="/api/v1/eventlog/_search", method= RequestMethod.POST)
    public Object getAll(@RequestBody(required=false) EventSearch search,
                         @RequestParam(value="page", required=false) Integer page) throws IOException {
        if (search == null) {
            search = new EventSearch();
        }
        return eventLogDao.getAll(search, new Paging(page));
    }

    @RequestMapping(value="/api/v2/eventlog/_search", method= RequestMethod.POST)
    public Object getAll_v2(@RequestBody(required=false) EventSearch search,
                            @RequestParam(value="page", required=false) Integer page) throws IOException {
        if (search == null) {
            search = new EventSearch();
        }
        return eventLogDao.getAll(search, new Paging(page));
    }

    @RequestMapping(value="/api/v1/eventlog/_count", method= RequestMethod.POST, produces= MediaType.APPLICATION_JSON_VALUE)
    public Object getCount(@RequestBody(required=false) EventSearch search) throws IOException {
        if (search == null) {
            search = new EventSearch();
        }
        return ImmutableMap.of("count", eventLogDao.count(search));
    }
}
