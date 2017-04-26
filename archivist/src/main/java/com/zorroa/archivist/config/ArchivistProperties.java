package com.zorroa.archivist.config;

import com.zorroa.common.config.NetworkEnvironment;
import com.zorroa.common.config.NetworkEnvironmentUtils;
import com.zorroa.common.config.SpringApplicationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by chambers on 2/17/16.
 */
@Configuration
public class ArchivistProperties extends SpringApplicationProperties {

    private static final Logger logger = LoggerFactory.getLogger(ArchivistProperties.class);

    @Bean
    public NetworkEnvironment getNetworkEnvironment() {
        return NetworkEnvironmentUtils.getNetworkEnvironment(this);
    }
}
