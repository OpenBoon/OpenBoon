package com.zorroa.cluster.tools

import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import com.google.common.collect.ImmutableList
import com.zorroa.cluster.client.MasterServerClient
import com.zorroa.cluster.thrift.*
import com.zorroa.cluster.zps.MetaZpsExecutor
import com.zorroa.cluster.zps.ZpsTask
import com.zorroa.sdk.processor.Reaction
import com.zorroa.sdk.processor.SharedData
import com.zorroa.sdk.util.Json
import com.zorroa.sdk.zps.ZpsError
import java.io.File
import java.util.concurrent.atomic.AtomicReference


class Args {

    @Parameter(names = ["-t", "--taskFile"], description = "The path to the task file")
    var taskFile: String? = null

    @Parameter(names = ["-s", "--sharedDir"], description = "The path to the shared dir")
    var sharedDir: String? = null

    @Parameter(names = ["-a", "--archivist"], description = "The archivist to talk back to.")
    var clusterHost: String? = null
}

fun main(args: Array<String>) {
    
    Args().let {
        JCommander(it, *args)

        val clusterClient = MasterServerClient(it.clusterHost)
        val zpsTask = Json.Mapper.readValue(File(it.taskFile), ZpsTask::class.java)
        val sharedData = SharedData(it.sharedDir)

        val mzps = MetaZpsExecutor(zpsTask, sharedData)
        mzps.addReactionHandler { zpsTask1, sharedData, reaction ->

            val result = AtomicReference(TaskResultT())

            if (zpsTask.id == 0) {
                if (reaction.response != null) {
                    result.get().setResult(Json.serialize(reaction.response))
                } else if (reaction.error != null) {
                    result.get().addToErrors(newTaskError(reaction.error))
                }
            } else {
                handleZpsReaction(clusterClient, zpsTask1, sharedData, reaction)
            }

        }
        mzps.execute()
    }
}

private fun handleZpsReaction(client: MasterServerClient, zpsTask: ZpsTask, sharedData: SharedData, reaction: Reaction) {

    if (reaction.error != null) {
        client.reportTaskErrors(zpsTask.id, ImmutableList.of(
                newTaskError(reaction.error)))
    }

    if (reaction.expand != null) {
        val script = reaction.expand

        val expand = ExpandT()
        expand.setScript(Json.serialize(script))
        expand.setName(script.name)
        client.expand(zpsTask.id, expand)
    }

    if (reaction.stats != null) {
        val stats = reaction.stats
        client.reportTaskStats(zpsTask.id, TaskStatsT()
                .setErrorCount(stats.errorCount)
                .setSuccessCount(stats.successCount)
                .setWarningCount(stats.warningCount))
    }
}

private fun newTaskError(zpsError: ZpsError): TaskErrorT {
    val error = TaskErrorT()
    error.setMessage(zpsError.message)
    error.setPhase(zpsError.phase)
    error.setProcessor(zpsError.processor)
    error.isSkipped = zpsError.isSkipped
    error.setTimestamp(System.currentTimeMillis())

    error.setStack(ImmutableList.of(StackElementT()
            .setClassName(zpsError.className)
            .setFile(zpsError.file)
            .setLineNumber(zpsError.lineNumber)
            .setMethod(zpsError.method)))

    if (zpsError.origin != null) {
        error.setId(zpsError.id)
        error.setOriginService(zpsError.origin.service)
        error.setOriginPath(zpsError.origin.path)
        error.setPath(zpsError.path)
    }
    return error
}


