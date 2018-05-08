package com.zorroa.archivist.web.api

import com.zorroa.archivist.repository.TaskDao
import com.zorroa.archivist.service.JobExecutorService
import com.zorroa.sdk.util.Json
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.InputStreamResource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.*
import java.util.concurrent.ExecutionException

@PreAuthorize("hasAuthority(T(com.zorroa.archivist.sdk.security.Groups).DEV) || hasAuthority(T(com.zorroa.archivist.sdk.security.Groups).ADMIN)")
@RestController
class TaskController @Autowired constructor(
        private val jobExecutorService: JobExecutorService,
        private val taskDao: TaskDao
) {

    @GetMapping(value = ["/api/v1/tasks/{id}/_log"])
    @ResponseBody
    @Throws(ExecutionException::class, IOException::class)
    fun streamLog(@PathVariable id: UUID): ResponseEntity<InputStreamResource> {
        val logFile = File(taskDao.getExecutableTask(id).getLogPath())

        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("text/plain"))
                .contentLength(logFile.length())
                .body(InputStreamResource(FileInputStream(logFile)))
    }

    @PutMapping(value = ["/api/v1/tasks/{id}/_retry"])
    @ResponseBody
    @Throws(ExecutionException::class, IOException::class)
    fun retry(@PathVariable id: UUID) {
        jobExecutorService.retryTask(taskDao.get(id))
    }

    @PutMapping(value = ["/api/v1/tasks/{id}/_skip"])
    @ResponseBody
    @Throws(ExecutionException::class, IOException::class)
    fun skip(@PathVariable id: UUID) {
        jobExecutorService.skipTask(taskDao.get(id))
    }

    @GetMapping(value = ["/api/v1/tasks/{id}/_script"])
    @ResponseBody
    @Throws(ExecutionException::class, IOException::class)
    fun getScript(@PathVariable id: UUID): String {
        return Json.prettyString(Json.Mapper.readValue(
                File(taskDao.getExecutableTask(id).getScriptPath()), Any::class.java))
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AssetController::class.java)
    }
}
