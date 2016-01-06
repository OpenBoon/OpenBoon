package com.zorroa.archivist.web;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.reflect.ClassPath;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * A controller for interrogating various types of plugins.
 */
@RestController
public class PluginsController {

    private static final Logger logger = LoggerFactory.getLogger(PluginsController.class);

    private Set<String> EXCLUDE_INGESTORS =
            ImmutableSet.of("com.zorroa.archivist.sdk.processor.ingest.IngestProcessor");

    @RequestMapping(value="/api/v1/plugins/ingestors", method= RequestMethod.GET)
    public List<String> ingestors() {
        List<String> result = Lists.newArrayListWithCapacity(30);
        try {
            ClassPath classPath = ClassPath.from(getClass().getClassLoader());
            for (ClassPath.ClassInfo info: classPath.getTopLevelClassesRecursive("com.zorroa")) {
                if (IngestProcessor.class.isAssignableFrom(info.load()) && !EXCLUDE_INGESTORS.contains(info.getName())) {
                    result.add(info.getName());
                }
            }
        } catch (IOException e) {
            logger.warn("Failed to load ingestors, ", e);
        }

        return result;
    }
}
