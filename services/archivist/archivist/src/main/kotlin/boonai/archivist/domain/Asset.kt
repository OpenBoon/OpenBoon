package boonai.archivist.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.convertValue
import com.google.common.hash.Hashing
import boonai.archivist.security.getProjectId
import boonai.archivist.util.randomString
import boonai.common.util.Json
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import org.elasticsearch.action.search.ClearScrollRequest
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.action.search.SearchScrollRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.search.SearchHit
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.Base64
import java.util.Spliterator
import java.util.UUID
import java.util.function.Consumer
import java.util.regex.Pattern

@ApiModel(
    "AssetState",
    description = "Describes the different states can asset can be in."
)
enum class AssetState {

    @ApiModelProperty("The Asset been created in the database but is pending analysis.")
    Pending,

    @ApiModelProperty("The Asset has been analyzed and augmented with fields.")
    Analyzed
}

@ApiModel(
    "Asset Spec",
    description = "Defines all the properties required to create an Asset."
)
class AssetSpec(

    @ApiModelProperty("The URI location of the asset.")
    var uri: String,

    // These cannot be sent over-wire.
    @JsonIgnore
    @ApiModelProperty("Additional metadata fields to add to the Asset in key/value format.")
    var attrs: Map<String, Any>? = null,

    @ApiModelProperty("Optional page number to extract.")
    private val page: Int? = null,

    @ApiModelProperty("Optional Model label which puts the asset in the given Model.")
    val label: Label? = null,

    @ApiModelProperty("An optional unique ID for the asset to override the auto-generated ID.")
    val id: String? = null,

    @ApiModelProperty("A hidden option for sending the entire composed document.  ", hidden = true)
    val document: MutableMap<String, Any>? = null,

    @JsonIgnore
    @ApiModelProperty("A checksum for the file.", hidden = true)
    var checksum: Int? = null,

    @JsonIgnore
    var parentAsset: Asset? = null,

    @ApiModelProperty("Additional metadata fields to add to the custom namespace")
    var custom: Map<String, Any>? = null,

    @ApiModelProperty("Temp unindexed attributes that get removed after the asset is processed.")
    var tmp: Map<String, Any>? = null,

    @ApiModelProperty("A list of languages to use for language specific ops")
    var languages: List<String>? = null

) {
    @JsonIgnore
    fun getChecksumValue(): Int? {
        return checksum ?: parentAsset?.getAttr("source.checksum", Int::class.java)
    }

    @JsonIgnore
    fun getPageNumber(): Int {
        return page?.coerceAtLeast(1)
            ?: if (document != null) {
                val asset = Asset(document)
                asset.getAttr("media.pageNumber", Int::class.java) ?: 1
            } else {
                1
            }
    }

    @JsonIgnore
    fun getRealPath(): String {
        return parentAsset?.getAttr("source.path") ?: uri
    }

    /**
     * Get the Spec for page one which can be used to generate a page stack id.
     */
    @JsonIgnore
    fun makePageOne(): AssetSpec {
        return AssetSpec(getRealPath(), page = 1, id = id, checksum = checksum)
    }

    /**
     * Apply a checksum to ID generation.
     */
    fun makeChecksum(bytes: ByteArray) {
        val hf = Hashing.adler32()
        this.checksum = hf.hashBytes(bytes).asInt()
    }
}

@ApiModel("Asset Counters", description = "Stores the types of asset counters we keep track off.")
class AssetCounters(

    @ApiModelProperty("Total number of assets.")
    val total: Int
)

@ApiModel(
    "Asset",
    description = "The file information and all the metadata generated during Analysis."
)
open class Asset(

    @ApiModelProperty("The unique ID of the Asset.")
    val id: String,

    @ApiModelProperty("The assets metadata.")
    val document: MutableMap<String, Any>
) {

    constructor() : this(randomString(24), mutableMapOf())

    constructor(id: String) : this(id, mutableMapOf())

    constructor(document: MutableMap<String, Any>) :
        this(randomString(24), document)

    constructor(hit: SearchHit) : this(hit.id, hit.sourceAsMap)

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
    fun <T> getAttr(attr: String, type: TypeReference<T>): T? {
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
            } else {
                (current as MutableMap<String, Any?>)[key] = Json.Mapper.convertValue(value)
            }
        } catch (ex: ClassCastException) {
            throw IllegalArgumentException("Invalid attribute: $attr", ex)
        }
    }

    /**
     * Return true if the asset has been analayzed before.
     */
    fun isAnalyzed(): Boolean {
        return attrExists("system.timeCreated") && getAttr<String>("system.state") == AssetState.Analyzed.name
    }

    /**
     * Adds the given labels to the asset's document.
     */
    fun addLabels(labels: Collection<Label>) {
        val allLabels = getAttr("labels", Label.SET_OF) ?: mutableSetOf()
        // Remove the labels first because if the label value
        // changes then it won't get added.  This basically
        // replaces a label for an existing tag.
        allLabels.removeAll(labels)
        allLabels.addAll(labels)
        setAttr("labels", allLabels)
    }

    fun addLabel(label: Label) {
        val allLabels = getAttr("labels", Label.SET_OF) ?: mutableSetOf()
        // Remove the labels first because if the label value
        // changes then it won't get added.  This basically
        // replaces a label for an existing tag.
        allLabels.remove(label)
        allLabels.add(label)
        setAttr("labels", allLabels)
    }

    /**
     * Adds the given labels to the asset's document.
     */
    fun removeLabels(labels: Collection<Label>) {
        val allLabels = getAttr("labels", Label.SET_OF) ?: mutableSetOf()
        allLabels.removeAll(labels)
        setAttr("labels", allLabels)
    }

    fun removeLabel(label: Label) {
        val allLabels = getAttr("labels", Label.SET_OF) ?: mutableSetOf()
        allLabels.remove(label)
        setAttr("labels", allLabels)
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
        return try {
            (`object` as Map<String, Any>)[key]
        } catch (ex: ClassCastException) {
            null
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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Asset) return false

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    companion object {

        private val logger = LoggerFactory.getLogger(Asset::class.java)

        private val PATTERN_ATTR = Pattern.compile(Attr.DELIMITER, Pattern.LITERAL)
    }
}

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

/**
 * A utility class for building unique Asset ids.
 *
 * @property spec: The AssetSpec
 */
class AssetIdBuilder(val spec: AssetSpec) {

    val projectId = getProjectId()
    var length = 32

    /**
     * Convert the givne UUID into a byte array.
     */
    private fun uuidToByteArray(uuid: UUID): ByteBuffer {
        val bb: ByteBuffer = ByteBuffer.wrap(ByteArray(16))
        bb.putLong(uuid.mostSignificantBits)
        bb.putLong(uuid.leastSignificantBits)
        return bb.asReadOnlyBuffer()
    }

    /**
     * Build and return the unique ID.
     */
    fun build(): String {
        if (spec.id != null) {
            return spec.id
        }

        /**
         * Nothing about the order of these statements
         * can ever change or duplicate assets will be
         * created.
         */
        val digester = MessageDigest.getInstance("SHA-256")
        digester.update(spec.uri.toByteArray())
        digester.update(uuidToByteArray(projectId))
        digester.update(spec.getPageNumber().toString().toByteArray())

        spec.getChecksumValue()?.let {
            val buf = ByteBuffer.allocate(4)
            buf.putInt(it)
            digester.update(buf.array())
        }

        // Clamp the size to 32, 48 is bit much and you still
        // get much better resolution than a UUID.  We could
        // also up it on shared indexes but probably not necessary.
        return Base64.getUrlEncoder()
            .encodeToString(digester.digest()).trim('=').substring(0, length)
    }
}

class AssetIterator(
    private val client: RestHighLevelClient,
    private val rsp: SearchResponse,
    private var maxResults: Long
) : Iterable<Asset> {

    override fun iterator(): Iterator<Asset> {
        return object : Iterator<Asset> {

            var hits = rsp.hits.hits
            private var index = 0
            private var count = 0

            init {
                if (maxResults == 0L) {
                    maxResults = rsp.hits.totalHits.value
                }
            }

            override fun hasNext(): Boolean {
                if (index >= hits.size) {
                    val sr = SearchScrollRequest()
                    sr.scrollId(rsp.scrollId)
                    sr.scroll("1m")
                    hits = client.scroll(sr, RequestOptions.DEFAULT).hits.hits
                    index = 0
                }

                val hasMore = index < hits.size && count < maxResults
                if (!hasMore) {
                    var csr = ClearScrollRequest()
                    csr.addScrollId(rsp.scrollId)
                    client.clearScroll(csr, RequestOptions.DEFAULT)
                }
                return hasMore
            }

            override fun next(): Asset {
                val hit = hits[index++]
                val asset = Asset(
                    hit.id,
                    hit.sourceAsMap
                )

                count++
                return asset
            }

            override fun forEachRemaining(action: Consumer<in Asset>) {
                throw UnsupportedOperationException()
            }
        }
    }

    override fun forEach(action: Consumer<in Asset>) {
        throw UnsupportedOperationException()
    }

    override fun spliterator(): Spliterator<Asset> {
        throw UnsupportedOperationException()
    }
}
