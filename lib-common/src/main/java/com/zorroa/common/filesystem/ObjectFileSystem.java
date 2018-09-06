package com.zorroa.common.filesystem;

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
     * Return an ObjectFile for storing a file with the given arguments.  The directory
     * for storing th file is created automatically.
     *
     * @param category
     * @param value
     * @param type
     * @param variant
     * @return
     */
    OfsFile prepare(String category, Object value, String type, String... variant);

    OfsFile get(String category, Object value, String type, String... variant);

    /**
     * Get a file based its unique ID, which is formatted as category/id_[variant].ext
     *
     * @return
     */
    OfsFile get(String id);

    /**
     * Get a file based its unique ID and name which is formatted as category/name
     *
     * @return
     */
    OfsFile get(String category, String name);
}
