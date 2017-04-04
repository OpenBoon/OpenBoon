package com.zorroa.cloudproxy.domain;

import java.io.File;

/**
 * Created by wex on 4/1/17.
 */
public class FilesystemEntry {
    public String name;
    public String path;
    public int id;
    public boolean isDirectory;

    public FilesystemEntry(File file) {
        name = file.getName();
        id = file.hashCode();
        path = file.getAbsolutePath();
        isDirectory = file.isDirectory();
    }
}
