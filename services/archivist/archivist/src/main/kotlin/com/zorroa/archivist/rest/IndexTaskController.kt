package com.zorroa.archivist.rest

import com.zorroa.archivist.domain.IndexTask
import com.zorroa.archivist.domain.IndexTaskState
import com.zorroa.archivist.repository.IndexTaskDao
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
    val indexTaskDao: IndexTaskDao
) {

    @GetMapping(value = ["/api/v1/index-tasks/{id}"])
    fun get(@PathVariable id: UUID): IndexTask {
        return indexTaskDao.getOne(id)
    }

    @GetMapping(value = ["/api/v1/index-tasks"])
    fun getActive(@PathVariable id: UUID): List<IndexTask> {
        return indexTaskDao.getAllByState(IndexTaskState.RUNNING)
    }
}
