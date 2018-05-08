package com.zorroa.archivist.domain

import com.zorroa.sdk.processor.ProcessorRef
import com.zorroa.sdk.search.AssetSearch
import java.util.*

class Export

/**
 * Defines fields needed to make new ExportFile.
 */
data class ExportFileSpec (
        val name: String,
        val mimeType: String,
        val size: Long)

/**
 * An ExportFile record.
 */
data class ExportFile (
        val id: UUID,
        val jobId: UUID,
        val name: String,
        val mimeType : String,
        val size : Long,
        val timeCreated: Long)


/**
 * Defines fields needed to create a new export.
 */
data class ExportSpec (
        val name: String?,
        val search: AssetSearch,
        val processors: List<ProcessorRef> = mutableListOf(),
        val args: Map<String,Any> = mutableMapOf(),
        val compress: Boolean = true)


