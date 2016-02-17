package com.zorroa.archivist.sdk.filesystem;

import com.zorroa.archivist.sdk.domain.Allocation;

import java.io.File;

/**
 * The object file system interface is for storing files of all types, including proxies,
 * cloud based assets, database dumps/files, and any other type of file that an ingestor
 * may need to do its job.
 */
public interface ObjectFileSystem {

    /**
     * Must be called to initialize the filesystem.
     */
    void init();

    /**
     * Set the URI location of the file system.
     *
     * @param location
     */
    void setLocation(String location);

    /**
     * Get the URI location of the file system;
     */
    String getLocation();

    /**
     * Create an allocation for the given category of data.  An allocation
     * is a directory.
     *
     * @param category
     * @return
     */
    Allocation build(String category);

    /**
     * Create an allocation for the given category of data with the given ID.
     *
     * @param category
     * @return
     */
    Allocation build(Object id, String category);

    /**
     * Find the path to the given file using only the category and the name.
     *
     * @param category
     * @param name
     * @return
     */
    File find(String category, String name);

    File get(String category, String path);
}
