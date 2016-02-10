package com.zorroa.archivist.web;

import com.zorroa.archivist.sdk.domain.AnalystPing;
import com.zorroa.archivist.service.AnalystService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Created by chambers on 2/9/16.
 */
@Controller
public class AnalystController {

    @Autowired
    AnalystService analystService;

    @RequestMapping(value="/api/v1/analyst/_register", method=RequestMethod.POST)
    public void register(@RequestBody AnalystPing ping) {
        analystService.register(ping);
    }

    @RequestMapping(value="/api/v1/analyst/_shutdown", method=RequestMethod.POST)
    public void shutdown(@RequestBody AnalystPing ping) {
        analystService.shutdown(ping);
    }
}
