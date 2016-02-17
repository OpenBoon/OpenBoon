package com.zorroa.archivist.sdk.filesystem;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.NameBasedGenerator;
import com.zorroa.archivist.sdk.domain.Allocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.UUID;

/**
 * A very simple UUID/Object based file system.
 */

public class UUIDFileSystem implements ObjectFileSystem {

    protected final Logger logger = LoggerFactory.getLogger(UUIDFileSystem.class);

    private static final int DEEPNESS = 4;

    private static final NameBasedGenerator nameBasedGenerator = Generators.nameBasedGenerator();

    private File storageBaseDir;
    private String location;

    public UUIDFileSystem() {}

    public UUIDFileSystem(String location) {
        this.location = location;
        this.init();
    }

    @Override
    public void init() {
        storageBaseDir = new File(location).getAbsoluteFile();
        storageBaseDir.mkdirs();
    }

    public void setLocation(String location) {
        this.location = location;
    }
    public String getLocation() {
        return location;
    }

    @Override
    public Allocation build(String category) {
        return build(UUID.randomUUID(), category);
    }

    @Override
    public Allocation build(Object id, String category) {
        UUID uuid = nameBasedGenerator.generate(id.toString());
        File path = buildPath(category, uuid.toString(), false);
        return new Allocation(uuid, category, path, relative(path));
    }

    @Override
    public File find(String category, String name) {
        return buildPath(category, name, true);
    }

    @Override
    public File get(String category, String path) {
        return new File(new StringBuilder(256)
                .append(storageBaseDir.getAbsolutePath())
                .append("/")
                .append(category)
                .append("/")
                .append(path)
                .toString());
    }

    private File relative(File path) {
        Path abs = path.toPath();
        Path base = storageBaseDir.toPath();
        return base.relativize(abs).toFile();
    }

    private File buildPath(String category, String name, boolean includeName) {
        /*
         * In UUIDFileSystemImpl, the first part of the name has to be a UUID.
         */
        String id = name.substring(0, 35);
        UUID.fromString(id);

        StringBuilder sb = new StringBuilder(256);
        sb.append(storageBaseDir.getAbsolutePath());
        sb.append("/");
        sb.append(category);
        sb.append("/");

        String _id = id.toString();
        for (int i=0; i<=DEEPNESS; i++) {
            sb.append(_id.charAt(i));
            sb.append("/");
        }
        if (includeName) {
            sb.append(name);
        }
        else {
            sb.deleteCharAt(sb.length() - 1);
        }
        return new File(sb.toString());
    }
}
