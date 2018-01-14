package com.zorroa.archivist.web.api

import com.zorroa.archivist.HttpUtils
import com.zorroa.archivist.domain.Command
import com.zorroa.archivist.service.CommandService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

@RestController
class CommandController @Autowired constructor(
        private val commandService: CommandService
){

    val pending: List<Command>
    @GetMapping(value = "/api/v1/commands")
    get() = commandService!!.getPendingByUser()

    @GetMapping(value = "/api/v1/commands/{id:\\d+}")
    operator fun get(@PathVariable id: Int): Command {
        return commandService!!.get(id)
    }

    @PutMapping(value = "/api/v1/commands/{id:\\d+}/_cancel")
    fun cancel(@PathVariable id: Int): Any {
        val cmd = commandService!!.get(id)
        val canceled = commandService!!.cancel(cmd)
        return HttpUtils.status("command", "cancel", canceled)
    }
}
