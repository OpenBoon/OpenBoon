package com.zorroa.archivist.web;

import com.zorroa.archivist.sdk.domain.Analyst;
import com.zorroa.archivist.sdk.domain.AnalystPing;
import com.zorroa.archivist.service.AnalystService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.ServletRequest;
import java.net.URI;
import java.util.List;

/**
 * Created by chambers on 2/9/16.
 */
@RestController
public class AnalystController {

    private static final Logger logger = LoggerFactory.getLogger(AnalystController.class);

    @Autowired
    AnalystService analystService;

    @RequestMapping(value="/api/v1/analyst/_register", method=RequestMethod.POST)
    public void register(@RequestBody AnalystPing ping, ServletRequest req) {
        /*
         * Override the IP/HOST sent in the ping.
         */
        URI uri = URI.create(ping.getUrl());
        ping.setUrl(uri.getScheme() + "://" + req.getRemoteAddr() + ":" + uri.getPort());
        
        logger.info("Received ping: {}", ping);
        analystService.register(ping);
    }

    @RequestMapping(value="/api/v1/analyst/_shutdown", method=RequestMethod.POST)
    public void shutdown(@RequestBody AnalystPing ping) {
        logger.info("Received shutdown ping: {}", ping);
        analystService.shutdown(ping);
    }

    @RequestMapping(value="/api/v1/analyst", method=RequestMethod.GET)
    public List<Analyst> getAll() {
        return analystService.getAll();
    }
}
