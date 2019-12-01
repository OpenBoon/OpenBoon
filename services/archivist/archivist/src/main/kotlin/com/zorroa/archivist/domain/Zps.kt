package com.zorroa.archivist.domain

import com.fasterxml.jackson.core.type.TypeReference
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

fun zpsTaskName(zps: ZpsScript): String {
    if (zps.name == null) {
        val list = mutableListOf<String>()

        zps.generate?.let {
            list.add("Generators=${it.size}")
        }

        zps.execute?.let {
            list.add("Processors=${it.size}")
        }

        zps.assets?.let {
            list.add("Assets=${it.size}")
        }
        return list.joinToString(" ")
    } else {
        return zps.name!!
    }
}

fun emptyZpsScript(name: String): ZpsScript {
    return ZpsScript(
        name,
        null, null, null
    )
}

@ApiModel("ZPS Script", description = "Describes a ZPS script that can be run by the Analysts.")
class ZpsScript(
    @ApiModelProperty("Name of the ZPS Script.")
    var name: String?,

    @ApiModelProperty("List of Processor Refs to add to the 'generate' section of the ZPS Script.")
    var generate: List<ProcessorRef>?,

    @ApiModelProperty("List of Processor Refs to add to the 'over' section of the ZPS Script.")
    var assets: List<Asset>?,

    @ApiModelProperty("List of Processor Refs to add to the 'execute' section of the ZPS Script.")
    var execute: List<ProcessorRef>?,

    @ApiModelProperty("Global arguments to apply to the Processors.")
    var globalArgs: MutableMap<String, Any>? = mutableMapOf(),

    @ApiModelProperty("Type of pipeline to run", allowableValues = "import,batch")
    var type: JobType = JobType.Import,

    @ApiModelProperty("Settings for the run of this ZPS Script.")
    var settings: MutableMap<String, Any?>? = null

    ) {
    /**
     * Set a key/value in the settings map.  If the settings map is
     * null then one is created.
     *
     * @param key: The name of the setting
     * @param value: value for the setting.
     */
    fun setSettting(key: String, value: Any?) {
        if (settings == null) {
            settings = mutableMapOf()
        }
        settings?.let {
            it[key] = value
        }
    }

    /**
     * Set a key/value in the global arg map.  If the arg map is
     * null then one is created.
     *
     * @param key: The name of the arg
     * @param value: value for the arg.
     */
    fun setGlobalArg(key: String, value: Any) {
        if (globalArgs == null) {
            globalArgs = mutableMapOf()
        }
        globalArgs?.let {
            it[key] = value
        }
    }
}

class ZpsError(
    var id: String? = null,
    var path: String? = null,
    var phase: String? = null,
    var message: String? = null,
    var processor: String? = null,
    var className: String? = null,
    var file: String? = null,
    var lineNumber: Int? = null,
    var method: String? = null,
    var skipped: Boolean = false
)

@ApiModel("Zps Filter")
class ZpsFilter(
    var expr: String? = null,
    var drop: Boolean = false
)

@ApiModel("Processor Ref", description = "Describes an instance of Processor that can be run by ZPS.")
class ProcessorRef(

    @ApiModelProperty("Dot-path to the Processor's python class.")
    var className: String,

    @ApiModelProperty("The docker container image.")
    var image: String,

    @ApiModelProperty("Args to pass to the Processor.")
    var args: Map<String, Any>? = mutableMapOf(),

    @ApiModelProperty("List of Processor Refs to execute as part of this Processor Ref.")
    var execute: List<ProcessorRef>? = mutableListOf(),

    @ApiModelProperty("Filters to apply to this Processor Ref.")
    var filters: List<ZpsFilter>? = mutableListOf(),

    @ApiModelProperty("File types to filter on.")
    var fileTypes: List<String>? = mutableListOf(),

    @ApiModelProperty("Envrironment variables that should be present during processor execution.")
    val env: Map<String, String> = mutableMapOf()
)

var LIST_OF_PREFS: TypeReference<List<ProcessorRef>> = object : TypeReference<List<ProcessorRef>>() {}