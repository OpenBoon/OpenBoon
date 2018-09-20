package com.zorroa.archivist.web.api

import com.zorroa.archivist.domain.TaskEvent
import com.zorroa.archivist.security.getAnalystEndpoint
import com.zorroa.archivist.service.AnalystService
import com.zorroa.archivist.service.DispatcherService
import com.zorroa.archivist.util.HttpUtils
import com.zorroa.common.domain.AnalystSpec
import com.zorroa.common.domain.DispatchTask
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.io.IOException


@RestController
class AnalystClusterController @Autowired constructor(
        val analystService: AnalystService,
        val dispatcherService: DispatcherService) {


    @PostMapping(value = ["/cluster/_ping"])
    fun ping(@RequestBody spec: AnalystSpec) : Any {
        return analystService.upsert(spec)
    }

    class QueueTaskReq(val endpoint: String)

    @PutMapping(value = ["/cluster/_queue"])
    @Throws(IOException::class)
    fun queue() : ResponseEntity<DispatchTask> {
        val endpoint = getAnalystEndpoint() ?: return ResponseEntity(HttpStatus.NOT_FOUND)

        val task = dispatcherService.getNext()
        return if (task == null) {
            ResponseEntity(HttpStatus.NOT_FOUND)
        } else {
            ResponseEntity(task, HttpStatus.OK)
        }
    }

    @PostMapping(value = ["/cluster/_event"])
    @Throws(IOException::class)
    fun event(@RequestBody event: TaskEvent) : Any {
        dispatcherService.handleEvent(event)
        return HttpUtils.status("event", "foo", true)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AnalystClusterController::class.java)
    }
}