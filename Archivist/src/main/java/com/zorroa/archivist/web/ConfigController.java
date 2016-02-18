package com.zorroa.archivist.web;

import com.google.common.collect.Sets;
import com.zorroa.archivist.sdk.domain.Analyst;
import com.zorroa.archivist.sdk.util.IngestUtils;
import com.zorroa.archivist.service.AnalystService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.Set;

/**
 * Created by chambers on 12/22/15.
 */
@RestController
public class ConfigController {

    private static final Logger logger = LoggerFactory.getLogger(ConfigController.class);

    @Autowired
    AnalystService analystService;

    @RequestMapping(value = "/api/v1/config/supported_formats", method = RequestMethod.GET)
    public Object supportedFormats() {
        return IngestUtils.SUPPORTED_FORMATS;
    }

    @RequestMapping(value="/api/v1/plugins/ingest", method= RequestMethod.GET)
    public Collection<String> ingest() throws Exception {
        Set<String> ingestorClasses = Sets.newHashSet();
        for (Analyst a: analystService.getAll()) {
            ingestorClasses.addAll(a.getIngestProcessorClasses());
        }
        return ingestorClasses;
    }
}
