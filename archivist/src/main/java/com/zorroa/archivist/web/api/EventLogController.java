package com.zorroa.archivist.web.api;

import com.zorroa.archivist.domain.EventLogSearch;
import com.zorroa.archivist.service.EventLogService;
import com.zorroa.sdk.domain.PagedList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

/**
 * Created by chambers on 12/29/15.
 */
@PreAuthorize("hasAuthority('group::manager') || hasAuthority('group::administrator')")
@RestController
public class EventLogController {

    @Autowired
    EventLogService eventLogService;

    @RequestMapping(value="/api/v1/eventlogs/{type}/_search", method= RequestMethod.POST)
    public PagedList<Map<String,Object>> search(
            @PathVariable String type,
            @RequestBody EventLogSearch search) throws IOException {
        return eventLogService.getAll(type, search);
    }
}
