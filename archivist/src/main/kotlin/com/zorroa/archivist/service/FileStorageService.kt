package com.zorroa.archivist.service

import com.zorroa.archivist.domain.FileStorage
import com.zorroa.archivist.domain.FileStorageSpec
import com.zorroa.archivist.domain.FileStorageStat
import com.zorroa.archivist.filesystem.ObjectFileSystem
import com.zorroa.archivist.util.FileUtils
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

     * @param[spec] The FileStorageSpec which describes what is being stored.
     * @return a FileStorageStat object which contains the size, mimeType, and online status.
     */
    fun getStat(spec: FileStorageSpec) : FileStorageStat
}


class OfsFileStorageService @Autowired constructor(
        private val ofs: ObjectFileSystem): FileStorageService {

    private val tika = Tika()

    override fun create(spec: FileStorageSpec) : FileStorage {
        val ofile = ofs.prepare(spec.category, spec.name, spec.type, spec.variants)
        return FileStorage(ofile.file.toURI().toString(), ofile.id, "file",
                tika.detect(ofile.file.toString()))
    }

    override fun getStat(spec: FileStorageSpec) : FileStorageStat {
        val ofile = ofs.get(spec.category, spec.name, spec.type, spec.variants)

        return try {
            FileStorageStat(
                    Files.size(ofile.file.toPath()),
                    FileUtils.getMediaType(ofile.file),
                    true)
        } catch (e: Exception) {
            FileStorageStat(
                   -1,
                    FileUtils.getMediaType(ofile.file),
                    false)
        }
    }
}


