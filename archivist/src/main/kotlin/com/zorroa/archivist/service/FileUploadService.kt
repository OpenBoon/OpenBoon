package com.zorroa.archivist.service

import com.zorroa.archivist.domain.*
import com.zorroa.archivist.security.getUser
import com.zorroa.common.domain.Job
import com.zorroa.common.domain.JobSpec
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import java.util.*

interface FileUploadService {
    fun ingest(spec: FileUploadSpec, files: Array<MultipartFile>): Job
}

@Component
class FileUploadServiceImpl @Autowired constructor(
        val fileStorageService: FileStorageService,
        val jobService: JobService,
        val pipelineService: PipelineService,
        val assetService: AssetService

) : FileUploadService {

    /**
     * archivist.pipeline.default-import-pipeline
     */
    override fun ingest(spec: FileUploadSpec, files: Array<MultipartFile>): Job {

        /**
         * Add each file to the storage system.
         */
        val filePaths = files.mapNotNull { file ->
            if (file.originalFilename != null) {
                val rsp = assetService.handleAssetUpload(
                        file.originalFilename as String, file.bytes)
                mapOf("id" to rsp.assetId.toString(), "path" to rsp.uri.toURL())
            }
            else {
                null
            }
        }

        val generate = listOf(
                ProcessorRef("zplugins.core.generators.FileUploadGenerator",
                        mapOf("files" to filePaths)))

        val execute = if (spec.processors.isNullOrEmpty()) {
            pipelineService.resolveDefault(PipelineType.Import)
        }
        else {
            pipelineService.resolve(PipelineType.Import, spec.processors)
        }

        val name = spec.name ?: "File Upload by ${getUser().username}"
        val jspec = JobSpec(name,
                ZpsScript("Generator",
                        generate=generate,
                        over=null,
                        execute=execute))

        return jobService.create(jspec)
    }

}