package com.zorroa.archivist;

import com.fasterxml.jackson.module.kotlin.KotlinModule;
import com.zorroa.sdk.util.Json;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.system.ApplicationPidFileWriter;
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
        if (ps.getProperty("pidfile") != null) {
            app.addListeners(new ApplicationPidFileWriter((String) ps.getProperty("pidfile")));
        }

        Json.Mapper.registerModule(new KotlinModule());
        app.run(args);
    }
}
