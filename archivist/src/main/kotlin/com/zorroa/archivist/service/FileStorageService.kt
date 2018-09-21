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
    fun create(spec: FileStorageSpec) : FileStorage
    fun getStat(spec: FileStorageSpec) : FileStorageStat
}

private inline fun checkNullVariants(spec: FileStorageSpec) : Array<String> {
    return if (spec.variants == null) {
        emptyArray()
    }
    else {
        spec.variants.toTypedArray()
    }
}

class OfsFileStorageService @Autowired constructor(
        private val ofs: ObjectFileSystem): FileStorageService {

    private val tika = Tika()

    override fun create(spec: FileStorageSpec) : FileStorage {
        val variants = checkNullVariants(spec)
        val ofile = ofs.prepare(spec.category, spec.name, spec.type, *variants)
        return FileStorage(ofile.file.toURI().toString(), ofile.id, "file",
                tika.detect(ofile.file.toString()))
    }

    override fun getStat(spec: FileStorageSpec) : FileStorageStat {
        val variants = checkNullVariants(spec)
        val ofile = ofs.get(spec.category, spec.name, spec.type, *variants)

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


