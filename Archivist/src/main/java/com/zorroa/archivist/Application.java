package com.zorroa.archivist;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.system.ApplicationPidFileWriter;
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
        if (ps.getProperty("pidfile") != null) {
            app.addListeners(new ApplicationPidFileWriter((String) ps.getProperty("pidfile")));
        }
        app.run(args);
    }
}
