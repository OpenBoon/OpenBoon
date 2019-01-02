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
        val pipelineService: PipelineService

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
                // TODO: check original file name for badness.
                val id = UUID.randomUUID()
                val fss =  fileStorageService.get(
                        FileStorageSpec("asset", id, file.originalFilename as String))
                fileStorageService.write(fss.id, file.bytes)
                mapOf("id" to id.toString(), "path" to fss.uri)
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