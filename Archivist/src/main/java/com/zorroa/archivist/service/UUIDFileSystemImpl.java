package com.zorroa.archivist.service;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.NameBasedGenerator;
import com.zorroa.archivist.domain.Allocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.UUID;

/**
 * A very simple UUID/Object based file system.
 */
@Component
public class UUIDFileSystemImpl implements ObjectFileSystem {

    protected final Logger logger = LoggerFactory.getLogger(UUIDFileSystemImpl.class);

    private static final int DEEPNESS = 4;

    private static final NameBasedGenerator nameBasedGenerator = Generators.nameBasedGenerator();

    @Value("${archivist.storage.basePath}")
    private String storageBasePath;

    private File storageBaseDir;

    @PostConstruct
    public void init() {
        storageBaseDir = new File(storageBasePath);
        storageBaseDir.mkdirs();
    }

    @Override
    public Allocation build(String category) {
        return build(UUID.randomUUID(), category);
    }

    @Override
    public Allocation build(Object id, String category) {
        UUID uuid = nameBasedGenerator.generate(id.toString());
        File path = buildPath(category, uuid.toString(), false);
        return new Allocation(uuid, category, path);
    }

    @Override
    public File find(String category, String name) {
        return buildPath(category, name, true);
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
