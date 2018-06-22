package com.zorroa.archivist;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.SimpleCommandLinePropertySource;

@ComponentScan
@EnableAutoConfiguration
@SpringBootApplication
@EnableConfigurationProperties
public class Application {

    public static void main(String[] args) {
        PropertySource ps = new SimpleCommandLinePropertySource(args);
        SpringApplication app =
                new SpringApplication(Application.class);
        app.run(args);
    }
}
