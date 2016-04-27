package com.zorroa.analyst.domain;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Created by chambers on 4/26/16.
 */
public class PluginProperties {

    private String name;
    private String description;
    private String version;
    private String className;

    public PluginProperties() { }

    public PluginProperties(String name, String description, String version, String className) {
        this.name = name;
        this.description = description;
        this.version = version;
        this.className = className;
    }

    public static PluginProperties readFromProperties(Path dir) throws IOException {
        Path descriptor = dir.resolve("plugin.properties");
        Properties props = new Properties();
        try (InputStream stream = Files.newInputStream(descriptor)) {
            props.load(stream);
        }
        String name = props.getProperty("name");
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Property [name] is missing in [" + descriptor + "]");
        }
        String description = props.getProperty("description");
        if (description == null) {
            throw new IllegalArgumentException("Property [description] is missing for plugin [" + name + "]");
        }
        String version = props.getProperty("version");
        if (version == null) {
            throw new IllegalArgumentException("Property [version] is missing for plugin [" + name + "]");
        }

        String esVersionString = props.getProperty("zorroa.version");
        if (esVersionString == null) {
            throw new IllegalArgumentException("Property [zorroa.version] is missing for plugin [" + name + "]");
        }

        String classname = props.getProperty("className");
        if (classname == null) {
            throw new IllegalArgumentException("Property [className] is missing for plugin [" + name + "]");
        }

        return new PluginProperties(name, description, version, classname);
    }


    public String getName() {
        return name;
    }

    public PluginProperties setName(String name) {
        this.name = name;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public PluginProperties setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getVersion() {
        return version;
    }

    public PluginProperties setVersion(String version) {
        this.version = version;
        return this;
    }

    public String getClassName() {
        return className;
    }

    public PluginProperties setClassName(String className) {
        this.className = className;
        return this;
    }
}
