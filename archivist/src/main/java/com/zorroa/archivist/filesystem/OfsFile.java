package com.zorroa.archivist.filesystem;

import com.zorroa.archivist.util.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Created by chambers on 3/9/16.
 */
public class OfsFile {

    private final File file;
    private final String category;
    private final String name;

    public OfsFile(String category, String name, File file) {
        this.category = category;
        this.name = name;
        this.file = file;
    }

    public void store(InputStream src) throws IOException {
        mkdirs();
        try (FileOutputStream fos = new FileOutputStream(file)) {
            int inByte;
            while ((inByte = src.read()) != -1)
                fos.write(inByte);
        }
    }

    /**
     * Symlink the given path into this OFS position.
     *
     * @param path
     * @throws IOException
     */
    public void link(Path path) throws IOException {
        mkdirs();
        Files.createSymbolicLink(file.toPath(), path);
    }

    public void mkdirs() {
        File dir = file.getParentFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public boolean exists() {
        return file.exists();
    }

    public File getFile() {
        return file;
    }

    public String type() {
        return FileUtils.extension(file.getPath());
    }

    public long size() {
        return file.length();
    }

    public String getCategory() {
        return category;
    }

    public String getId() {
        return String.format("%s/%s", category, FileUtils.filename(file.getAbsolutePath()));
    }

    public void deleteOnExit() {
        getFile().deleteOnExit();
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OfsFile that = (OfsFile) o;
        return Objects.equals(getFile(), that.getFile());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getFile());
    }
}
