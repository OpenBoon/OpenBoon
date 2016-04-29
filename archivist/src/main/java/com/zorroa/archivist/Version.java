package com.zorroa.archivist;

import java.io.IOException;
import java.util.Properties;

/**
 * Created by chambers on 2/29/16.
 */
public class Version {

    public static void main(String[] args) {
        Properties props = new Properties();
        try {
            props.load(Version.class.getResourceAsStream("/META-INF/maven/com.zorroa/archivist/pom.properties"));
            System.out.println(props.getProperty("version"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
