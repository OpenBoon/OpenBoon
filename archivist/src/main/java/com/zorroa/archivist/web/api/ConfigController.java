package com.zorroa.archivist.web.api;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.reflect.ClassPath;
import com.zorroa.archivist.service.AnalystService;
import com.zorroa.sdk.processor.Aggregator;
import com.zorroa.sdk.util.IngestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Created by chambers on 12/22/15.
 */
@RestController
public class ConfigController {

    private static final Logger logger = LoggerFactory.getLogger(ConfigController.class);

    @Autowired
    AnalystService analystService;

    private List<String> aggregators = Lists.newArrayList();

    @PostConstruct
    public void init() {
        try {
            ClassLoader classLoader = ConfigController.class.getClassLoader();
            ClassPath classPath = ClassPath.from(classLoader);
            for (ClassPath.ClassInfo info: classPath.getTopLevelClassesRecursive("com.zorroa")) {
                try {
                    Class<?> clazz = classLoader.loadClass(info.getName());
                    if (Aggregator.class.isAssignableFrom(clazz) && !EXCLUDE_INGESTORS.contains(info.getName())) {
                        aggregators.add(info.getName());
                    }
                } catch (NoClassDefFoundError | ClassNotFoundException ignore) {
                    /*
                     * This fails for dynamically loaded classes that inherit from an
                     * external base class.
                     */
                }
            }
        } catch (IOException e) {
            logger.warn("Failed to load aggregators, ", e);
        }
    }

    private Set<String> EXCLUDE_INGESTORS =
            ImmutableSet.of("com.zorroa.sdk.processor.Aggregator");

    @RequestMapping(value="/api/v1/plugins/aggregator", method=RequestMethod.GET)
    public Collection<String> aggregators() {
        return aggregators;
    }
}
