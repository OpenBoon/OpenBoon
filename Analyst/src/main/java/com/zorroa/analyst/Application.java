package com.zorroa.analyst;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.system.ApplicationPidFileWriter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

/**
 * Created by chambers on 2/8/16.
 */
@ComponentScan
@EnableAutoConfiguration
@SpringBootApplication
@EnableConfigurationProperties
public class Application {

    public static void main(String[] args) {
        SpringApplication app =
                new SpringApplication(Application.class);
        if (System.getenv("PIDFILE") != null) {
            app.addListeners(new ApplicationPidFileWriter());
        }
        app.run(args);
    }

    public static boolean isUnitTest() {
        return Boolean.parseBoolean(System.getProperty("zorroa.unittest"));
    }
}
