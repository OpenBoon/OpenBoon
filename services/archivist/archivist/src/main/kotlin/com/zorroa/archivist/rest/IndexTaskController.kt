package com.zorroa.archivist.rest

import com.zorroa.archivist.domain.IndexTask
import com.zorroa.archivist.domain.IndexTaskState
import com.zorroa.archivist.repository.IndexTaskDao
import com.zorroa.archivist.service.IndexTaskService
import org.elasticsearch.common.Strings
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.Resource
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import springfox.documentation.annotations.ApiIgnore
import java.util.UUID

@PreAuthorize("hasAuthority('SystemManage')")
@RestController
@ApiIgnore
class IndexTaskController @Autowired constructor(
    val indexTaskDao: IndexTaskDao,
    val indexTaskService: IndexTaskService
) {

    @GetMapping(value = ["/api/v1/index-tasks/{id}"])
    fun get(@PathVariable id: UUID): IndexTask {
        return indexTaskDao.getOne(id)
    }

    @GetMapping(value = ["/api/v1/index-tasks/{id}/_es_task_info"])
    fun getEsStatus(@PathVariable id: UUID): ResponseEntity<Resource> {
        val task = indexTaskDao.getOne(id)
        val content = Strings.toString(indexTaskService.getEsTaskInfo(task))
        return ResponseEntity.ok()
            .contentLength(content.length.toLong())
            .body(InputStreamResource(content.byteInputStream()))
    }

    @GetMapping(value = ["/api/v1/index-tasks"])
    fun getRunning(): List<IndexTask> {
        return indexTaskDao.getAllByStateOrderByTimeCreatedDesc(IndexTaskState.RUNNING)
    }
}
