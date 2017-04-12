package com.zorroa.archivist.config;

import com.zorroa.archivist.HttpUtils;
import com.zorroa.archivist.domain.NetworkEnvironment;
import com.zorroa.common.config.SpringApplicationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by chambers on 2/17/16.
 */
@Configuration
public class ArchivistProperties extends SpringApplicationProperties {

    @Bean
    public NetworkEnvironment getNetworkEnvironment() {
        NetworkEnvironment env = new NetworkEnvironment();
        env.setUri(HttpUtils.getUrl(this));
        env.setLocation(HttpUtils.getLocation());
        return env;
    }
}
