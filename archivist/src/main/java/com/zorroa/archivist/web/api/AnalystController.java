package com.zorroa.archivist.web.api;

import com.zorroa.archivist.service.AnalystService;
import com.zorroa.common.domain.Analyst;
import com.zorroa.sdk.domain.PagedList;
import com.zorroa.sdk.domain.Pager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by chambers on 2/9/16.
 */
@PreAuthorize("hasAuthority('group::developer') || hasAuthority('group::administrator')")
@RestController
public class AnalystController {

    @Autowired
    AnalystService analystService;

    @RequestMapping(value="/api/v1/analysts", method=RequestMethod.GET)
    public PagedList<Analyst> getAll(
            @RequestParam(value="page", required=false) Integer page,
            @RequestParam(value="count", required=false) Integer count) {
        return analystService.getAll(new Pager(page, count));
    }
}
