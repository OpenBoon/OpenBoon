package com.zorroa.archivist.sdk.filesystem;

import com.zorroa.archivist.sdk.util.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by chambers on 3/9/16.
 */
public class ObjectFile {

    private final File file;

    public ObjectFile(File file) {
        this.file = file;
    }

    public ObjectFile(String file) {
        this.file = new File(file);
    }

    public void store(InputStream src) throws IOException {
        mkdirs();
        try (FileOutputStream fos = new FileOutputStream(file)) {
            int inByte;
            while ((inByte = src.read()) != -1)
                fos.write(inByte);
        }
    }

    public void mkdirs() throws IOException {
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

}
