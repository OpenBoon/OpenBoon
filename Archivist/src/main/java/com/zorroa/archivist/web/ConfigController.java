package com.zorroa.archivist.web;

import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.ClassPath;
import com.zorroa.archivist.sdk.processor.ProcessorFactory;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import com.zorroa.archivist.sdk.util.IngestUtils;
import com.zorroa.archivist.service.AnalystService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Created by chambers on 12/22/15.
 */
@RestController
public class ConfigController {

    private static final Logger logger = LoggerFactory.getLogger(ConfigController.class);

    private Set<String> EXCLUDE_INGESTORS =
            ImmutableSet.of("com.zorroa.archivist.sdk.processor.ingest.IngestProcessor");

    @Autowired
    AnalystService analystService;

    @RequestMapping(value = "/api/v1/config/supported_formats", method = RequestMethod.GET)
    public Object supportedFormats() {
        return IngestUtils.SUPPORTED_FORMATS;
    }

    @RequestMapping(value="/api/v1/plugins/ingest", method= RequestMethod.GET)
    public List<String> ingest() throws Exception {

        logger.info("getting ingestors");
        List<String> result = analystService.getAnalystClient().getAvailableIngestors();

        try {
            // Use the special ingest factory ClassLoader
            ClassLoader classLoader = new ProcessorFactory<>().getSiteClassLoader();
            ClassPath classPath = ClassPath.from(classLoader);
            for (ClassPath.ClassInfo info: classPath.getTopLevelClassesRecursive("com.zorroa")) {
                try {
                    Class<?> clazz = classLoader.loadClass(info.getName()); // Avoid Guava's info.load()
                    if (IngestProcessor.class.isAssignableFrom(clazz) && !EXCLUDE_INGESTORS.contains(info.getName())) {
                        result.add(info.getName());
                    }
                } catch (java.lang.NoClassDefFoundError | ClassNotFoundException ignore) {
                    /*
                     * This fails for dynamically loaded classes that inherit from an
                     * external base class.
                     */
                }
            }
        } catch (IOException e) {
            logger.warn("Failed to load ingestors, ", e);
        }

        return result;
    }
}
