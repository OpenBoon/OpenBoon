package boonai.archivist.domain

import com.fasterxml.jackson.core.type.TypeReference
import boonai.common.util.Json
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.util.zip.Adler32
import java.util.zip.Checksum

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

fun emptyZpsScripts(name: String): List<ZpsScript> {
    return listOf(
        ZpsScript(
            name,
            null, null, null
        )
    )
}

@ApiModel("Resolved Pipeline", description = "The result of resolving a Pipeline")
class ResolvedPipeline(

    @ApiModelProperty("The execute portion of the pipeline.")
    val execute: List<ProcessorRef>,

    @ApiModelProperty("Any global args set by pipeline resolution.")
    val globalArgs: MutableMap<String, Any> = mutableMapOf()
)

@ApiModel("ZPS Script", description = "Describes a ZPS script that can be run by the Analysts.")
class ZpsScript(
    @ApiModelProperty("Name of the ZPS Script.")
    var name: String?,

    @ApiModelProperty("List of Processor Refs to add to the 'generate' section of the ZPS Script.")
    var generate: List<ProcessorRef>?,

    @ApiModelProperty("List of Processor Refs to add to the 'assets' section of the ZPS Script.")
    var assets: List<Asset>?,

    @ApiModelProperty("List of Processor Refs to add to the 'execute' section of the ZPS Script.")
    var execute: List<ProcessorRef>?,

    @ApiModelProperty("Global arguments to apply to the Processors.")
    var globalArgs: MutableMap<String, Any>? = mutableMapOf(),

    @ApiModelProperty("Settings for the run of this ZPS Script.")
    var settings: MutableMap<String, Any?>? = null,

    @ApiModelProperty("List of Asset IDs that will be resolved at dispatch time.")
    var assetIds: List<String>? = null,

    @ApiModelProperty("A list of dependent child scripts")
    var children: List<ZpsScript>? = null

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

    fun copyWithoutChildren(): ZpsScript {
        return ZpsScript(name, generate, assets, execute, globalArgs, settings, assetIds)
    }
}

@JsonInclude(JsonInclude.Include.ALWAYS)
class BuildZpsScriptRequest(
    @ApiModelProperty("The ID of the Asset.")
    val assetId: String?,
    @ApiModelProperty("The modules to apply.")
    val modules: List<String>
)

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
    var args: Map<String, Any?>? = mutableMapOf(),

    @ApiModelProperty("List of Processor Refs to execute as part of this Processor Ref.")
    var execute: List<ProcessorRef>? = null,

    @ApiModelProperty("Filters to apply to this Processor Ref.")
    var filters: List<ZpsFilter>? = null,

    @ApiModelProperty("File types to filter on.")
    var fileTypes: List<String>? = null,

    @ApiModelProperty("Environment variables that should be present during processor execution.")
    val env: Map<String, String>? = null,

    @ApiModelProperty("The Pipeline module which added this processor.")
    var module: String = "standard",

    @ApiModelProperty("The Processor name to use for the checksum")
    var checksumName: String? = null,

    @ApiModelProperty("Set to true foe the processor to run even if it's been run.s")
    var force: Boolean = false

) {
    /**
     * Return a arg checksum for the configuration of the processor.
     */
    fun getChecksum(): Long {
        val checksum: Checksum = Adler32()
        checksum.update((checksumName ?: className).toByteArray())
        checksum.update(Json.serialize(args ?: mapOf<String, Any>()))
        return checksum.value
    }

    override fun toString(): String {
        return "ProcessorRef(className='$className', image='$image')"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ProcessorRef) return false

        if (className != other.className) return false
        if (image != other.image) return false

        return true
    }

    override fun hashCode(): Int {
        var result = className.hashCode()
        result = 31 * result + image.hashCode()
        return result
    }

    companion object {
        val LIST_OF: TypeReference<List<ProcessorRef>> = object : TypeReference<List<ProcessorRef>>() {}
    }
}
