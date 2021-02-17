package boonai.archivist.rest

import com.fasterxml.jackson.module.kotlin.readValue
import boonai.archivist.domain.IndexTask
import boonai.archivist.domain.IndexTaskState
import boonai.archivist.repository.IndexTaskDao
import boonai.archivist.service.IndexTaskService
import boonai.common.util.Json
import org.elasticsearch.common.Strings
import org.springframework.beans.factory.annotation.Autowired
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
    fun getEsStatus(@PathVariable id: UUID): Map<String, Any> {
        val task = indexTaskDao.getOne(id)
        val taskinfo = indexTaskService.getEsTaskInfo(task)

        val rsp = Json.Mapper.readValue<MutableMap<String, Any>>(Strings.toString(taskinfo.taskInfo))
        rsp["isCompleted"] = taskinfo.isCompleted
        return rsp
    }

    @GetMapping(value = ["/api/v1/index-tasks"])
    fun getRunning(): List<IndexTask> {
        return indexTaskDao.getAllByStateOrderByTimeCreatedDesc(IndexTaskState.RUNNING)
    }
}
