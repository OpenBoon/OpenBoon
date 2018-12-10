package com.zorroa.archivist.domain

import com.zorroa.archivist.search.AssetSearch
import java.util.*


/**
 * Defines fields needed to make new ExportFile.
 * @property storageId: The id of the exported file.
 * @property filename: The filename the server will set when downloaded.
 */
data class ExportFileSpec (
        var storageId: String,
        var filename: String
)

/**
 * An ExportFile record.
 */
data class ExportFile (
        val id: UUID,
        val exportId: UUID,
        val name: String,
        val path: String,
        val mimeType : String,
        val size : Long,
        val timeCreated: Long)

/**
 * Defines fields needed to create a new export.
 */
data class ExportSpec (
        var name: String?,
        var search: AssetSearch,
        var processors: List<ProcessorRef> = mutableListOf(),
        var args: Map<String,Any> = mutableMapOf(),
        var env: Map<String,String> = mutableMapOf())


