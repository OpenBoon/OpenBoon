package com.zorroa.analyst.web;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.reflect.ClassPath;
import com.zorroa.archivist.sdk.processor.ProcessorFactory;
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
 * Created by chambers on 12/22/15.
 */
@RestController
public class ConfigController {

    private static final Logger logger = LoggerFactory.getLogger(ConfigController.class);

    private Set<String> EXCLUDE_INGESTORS =
            ImmutableSet.of("com.zorroa.archivist.sdk.processor.ingest.IngestProcessor");

    @RequestMapping(value="/api/v1/plugins/ingest", method=RequestMethod.GET)
    public List<String> ingest() {
        List<String> result = Lists.newArrayListWithCapacity(30);
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
                } catch (NoClassDefFoundError | ClassNotFoundException ignore) {
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
