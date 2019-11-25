package com.zorroa.archivist.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.uuid.Generators
import com.fasterxml.uuid.impl.NameBasedGenerator
import com.zorroa.archivist.security.getProjectId
import com.zorroa.archivist.util.Json
import com.zorroa.archivist.util.randomString
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import org.slf4j.LoggerFactory
import java.security.MessageDigest
import java.util.Base64
import java.util.UUID
import java.util.regex.Pattern

@ApiModel("Batch Asset Op Status",
    description = "Used to describe the result of a batch asset operation")
class BatchAssetOpStatus(
    val assetId: String,
    val failureMessage: String?=null
) {
    val failed : Boolean = failureMessage != null
}


@ApiModel("Batch Index Assets Request",
    description = "Defines the properties necessary to reindex a batch of Assets.")
class BatchUpdateAssetsRequest(

    @ApiModelProperty("The list of assets to be indexed.  The assets must already exist.")
    val assets: List<Asset>,

    @ApiModelProperty("Set to true if the batch should be flushed immedately.")
    val resfresh: Boolean = false

)

@ApiModel("Batch Index Assets Response",
    description = "Defines the properties necessary to index a batch of assets. ")
class BatchUpdateAssetsResponse(size: Int) {

    @ApiModelProperty("A map of the assetId to indexed status. " +
        "An asset will fail to index unless it already exists")
    val status: Array<BatchAssetOpStatus?> = arrayOfNulls<BatchAssetOpStatus?>(size)
}

@ApiModel("Batch Create Assets Request",
    description = "Defines the properties necessary to provision a batch of assets.")
class BatchCreateAssetsRequest(

    @ApiModelProperty("The list of assets to be provisioned")
    val assets: List<AssetSpec>,

    @ApiModelProperty("Set to true if the assets should undergo " +
        "further analysis, or false to stay in the provisioned state.")
    val analyze: Boolean = true,

    @ApiModelProperty("The analysis to apply.")
    val analysis: List<String>? = null
)


@ApiModel("Batch Provision Assets Response",
    description = "The response returned after provisioning assets.")
class BatchCreateAssetsResponse(

    @ApiModelProperty("A map of the assetId to provisioned status. " +
        "An asset will fail to provision if it already exists.")
    val status: MutableList<BatchAssetOpStatus> = mutableListOf(),

    @ApiModelProperty("The last of assets that was provisioned")
    var assets: List<Asset> = mutableListOf(),

    @ApiModelProperty("The ID of the analysis job, if analysis was selected")
    var jobId: UUID? = null
)

@ApiModel("Asset Spec",
    description = "Defines all the properties required to create an Asset.")
class AssetSpec(

    @ApiModelProperty("The URI location of the asset.")
    var uri: String,

    @ApiModelProperty("Additional metadata fields to add to the Asset.")
    var document: Map<String, Any>? = null,

    @ApiModelProperty("An optional unique ID for the asset to override an auto-generated ID.")
    var id: String? = null
)

@ApiModel("Asset Counters", description = "Stores the types of asset counters we keep track off.")
class AssetCounters(

    @ApiModelProperty("Total number of assets.")
    val total: Int = 0,

    @ApiModelProperty("Total error count.")
    val errors: Int = 0,

    @ApiModelProperty("Total number of warnings")
    val warnings: Int = 0,

    @ApiModelProperty("Total number of assets created.")
    val created: Int = 0,

    @ApiModelProperty("Total number of assets replaced")
    val replaced: Int = 0
)

@ApiModel("Asset",
    description = "The file information and all the metadata generated during Analysis.")
open class Asset(

    @ApiModelProperty("The unique ID of the Asset.")
    val id: String,

    @ApiModelProperty("The assets metadata.")
    val document: Map<String, Any> = mutableMapOf()
) {

    constructor() : this(randomString(24))

    constructor(document: Map<String, Any>) :
        this(randomString(24), document)

    /**
     * Remove an attribute.  If the attr cannot be remove it is set to null.
     *
     * @param attr
     */
    fun removeAttr(attr: String): Boolean {
        val current = getContainer(attr, true)
        val key = Attr.name(attr)

        /*
         * Finally, just try treating it like a map.
         */
        try {
            return (current as MutableMap<String, Any>).remove(key) != null
        } catch (ex: ClassCastException) {
            throw IllegalArgumentException("Invalid attribute: $attr")
        }
    }

    /**
     * Get an attribute value  by its fully qualified name.
     *
     * @param attr
     * @param <T>
     * @return
    </T> */
    fun <T> getAttr(attr: String): T? {
        val current = getContainer(attr, false)
        return getChild(current, Attr.name(attr)) as T?
    }

    /**
     * Return true if the attribute is not null.
     *
     * @param attr The attr name
     * @return true if attr exists
     */
    fun attrExists(attr: String): Boolean {
        val current = getContainer(attr, false)
        return getChild(current, Attr.name(attr)) != null
    }

    /**
     * Get an attribute value by its fully qualified name.  Uses a JSON mapper
     * to map the data into the specified class.
     *
     * @param attr
     * @param type
     * @param <T>
     * @return
    </T> */
    fun <T> getAttr(attr: String, type: Class<T>): T? {
        val current = getContainer(attr, false)
        return Json.Mapper.convertValue(getChild(current, Attr.name(attr)), type)
    }

    /**
     * Get an attribute value by its fully qualified name.  Uses a JSON mapper
     * to map the data into the specified TypeReference.
     *
     * @param attr
     * @param type
     * @param <T>
     * @return
    </T> */
    fun <T> getAttr(attr: String, type: TypeReference<T>): T {
        val current = getContainer(attr, false)
        return Json.Mapper.convertValue(getChild(current, Attr.name(attr)), type)
    }

    /**
     * Set an attribute value.
     *
     * @param attr
     * @param value
     */
    fun setAttr(attr: String, value: Any?) {
        val current = getContainer(attr, true)
        val key = Attr.name(attr)

        try {
            (current as MutableMap<String, Any?>)[key] = Json.Mapper.convertValue(value as Any)
        } catch (ex: ClassCastException) {
            throw IllegalArgumentException("Invalid attribute: $attr", ex)
        }
    }

    private fun getContainer(attr: String, forceExpand: Boolean): Any? {
        val parts = PATTERN_ATTR.split(attr)

        var current: Any? = document
        for (i in 0 until parts.size - 1) {

            var child = getChild(current, parts[i])
            if (child == null) {
                if (forceExpand) {
                    child = createChild(current, parts[i])
                } else {
                    return null
                }
            }
            current = child
        }
        return current
    }

    private fun getChild(`object`: Any?, key: String): Any? {
        if (`object` == null) {
            return null
        }
        try {
            return (`object` as Map<String, Any>)[key]
        } catch (ex: ClassCastException) {
            return null
        }
    }

    private fun createChild(parent: Any?, key: String): Any {
        val result = mutableMapOf<String, Any>()
        try {
            (parent as MutableMap<String, Any>)[key] = result
        } catch (ex: ClassCastException) {
            throw IllegalArgumentException("Invalid attribute: $key parent: $parent")
        }
        return result
    }

    override fun toString(): String {
        return "<Asset $id - $document>"
    }

    companion object {

        private val logger = LoggerFactory.getLogger(Asset::class.java)

        private val PATTERN_ATTR = Pattern.compile(Attr.DELIMITER, Pattern.LITERAL)
    }
}

object Attr {

    const val DELIMITER = "."

    /**
     * A convenience method which takes a variable list of strings and
     * turns it into an attribute name.  This is preferred over using
     * string concatenation.
     *
     * @param name
     * @return
     */
    fun attr(vararg name: String): String {
        return name.joinToString(DELIMITER)
    }

    /**
     * Return the last part of an attribute string.  For example, if fully qualified
     * name is "a:b:c:d", this method will return "d".
     *
     * @param attr
     * @return
     */
    fun name(attr: String): String {
        return attr.substring(attr.lastIndexOf(DELIMITER) + 1)
    }

    /**
     * Return the fully qualified namespace for the attribute.  For example, if
     * the attribute is "a:b:c:d", this method will return "a:b:c"
     *
     * @param attr
     * @return
     */
    fun namespace(attr: String): String {
        return attr.substring(0, attr.lastIndexOf(DELIMITER))
    }
}

object IdGen {

    /**
     * Generate an UUID string utilizing the given value.
     *
     * @param value
     * @return
     */
    fun getId(value: String, idKey: String?=null): String {
        val sb = StringBuilder(128)
        sb.append(getProjectId().toString().replace("-", ""))
        sb.append(value)
        idKey?.let {
            sb.append(idKey)
        }

        val digester = MessageDigest.getInstance("SHA-1")
        digester.update(sb.toString().toByteArray())
        return Base64.getEncoder().encodeToString(digester.digest()).trim('=')
    }
}

