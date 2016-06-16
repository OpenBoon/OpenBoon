package com.zorroa.archivist.web.api;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;
import com.google.common.reflect.ClassPath;
import com.zorroa.archivist.service.AnalystService;
import com.zorroa.sdk.plugins.PluginProperties;
import com.zorroa.sdk.processor.Aggregator;
import com.zorroa.sdk.processor.ProcessorType;
import com.zorroa.sdk.util.IngestUtils;
import com.zorroa.sdk.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
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

    @RequestMapping(value = "/api/v1/config/supported_formats", method = RequestMethod.GET)
    public Object supportedFormats() {
        return IngestUtils.SUPPORTED_FORMATS;
    }

    @RequestMapping(value="/api/v1/plugins", method= RequestMethod.GET)
    public List<PluginProperties> getPlugins() {
        return analystService.getPlugins();
    }

    @RequestMapping(value="/api/v1/plugins/processors", method= RequestMethod.GET)
    public Object getProcessors() {
        return analystService.getProcessors();
    }

    @RequestMapping(value="/api/v1/plugins/processors/{type}", method= RequestMethod.GET)
    public Object getProcessorsByType(@PathVariable String type) {
        Integer ival = Ints.tryParse(type);
        if (ival != null) {
            return analystService.getProcessors(ProcessorType.values()[ival.intValue()]);
        }
        else {
            return analystService.getProcessors(ProcessorType.valueOf(StringUtils.capitalize(type)));
        }
    }

    private Set<String> EXCLUDE_INGESTORS =
            ImmutableSet.of("com.zorroa.sdk.processor.Aggregator");

    @RequestMapping(value="/api/v1/plugins/aggregator", method=RequestMethod.GET)
    public Collection<String> aggregators() {
        return aggregators;
    }
}
