package com.zorroa.archivist.sdk.filesystem;

import java.net.URI;

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


    ObjectFile transfer(URI src, ObjectFile dst);

    /**
     * Get a file based on a category, the ID of the object, the type, and variable
     * argument list of variant.  The file may or may not exist already.  The
     * parent directory is automatically created.
     *
     * @param category
     * @param hashable
     * @param type
     * @param variant
     * @return
     */
    ObjectFile prepare(String category, Object hashable, String type, String ... variant);

    /**
     * Get a file based its unique ID, which is formatted as category/id_[variant].ext
     *
     * @return
     */
    ObjectFile get(String id);

    ObjectFile get(String category, String name);
    /**
     * Get a URL that can be used to download this file from the server.
     *
     * @param file
     * @return
     */
    String getUrl(ObjectFile file);

}
