package com.zorroa.analyst;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.system.ApplicationPidFileWriter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.SimpleCommandLinePropertySource;

/**
 * Created by chambers on 2/8/16.
 */
@ComponentScan
@EnableAutoConfiguration
@SpringBootApplication
@EnableConfigurationProperties
public class Application {
    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        PropertySource ps = new SimpleCommandLinePropertySource(args);
        SpringApplication app =
                new SpringApplication(Application.class);

        if (ps.getProperty("p") != null) {
            app.addListeners(new ApplicationPidFileWriter((String) ps.getProperty("p")));
        }
        app.run(args);
    }

    public static boolean isUnitTest() {
        return Boolean.parseBoolean(System.getProperty("zorroa.unittest"));
    }
}
