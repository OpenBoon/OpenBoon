package com.zorroa.archivist.config;

import com.zorroa.common.config.ApplicationProperties;
import com.zorroa.sdk.util.FileUtils;
import org.h2.server.web.WebServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * Created by chambers on 10/25/16.
 */
@Configuration
public class StaticResourceConfiguration extends WebMvcConfigurerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(StaticResourceConfiguration.class);

    @Autowired
    ApplicationProperties properties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        java.io.File path = new java.io.File(FileUtils.normalize(properties.getString("archivist.path.docs") + "/"));
        registry.addResourceHandler("/docs/**").addResourceLocations(path.toURI().toString());
        super.addResourceHandlers(registry);
    }

    @Bean
    public ServletRegistrationBean h2servletRegistration() {
        ServletRegistrationBean registrationBean = new ServletRegistrationBean(new WebServlet());
        registrationBean.addUrlMappings("/console/*");
        return registrationBean;
    }

}
