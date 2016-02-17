package com.zorroa.archivist.sdk.domain;

import com.google.common.io.Files;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An allocation is a directory created by an ObjectFileSystem implementation.
 */
public class Allocation {

    private static final Logger logger = LoggerFactory.getLogger(Allocation.class);

    private UUID id;
    private String category;
    private File path;
    private File relative;
    private AtomicBoolean exists = new AtomicBoolean(false);

    public Allocation(UUID id, String category, File path, File relative) {
        this.id = id;
        this.category = category;
        this.path = path;
        this.relative = relative;
        this.exists.set(path.exists());
    }

    public Allocation create() {
        this.path.mkdirs();
        return this;
    }

    public String getRelativePath(String name) {
        return new StringBuilder(256)
                .append(relative.toString())
                .append("/")
                .append(id.toString())
                .append("_")
                .append(name)
                .toString();
    }

    public File getRelativePath(String type, Object ... tokens) {
        StringBuilder sb = new StringBuilder(256)
                .append(relative.toString())
                .append("/")
                .append(id.toString())
                .append("_");
        for (Object token: tokens) {
            sb.append(token.toString().replace('.', '_'));
            sb.append("_");
        }
        sb.deleteCharAt(sb.length()-1);
        sb.append(".");
        sb.append(type);
        return new File(sb.toString());
    }

    public File getAbsolutePath(String name) {
        return new File(new StringBuilder(256)
                .append(path.getAbsolutePath())
                .append("/")
                .append(id.toString())
                .append("_")
                .append(name)
                .toString());
    }

    public File getAbsolutePath(String type, Object ... tokens) {
        StringBuilder sb = new StringBuilder(256)
                .append(path.getAbsolutePath())
                .append("/")
                .append(id.toString())
                .append("_");
        for (Object token: tokens) {
            sb.append(token.toString().replace('.', '_'));
            sb.append("_");
        }
        sb.deleteCharAt(sb.length()-1);
        sb.append(".");
        sb.append(type);
        return new File(sb.toString());
    }

    public boolean exists(String type, Object ... tokens) {
        return getAbsolutePath(type, tokens).exists();
    }

    public File store(InputStream src, String type, Object ... tokens) throws IOException {
        if (exists.compareAndSet(false, true)) {
            create();
        }
        File path = getAbsolutePath(type, tokens);
        try (FileOutputStream fos = new FileOutputStream(path)) {
            int inByte;
            while ((inByte = src.read()) != -1)
                fos.write(inByte);
        }
        return path;
    }

    public File store(File src, String type, Object ... tokens) throws IOException {
        if (exists.compareAndSet(false, true)) {
            create();
        }
        File dst = getAbsolutePath(type, tokens);
        Files.copy(src, dst);
        return dst;
    }
}
