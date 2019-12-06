package com.zorroa.archivist.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.convertValue
import com.zorroa.archivist.security.getProjectId
import com.zorroa.archivist.util.Json
import com.zorroa.archivist.util.randomString
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import org.slf4j.LoggerFactory
import org.springframework.web.multipart.MultipartFile
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.Base64
import java.util.UUID
import java.util.regex.Pattern

@ApiModel("AssetState",
    description = "Describes the different states can asset can be in.")
enum class AssetState {

    @ApiModelProperty("The Asset been created in the database but is pending analysis.")
    Pending,

    @ApiModelProperty("The Asset has been analyzed and augmented with fields.")
    Analyzed
}

@ApiModel("Batch Asset Op Status",
    description = "Used to describe the result of a batch asset operation")
class BatchAssetOpStatus(

    @ApiModelProperty("The ID of the asset.")
    val assetId: String,

    @ApiModelProperty("A failure message will be set if the operation filed.")
    val failureMessage: String?=null
) {

    @ApiModelProperty("True of the operation failed.")
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

    @ApiModelProperty("The list of assets to be created")
    val assets: List<AssetSpec>,

    @ApiModelProperty("Set to true if the assets should undergo " +
        "further analysis, or false to stay in the provisioned state.")
    val analyze: Boolean = true,

    @ApiModelProperty("The analysis to apply.")
    val analysis: List<String>? = null,

    @JsonIgnore
    @ApiModelProperty("The task that is creating the assets")
    val task: InternalTask? = null
)

@ApiModel("Batch Provision Assets Response",
    description = "The response returned after provisioning assets.")
class BatchCreateAssetsResponse(

    @ApiModelProperty("The initial state of the assets added to the database.")
    var assets: List<Asset>,

    @ApiModelProperty("A map of the assetId to provisioned status. " +
        "An asset will fail to provision if it already exists.")
    val status: MutableList<BatchAssetOpStatus> = mutableListOf(),

    @ApiModelProperty("The ID of the analysis job, if analysis was selected")
    var jobId: UUID? = null
)

@ApiModel(
    "Batch Upload Assets Request",
    description = "Defines the properties required to batch upload a list of assets."
)
class BatchUploadAssetsRequest(

    @ApiModelProperty("A list of AssetSpec objects which define the Assets starting metadata.")
    var assets: List<AssetSpec>,

    @ApiModelProperty(
        "Set to true if the assets should undergo " +
            "further analysis, or false to stay in the provisioned state."
    )
    val analyze: Boolean = true,

    @ApiModelProperty("The analysis to apply.")
    val analysis: List<String>? = null
) {

    lateinit var files: Array<MultipartFile>
}

@ApiModel("Asset Spec",
    description = "Defines all the properties required to create an Asset.")
class AssetSpec(

    @ApiModelProperty("The URI location of the asset.")
    var uri: String,

    @ApiModelProperty("Additional metadata fields to add to the Asset in key/value format.")
    var attrs: Map<String, Any>? = null,

    @ApiModelProperty("An optional unique ID for the asset to override an auto-generated ID.")
    val id: String? = null
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
            if (value == null) {
                (current as MutableMap<String, Any?>)[key] = null
            }
            else {
                (current as MutableMap<String, Any?>)[key] = Json.Mapper.convertValue(value)
            }
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

@ApiModel("Asset Search", description = "Stores an Asset search, is modeled after an ElasticSearch request.")
class AssetSearch(

    @ApiModelProperty(
        "The search to execute",
        reference = "https://www.elastic.co/guide/en/elasticsearch/reference/6.4/query-filter-context.html"
    )
    val search: Map<String, Any>? = null,

    @ApiModelProperty(
        "A query to execute on deep elements",
        reference = "https://www.elastic.co/guide/en/elasticsearch/reference/6.4/query-filter-context.html"
    )
    val deepQuery: Map<String, Any>? = null
)

object Attr {

    const val DELIMITER = "."

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
}

class AssetIdBuilder(val spec: AssetSpec, val randomness: String? = null) {

    private val projectId = getProjectId()
    private var dataSourceId: UUID? = null
    private var length = 32

    private fun uuidToByteArray(uuid: UUID): ByteBuffer {
        val bb: ByteBuffer = ByteBuffer.wrap(ByteArray(16))
        bb.putLong(uuid.mostSignificantBits)
        bb.putLong(uuid.leastSignificantBits)
        return bb.asReadOnlyBuffer()
    }

    fun dataSource(dataSource: UUID?): AssetIdBuilder {
        dataSource?.let { this.dataSourceId = it }
        return this
    }

    fun build(): String {
        if (spec.id != null) {
            return spec.id
        }

        val digester = MessageDigest.getInstance("SHA-256")
        digester.update(spec.uri.toByteArray())
        digester.update(uuidToByteArray(projectId))
        dataSourceId?.let {
            digester.update(uuidToByteArray(it))
        }
        // TODO: clip tokens.
        randomness?.let {
            digester.update(randomness.toByteArray())
        }
        // Clamp the size to 32, 48 is bit much and you still
        // get much better resolution than a UUID.  We could
        // also up it on shared indexes but probably not necessary.
        return Base64.getUrlEncoder()
            .encodeToString(digester.digest()).trim('=').substring(0, length)
    }
}
