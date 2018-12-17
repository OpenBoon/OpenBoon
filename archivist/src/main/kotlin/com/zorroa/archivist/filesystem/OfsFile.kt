package com.zorroa.archivist.filesystem

import com.zorroa.archivist.util.FileUtils

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.Objects

/**
 * Created by chambers on 3/9/16.
 */
class OfsFile(val category: String, val name: String, val file: File) {

    val id: String
        get() = String.format("%s/%s", category, FileUtils.filename(file.absolutePath))

    val path: Path = file.toPath()

    /**
     * Symlink the given path into this OFS position.
     *
     * @param path
     * @throws IOException
     */
    @Throws(IOException::class)
    fun link(path: Path) {
        mkdirs()
        Files.createSymbolicLink(file.toPath(), path)
    }

    fun mkdirs() {
        val dir = file.parentFile
        if (!dir.exists()) {
            dir.mkdirs()
        }
    }

    fun exists(): Boolean {
        return file.exists()
    }

    fun type(): String {
        return FileUtils.extension(file.path)
    }

    fun size(): Long {
        return file.length()
    }

    fun deleteOnExit() {
        file.deleteOnExit()
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val that = o as OfsFile?
        return file == that!!.file
    }

    override fun hashCode(): Int {
        return Objects.hashCode(file)
    }
}
