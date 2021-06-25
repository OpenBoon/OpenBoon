package boonai.archivist.storage

import boonai.archivist.domain.BoonLib
import boonai.archivist.domain.BoonLibImportResponse
import boonai.archivist.domain.Dataset
import org.springframework.core.io.Resource
import org.springframework.http.ResponseEntity
import java.io.InputStream

interface BoonLibStorageService {
    /**
     * Copy a Map<src file, dst file> of files from one place to another.
     */
    fun copyFromProject(paths: Map<String, String>)

    /**
     * Store a file at the given path.
     */
    fun store(path: String, size: Long, stream: InputStream)

    /**
     * Stream a file using a ResponseEntity
     */
    fun stream(path: String): ResponseEntity<Resource>

    /**
     * Import assets into the given dataset.
     */
    fun importAssetsInto(boonLib: BoonLib, dataset: Dataset): BoonLibImportResponse
}
