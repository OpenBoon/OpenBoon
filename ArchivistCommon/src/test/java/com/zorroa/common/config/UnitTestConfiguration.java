package com.zorroa.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by chambers on 2/17/16.
 */
@Configuration
public class UnitTestConfiguration {

    @Bean
    public SpringApplicationProperties properties() {
        return new SpringApplicationProperties();
    }

}
