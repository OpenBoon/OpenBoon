package com.zorroa.archivist.filesystem

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
interface ObjectFileSystem {

    /**
     * Return an ServableFile for storing a file with the given arguments.  The directory
     * for storing th file is created automatically.
     *
     * @param category
     * @param value
     * @param type
     * @param variant
     * @return
     */
    fun prepare(category: String, value: Any, type: String, variant: List<String>?): OfsFile

    operator fun get(category: String, value: Any, type: String, variant: List<String>?): OfsFile

    /**
     * Get a file based its unique ID, which is formatted as category/id_[variant].ext
     *
     * @return
     */
    operator fun get(id: String): OfsFile

    /**
     * Get a file based its unique ID and name which is formatted as category/name
     *
     * @return
     */
    operator fun get(category: String, name: String): OfsFile
}
