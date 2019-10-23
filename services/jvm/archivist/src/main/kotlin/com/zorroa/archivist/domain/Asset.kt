package com.zorroa.archivist.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.uuid.Generators
import com.fasterxml.uuid.impl.NameBasedGenerator
import com.google.common.base.MoreObjects
import com.zorroa.archivist.search.AssetSearch
import com.zorroa.common.domain.ArchivistWriteException
import com.zorroa.common.domain.EntityNotFoundException
import com.zorroa.common.util.Json
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import org.slf4j.LoggerFactory
import org.springframework.security.access.AccessDeniedException
import java.net.URI
import java.util.UUID
import java.util.regex.Pattern

/**
 * The response from a request to create an asset from an uploaded source.
 * @property assetId The assetId that was created
 * @property uri The URI where the source file was placed.
 */
class AssetUploadedResponse(
    val assetId: UUID,
    val uri: URI
)

@ApiModel("Update Asset Request", description = "Request structure to update an Asset.")
class UpdateAssetRequest(

    @ApiModelProperty("Key/vaue pairs to be updated.")
    val update: Map<String, Any>? = null,

    @ApiModelProperty("Array of fields to remove.")
    val remove: List<String>? = null,

    @ApiModelProperty("Append values to a list attribute.")
    val appendToList: Map<String, Any>? = null,

    @ApiModelProperty("Append values to a list attribute and ensure the result is unique.")
    val appendToUniqueList: Map<String, Any>? = null,

    @ApiModelProperty("Remove values from a list attribute.")
    val removeFromList: Map<String, Any>? = null,

    @JsonIgnore
    @ApiModelProperty("Allow systems vars to be set. Default to false. Cannot be set over wire.")
    val allowSystem: Boolean = false
)

@ApiModel("Batch Update Assets Request", description = "Defines how to batch update a list of assets.")
class BatchUpdateAssetsRequest(
    @ApiModelProperty(
        "Attributes to update. They should be in dot notation. " +
            "Example: { \"foo.bar\" : 1, \"source.ext\": \"png\"}"
    ) val batch: Map<String, UpdateAssetRequest>
) {
    override fun toString(): String {
        return "<BatchUpdateAssetRequest update='$batch'>"
    }
}

@ApiModel(
    "Batch Update Assets Response", description = "Response object for batch updating large numbers of " +
        "assets. Batch updates are able to edit individual attributes however the entire document is rewritten."
)
class BatchUpdateAssetsResponse {

    @ApiModelProperty("UUIDs of updated Assets.")
    val updatedAssetIds = mutableSetOf<String>()

    @ApiModelProperty("UUIDs of Assets that encountered errors.")
    val erroredAssetIds = mutableSetOf<String>()

    @ApiModelProperty("UUIDs of Assets that were denied write access.")
    val accessDeniedAssetIds = mutableSetOf<String>()

    operator fun plus(other: BatchUpdateAssetsResponse) {
        updatedAssetIds.addAll(other.updatedAssetIds)
        erroredAssetIds.addAll(other.erroredAssetIds)
        accessDeniedAssetIds.addAll(other.accessDeniedAssetIds)
    }

    /**
     * Add the counts from an BatchIndexAssetsResponse to this object.
     */
    operator fun plus(other: BatchIndexAssetsResponse) {
        updatedAssetIds.addAll(other.replacedAssetIds)
        updatedAssetIds.addAll(other.replacedAssetIds)
        erroredAssetIds.addAll(other.erroredAssetIds)
    }

    override fun toString(): String {
        return "<BatchUpdateAssetsResponse " +
            "updated=${updatedAssetIds.size} " +
            "errored=${erroredAssetIds.size} " +
            "accessDenied=${accessDeniedAssetIds.size}"
    }

    @JsonIgnore
    fun getThrowableError(): Throwable {
        return when {
            accessDeniedAssetIds.isNotEmpty() -> AccessDeniedException(
                "Access denied updating assets: $accessDeniedAssetIds"
            )
            erroredAssetIds.isNotEmpty() -> EntityNotFoundException(
                "Cannot update missing assets: $erroredAssetIds"
            )
            // Should never get here.
            else -> ArchivistWriteException("Unspecified update exception")
        }
    }

    @JsonIgnore
    fun isSuccess(): Boolean {
        return updatedAssetIds.isNotEmpty() &&
            erroredAssetIds.isEmpty() && accessDeniedAssetIds.isEmpty()
    }
}

@ApiModel("Batch Update Permissions Response", description = "Response object for a BatchUpdatePermissionsRequest.")
class BatchUpdatePermissionsResponse {

    @ApiModelProperty("UUIDs of Assets that were updated.")
    val updatedAssetIds = mutableSetOf<String>()

    @ApiModelProperty("Errors that occurred while processing.")
    val errors = mutableMapOf<String, String>()

    operator fun plus(other: BatchIndexAssetsResponse) {
        updatedAssetIds.addAll(other.replacedAssetIds)
    }
}

@ApiModel("Batch Create Assets Request", description = "Structure for upserting a batch of assets.")
class BatchCreateAssetsRequest(

    @ApiModelProperty("List of Documents to upsert.")
    val sources: List<Document>,

    @ApiModelProperty("UUID of the Job doing the upsert.")
    val jobId: UUID?,

    @ApiModelProperty("UUID of the Task doing to upsert.")
    val taskId: UUID?
) {

    /**
     * A convenience constructor for unit tests.
     */
    constructor(doc: Document) : this(listOf(doc))

    @JsonIgnore
    var skipAssetPrep = false

    @JsonIgnore
    var scope = "index"

    var isUpload = false

    constructor(sources: List<Document>, scope: String = "index", skipAssetPrep: Boolean = false) :
        this(sources, null, null) {
        this.scope = scope
        this.skipAssetPrep = skipAssetPrep
    }
}

@ApiModel("Batch Create Assets Response", description = "The response after batch creating an array of assets.")
class BatchIndexAssetsResponse(val total: Int) {

    @ApiModelProperty("UUIDs of Assets that were created.")
    var createdAssetIds = mutableSetOf<String>()

    @ApiModelProperty("UUIDs of Assets that were replaced.")
    var replacedAssetIds = mutableSetOf<String>()

    @ApiModelProperty("UUIDs of Assets that had errors.")
    var erroredAssetIds = mutableSetOf<String>()

    @ApiModelProperty("UUIDs of Assets that had warnings.")
    var warningAssetIds = mutableSetOf<String>()

    @ApiModelProperty("Number of retries it took finish this batch request.")
    var retryCount = 0

    fun add(other: BatchIndexAssetsResponse): BatchIndexAssetsResponse {
        createdAssetIds.addAll(other.createdAssetIds)
        replacedAssetIds.addAll(other.replacedAssetIds)
        erroredAssetIds.addAll(other.erroredAssetIds)
        warningAssetIds.addAll(other.warningAssetIds)
        retryCount += other.retryCount
        return this
    }

    /**
     * Return true if assets were created or replaced.
     */
    fun assetsChanged(): Boolean {
        return createdAssetIds.isNotEmpty() || replacedAssetIds.isNotEmpty()
    }

    /**
     * Return an AssetCounters instance for incrementing job and task counts.
     */
    fun getAssetCounters(): AssetCounters {
        return AssetCounters(
            created = createdAssetIds.size,
            replaced = replacedAssetIds.size,
            errors = erroredAssetIds.size,
            warnings = warningAssetIds.size
        )
    }

    override fun toString(): String {
        return MoreObjects.toStringHelper(this)
            .add("created", createdAssetIds.size)
            .add("replaced", replacedAssetIds.size)
            .add("warnings", warningAssetIds)
            .add("errors", erroredAssetIds.size)
            .add("retries", retryCount)
            .toString()
    }
}

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

@ApiModel("Batch Delete Assets Request", description = "Describes a request to delete Assets.")
class BatchDeleteAssetsRequest(
    @ApiModelProperty("UUIDs of Assets to delete.") val assetIds: List<String>
)

@ApiModel("Batch Delete Assets Response", description = "Response returned when Assets are deleted.")
class BatchDeleteAssetsResponse(
    @ApiModelProperty("Number of assets requested to be deleted. Includes the resolved number of clips.")
    var totalRequested: Int = 0,

    @ApiModelProperty("Number of assets deleted.")
    var deletedAssetIds: MutableSet<String> = mutableSetOf(),

    @ApiModelProperty("UUIDS o Assets skipped due to being on hold.")
    var onHoldAssetIds: MutableSet<String> = mutableSetOf(),

    @ApiModelProperty("UUIDs of Assets skipped due to permissions")
    var accessDeniedAssetIds: MutableSet<String> = mutableSetOf(),

    @ApiModelProperty("UUIDS of Assets that have already been deleted.")
    var missingAssetIds: MutableSet<String> = mutableSetOf(),

    @ApiModelProperty("Map of AssetID/Message for all errors encountered.")
    var errors: MutableMap<String, String> = mutableMapOf()
) {

    operator fun plus(other: BatchDeleteAssetsResponse) {
        totalRequested += other.totalRequested
        deletedAssetIds.addAll(other.deletedAssetIds)
        onHoldAssetIds.addAll(other.onHoldAssetIds)
        accessDeniedAssetIds.addAll(other.accessDeniedAssetIds)
        missingAssetIds.addAll(other.missingAssetIds)
        errors.putAll(other.errors)
    }
}

enum class AssetState {
    /**
     * The default state for an Asset.
     */
    Active,

    /**
     * The asset has been deleted from ES.
     */
    Deleted
}

/**
 * The ES document
 */
@ApiModel("Document", description = "Represents an Elasticsearch (ES) document.")
open class Document {

    @ApiModelProperty("Contents of the Document.")
    var document: Map<String, Any>

    @ApiModelProperty("UUID of the Document.")
    var id: String = UUID.randomUUID().toString()

    @ApiModelProperty("Type of the Document.")
    var type = "asset"

    @ApiModelProperty("Permissions associated with the Document.")
    var permissions: MutableMap<String, Int>? = null

    @ApiModelProperty("Links associated with the Document.")
    var links: MutableList<Tuple<String, Any>>? = null

    @ApiModelProperty("Search result score.")
    var score: Float? = null

    @ApiModelProperty(hidden = true)
    var replace = false

    constructor() {
        document = mutableMapOf()
    }

    constructor(doc: Document) {
        this.id = doc.id
        this.type = doc.type
        this.document = doc.document
    }

    constructor(id: String, doc: Map<String, Any>) {
        this.id = id
        this.document = doc
    }

    constructor(id: String) {
        this.id = id
        this.document = mutableMapOf()
    }

    constructor(id: UUID) {
        this.id = id.toString()
        this.document = mutableMapOf()
    }

    constructor(doc: Map<String, Any>) {
        this.document = doc
    }

    fun addToLinks(type: String, id: Any): Document {
        if (links == null) {
            links = mutableListOf()
        }
        links?.apply {
            this.add(Tuple(type, id))
        }
        return this
    }

    fun addToPermissions(group: String, access: Int): Document {
        if (permissions == null) {
            permissions = mutableMapOf()
        }
        permissions?.apply {
            this[group] = access
        }
        return this
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
     * Assumes the target attribute is a collection of some sort and tries to add
     * the given value.
     *
     * @param attr The attr name in dot notation, must point to a collection.
     * @param value The value to append to the collection.
     * @param unique If true, uniquify the collection.
     * @return Return true if the value was added to the collection.
     */
    fun addToAttr(attr: String, value: Any, unique: Boolean = true): Boolean {
        val key = Attr.name(attr)

        try {
            var container = getContainer(attr, true) as MutableMap<String, Any>
            var collection = container[key] as MutableCollection<Any?>?
            if (collection == null) {
                collection = mutableListOf()
                container[key] = collection
            }

            val res = if (value is Collection<*>) {
                collection.addAll(value)
            } else {
                collection.add(value)
            }

            if (unique) {
                val uniqList = collection.distinct()
                collection.clear()
                collection.addAll(uniqList)
            }

            return res
        } catch (e: Exception) {
            logger.warn("Unable to append to attr '$attr', it maybe not be a collection")
        }

        return false
    }

    /**
     * Assumes the target attribute is a collection of some sort and tries to remove
     * the given value.
     *
     * @param attr The attr name in dot notation, must point to a collection.
     * @param value The value to remove from the collection.
     */
    fun removeFromAttr(attr: String, value: Any, unique: Boolean = true): Boolean {
        val key = Attr.name(attr)

        try {
            var container = getContainer(attr, true) as MutableMap<String, Any>
            var collection = container[key] as MutableCollection<Any?>?
            if (collection == null) {
                collection = mutableListOf()
                container[key] = collection
            }

            return if (value is Collection<*>) {
                collection.removeAll(value)
            } else {
                collection.remove(value)
            }
        } catch (e: Exception) {
            logger.warn("Unable to remove from attr '$attr', it maybe not be a collection")
        }

        return false
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
     * Return true if the document has the given namespace.
     * @param attr
     * @return
     */
    fun attrExists(attr: String): Boolean {
        val container = getContainer(attr, false)
        return getChild(container, Attr.name(attr)) != null
    }

    /**
     * Return true if the given element is empty.
     *
     * @param attr
     * @return
     */
    fun isEmpty(attr: String): Boolean {
        val container = getContainer(attr, false)
        val child = getChild(container, Attr.name(attr))

        return try {
            val map = child as MutableMap<String, Any>?
            map?.isEmpty() ?: true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Return true if the value of an attribute contains the given value.
     *
     * @param attr
     * @return
     */
    fun attrContains(attr: String, value: Any): Boolean {
        val parent = getContainer(attr, false)
        val child = getChild(parent, Attr.name(attr))

        if (child is Collection<*>) {
            return child.contains(value)
        } else if (child is String) {
            return child.contains(value.toString())
        }
        return false
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
        return "<Document $id - $document>"
    }

    companion object {

        private val logger = LoggerFactory.getLogger(Document::class.java)

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

    private val uuidGenerator = Generators.nameBasedGenerator(NameBasedGenerator.NAMESPACE_URL)

    /**
     * Generate an UUID string utilizing the given value.
     *
     * @param value
     * @return
     */
    fun getId(value: String): String {
        return uuidGenerator.generate(value).toString()
    }

    /**
     * Returns a readable unique string for an asset that is based
     * on the file path and clip properties.
     *
     * @param source
     * @return
     */
    fun getRef(source: Document): String? {
        val idkey = source.getAttr("source.idkey", String::class.java)
        var key = source.getAttr("source.path", String::class.java)

        if (idkey != null) {
            key = "$key?$idkey"
        }
        return key
    }

    /**
     * Return the unique ID for the given source.
     *
     * @param source
     * @return
     */
    fun getId(source: Document): String {
        return uuidGenerator.generate(getRef(source)).toString()
    }
}

/**
 * Copied from IRM source.
 */
enum class DocumentState private constructor(val value: Long) {
    METADATA_UPLOADED(10), DOCUMENT_UPLOADED(20), PROCESSED(30), INDEXED(40), REPROCESS(50);

    companion object {
        fun findByValue(value: Long): DocumentState? {
            for (state in DocumentState.values()) {
                if (state.value == value) {
                    return state
                }
            }
            return null
        }
    }
}

class Tuple<L, R>(val left: L, val right: R)
class MutableTuple<L, R>(var left: L, var right: R)
