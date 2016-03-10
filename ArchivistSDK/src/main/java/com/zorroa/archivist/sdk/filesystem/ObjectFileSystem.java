package com.zorroa.archivist.sdk.filesystem;

/**
 * The object file system interface is for storing files of all types, including proxies,
 * assets, spreadsheets, models, and any other type of file the analyst may need to its
 * job.  Its referred to as an object file system because the ID of a file is calculated
 * from its content or human readable name.
 *
 * In object file systems the content is typically retrieved using the objects ID, however
 * implementations of this interface should also allow retrieval by relative path.
 *
 */
public interface ObjectFileSystem {

    /**
     * Get a file based on a category, the ID of the object, the type, and variable
     * argument list of variant.  The file may or may not exist already.
     *
     * @param category
     * @param id
     * @param type
     * @param variant
     * @return
     */
    ObjectFile get(String category, Object id, String type, String ... variant);

    /**
     * Get a file based its path into the object file system.
     *
     * @param relativePath
     * @return
     */
    ObjectFile get(String relativePath);

    /**
     * Return true if an object that hashes to a specific key exists in the filesystem.
     *
     * @param category
     * @param id
     * @return
     */
    boolean exists(String category, Object id, String type, String ... variant);

    /**
     * Return true if a relative path within the file system.  Non realtive paths
     * will be treated as relative to the filesystem room.
     *
     * @param relativePath
     * @return
     */
    boolean exists(String relativePath);

    /**
     * Get a URL that can be used to download this file from the server.
     *
     * @param file
     * @return
     */
    String getUrl(ObjectFile file);
}
