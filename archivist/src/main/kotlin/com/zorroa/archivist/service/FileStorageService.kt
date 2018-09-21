package com.zorroa.archivist.service

import com.zorroa.archivist.domain.FileStorage
import com.zorroa.archivist.domain.FileStorageSpec
import com.zorroa.archivist.filesystem.ObjectFileSystem
import com.zorroa.archivist.filesystem.OfsFile
import org.apache.tika.Tika
import org.springframework.beans.factory.annotation.Autowired
import java.nio.file.Files

interface FileStorageService {
    /**
     * Allocates new file storage.
     *
     * @param[spec] The FileStorageSpec which describes what is being stored.
     * @return a FileStorage object detailing the location of the storage
     */
    fun create(spec: FileStorageSpec) : FileStorage

    /**
     * Use a FileStorageSpec to determine if a file already exists with the given spec.
     *
     * @param[spec] The FileStorageSpec which describes what is being stored.
     * @return a FileStorage object detailing the location of the storage
     */
    fun get(spec: FileStorageSpec) : FileStorage

    /**
     * Use a FileStorage ID to get an existing FileStorage record
     *
     * @param[id] The unqiue id of the storage element
     * @return a FileStorage object detailing the location of the storage
     */
    fun get(id: String) : FileStorage
}


class OfsFileStorageService @Autowired constructor(
        private val ofs: ObjectFileSystem): FileStorageService {

    private val tika = Tika()

    override fun create(spec: FileStorageSpec) : FileStorage {
        val ofile = ofs.prepare(spec.category, spec.name, spec.type, spec.variants)
        return buildFileStorage(ofile)
    }

    override fun get(spec: FileStorageSpec) : FileStorage {
        val ofile = ofs.get(spec.category, spec.name, spec.type, spec.variants)
        return buildFileStorage(ofile)
    }

    override fun get(id: String) : FileStorage {
        val ofile = ofs.get(id)
        return buildFileStorage(ofile)
    }

    private fun buildFileStorage(ofile: OfsFile) : FileStorage {

        val size : Long = try {
            Files.size(ofile.file.toPath())
        }
        catch (e: Exception) {
            -1
        }

        return FileStorage(
                ofile.file.toURI().toString(),
                ofile.id, "file",
                tika.detect(ofile.file.toString()),
                size,
                size != -1L)
    }
}


