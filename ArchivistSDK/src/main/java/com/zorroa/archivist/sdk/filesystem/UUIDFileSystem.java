package com.zorroa.archivist.sdk.filesystem;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.NameBasedGenerator;

import java.io.File;
import java.nio.file.Path;
import java.util.Properties;

/**
 * A very simple UUID/Object based file system.
 */
public class UUIDFileSystem extends AbstractFileSystem {

    private static final int DEEPNESS = 8;

    private static final NameBasedGenerator nameBasedGenerator = Generators.nameBasedGenerator();

    private String baseDir;

    public UUIDFileSystem(Properties props) {
        super(props);
    }

    @Override
    public void init() {
        File directory = new File(properties.getProperty("analyst.filesystem.root"));
        directory.mkdirs();

        baseDir = directory.toPath().normalize().toAbsolutePath().toString();
        logger.info("Filesystem initialized: {}", baseDir);
    }

    @Override
    public ObjectFile get(String category, Object id, String type, String ... variant) {

        String uuid = nameBasedGenerator.generate(id.toString()).toString();

        StringBuilder sb = new StringBuilder(256);
        sb.append(baseDir);
        sb.append("/");
        sb.append(category);
        sb.append("/");

        String _id = uuid.toString();
        for (int i=0; i<=DEEPNESS; i++) {
            sb.append(_id.charAt(i));
            sb.append("/");
        }

        sb.append(uuid);
        if (variant.length > 0) {
            sb.append("_");
            sb.append(String.join("_", variant));
        }
        sb.append(".");
        sb.append(type);

        return new ObjectFile(sb.toString());
    }

    @Override
    public ObjectFile get(String path) {
        if (path.startsWith("/")) {
            throw new IllegalArgumentException("Absolute paths are not valid: " + path);
        }
        return new ObjectFile(new StringBuilder(baseDir.length() + path.length() + 1)
                .append(baseDir).append("/").append(path).toString());
    }

    @Override
    public boolean exists(String category, Object id, String type, String ... variant) {
        return get(category, id, type, variant).exists();
    }

    @Override
    public boolean exists(String path) {
        if (path.startsWith("/")) {
            throw new IllegalArgumentException("Absolute paths are not valid: " + path);
        }
        return new File(new StringBuilder(baseDir.length() + path.length() + 1)
                .append(baseDir).append("/").append(path).toString()).exists();
    }

    @Override
    public String getUrl(ObjectFile file) {
        Path abs = file.getFile().toPath();
        Path base = new File(baseDir).toPath();

        StringBuilder url = new StringBuilder(128);
        url.append(System.getProperty("server.url"));
        url.append("/api/v1/fs/");
        url.append(base.relativize(abs).toString());
        return url.toString();
    }

}
