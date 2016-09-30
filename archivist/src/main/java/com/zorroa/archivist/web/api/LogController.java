package com.zorroa.archivist.web.api;

import com.zorroa.archivist.domain.LogSearch;
import com.zorroa.archivist.service.LogService;
import com.zorroa.sdk.domain.Pager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * Created by chambers on 12/29/15.
 */
@RestController
public class LogController {

    @Autowired
    LogService logService;

    @RequestMapping(value="/api/v1/logs/_search", method= RequestMethod.GET)
    public Object search(@RequestBody(required=false) LogSearch search,
                            @RequestParam(value="page", required=false) Integer page) throws IOException {
        return logService.search(search == null ? new LogSearch() : search, new Pager(page));
    }
}
