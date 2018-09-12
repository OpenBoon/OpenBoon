package com.zorroa.archivist.domain

import com.zorroa.archivist.search.AssetSearch
import java.util.*


/**
 * Defines fields needed to make new ExportFile.
 */
data class ExportFileSpec (
        var path: String,
        val mimeType: String,
        val size: Long
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
        var env: Map<String,String> = mutableMapOf(),
        var compress: Boolean = true)


