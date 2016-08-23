package com.zorroa.analyst.config;

import com.zorroa.analyst.ArchivistClient;
import com.zorroa.common.config.CommonBeanConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * Created by chambers on 2/16/16.
 */
@Configuration
@Order(Ordered.LOWEST_PRECEDENCE)
public class AnalystCommonBeanConfig extends CommonBeanConfig {

    @Autowired
    ArchivistClient archivistClient;

}
