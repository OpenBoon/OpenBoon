package com.zorroa.common;

import com.zorroa.common.config.SpringApplicationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

/**
 * Created by chambers on 2/17/16.
 */
@Configuration
public class UnitTestConfiguration {

    @Bean
    public SpringApplicationProperties properties() {
        return new SpringApplicationProperties();
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }
}
