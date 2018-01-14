package com.zorroa.archivist.web.api

import com.zorroa.archivist.repository.TaskDao
import com.zorroa.archivist.service.JobExecutorService
import com.zorroa.sdk.util.Json
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.InputStreamResource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.concurrent.ExecutionException

@PreAuthorize("hasAuthority('group::developer') || hasAuthority('group::administrator')")
@RestController
class TaskController @Autowired constructor(
        private val jobExecutorService: JobExecutorService,
        private val taskDao: TaskDao
) {

    @GetMapping(value = "/api/v1/tasks/{id}/_log")
    @ResponseBody
    @Throws(ExecutionException::class, IOException::class)
    fun streamLog(@PathVariable id: Int): ResponseEntity<InputStreamResource> {
        val logFile = File(taskDao.getExecutableTask(id).getLogPath())

        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("text/plain"))
                .contentLength(logFile.length())
                .body(InputStreamResource(FileInputStream(logFile)))
    }

    @PutMapping(value = "/api/v1/tasks/{id}/_retry")
    @ResponseBody
    @Throws(ExecutionException::class, IOException::class)
    fun retry(@PathVariable id: Int) {
        jobExecutorService.retryTask(taskDao.get(id))
    }

    @PutMapping(value = "/api/v1/tasks/{id}/_skip")
    @ResponseBody
    @Throws(ExecutionException::class, IOException::class)
    fun skip(@PathVariable id: Int) {
        jobExecutorService.skipTask(taskDao.get(id))
    }

    @GetMapping(value = "/api/v1/tasks/{id}/_script")
    @ResponseBody
    @Throws(ExecutionException::class, IOException::class)
    fun getScript(@PathVariable id: Int): String {
        return Json.prettyString(Json.Mapper.readValue(
                File(taskDao.getExecutableTask(id).getScriptPath()), Any::class.java))
    }
}
