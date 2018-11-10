package com.zorroa.archivist.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.uuid.Generators
import com.fasterxml.uuid.impl.NameBasedGenerator
import com.google.common.base.MoreObjects
import com.zorroa.common.util.Json
import org.slf4j.LoggerFactory
import java.util.*
import java.util.regex.Pattern

/**
 * Structure for upserting a batch of assets.
 *
 * @property sources: The source documents
 * @property jobId: The associated job Id
 * @property taskID: The associated task Id
 */
class BatchCreateAssetsRequest(
        val sources: List<Document>,
        val jobId: UUID?,
        val taskId: UUID?) {

    constructor( sources: List<Document>) : this(sources, null, null)
}


/**
 * The response after batch creating an array of assets.
 */
class BatchCreateAssetsResponse {
    var tried = 0
    var created = 0
    var replaced = 0
    var warnings = 0
    var retries = 0
    var errors = 0
    var total = 0
    var logs: MutableList<String> = mutableListOf()
    var assetIds: MutableList<String> = mutableListOf()

    fun add(other: BatchCreateAssetsResponse): BatchCreateAssetsResponse {
        tried += other.tried
        created += other.created
        replaced += other.replaced
        warnings += other.warnings
        errors += other.errors
        retries += other.retries
        logs.addAll(other.logs)
        assetIds.addAll(other.assetIds)
        return this
    }

    override fun toString(): String {
        return MoreObjects.toStringHelper(this)
                .add("tried", tried)
                .add("created", created)
                .add("replaced", replaced)
                .add("warnings", warnings)
                .add("errors", errors)
                .add("retries", retries)
                .toString()
    }
}
/**
 * A request to batch delete assets.
 *
 * @property assetIds an array of assetIds to delete.
 */
class BatchDeleteAssetsRequest (
        val assetIds: List<String>
)

/**
 * The response returned when assets are deleted.
 *
 * @property totalRequested The total number of assets requested to be deleted.  This includes the resolved # of clips.
 * @property totalDeleted The total number of assets actually deleted.
 * @property childrenRequested The total number of children deleted.
 * @property onHold Assets skipped due to being on hold.
 * @property accessDenied Assets skipped due to permissions
 * @property missing Assets that have already been deleted.
 * @property failures A map AssetID/Message failures.
 */
class BatchDeleteAssetsResponse (
    var totalRequested: Int=0,
    var totalDeleted: Int=0,
    var childrenRequested: Int=0,
    var onHold: Int=0,
    var accessDenied: Int=0,
    var missing: Int=0,
    var failures: MutableMap<String, String> = mutableMapOf(),
    @JsonIgnore var success: MutableSet<String> = mutableSetOf()
) {
    operator fun plus(other: BatchDeleteAssetsResponse) {
        totalRequested += other.totalRequested
        totalDeleted += other.totalDeleted
        childrenRequested += other.childrenRequested
        onHold += other.onHold
        accessDenied += other.accessDenied
        missing += other.missing
        failures.putAll(other.failures)
    }
}

/**
 * The ES document
 */
open class Document {

    var document: Map<String, Any>

    var id: String = UUID.randomUUID().toString()

    var type = "asset"

    var permissions: MutableMap<String, Int>? = null

    var links: MutableList<Tuple<String, Any>>? = null

    var score : Float? = null

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
    fun <T> getAttr(attr: String, type: Class<T>): T {
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
     * @param attr
     * @param value
     */
    fun addToAttr(attr: String, value: Any) {
        val current = getContainer(attr, true)
        val key = Attr.name(attr)


        /**
         * Handle the case where the object is a standard map.
         */
        try {
            val map = current as MutableMap<String, Any>?
            var collection: MutableCollection<Nothing>? = map!![key] as MutableCollection<Nothing>
            if (collection == null) {
                collection = mutableListOf()
                map[key] = collection
            }
            if (value is Collection<*>) {
                collection!!.addAll(value as Collection<Nothing>)
            } else {
                collection!!.add(value as Nothing)
            }
            return
        } catch (ex2: Exception) {
            logger.warn("The parent attribute {} of type {} is not valid.",
                    attr, current!!.javaClass.name)
        }

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
            (current as MutableMap<String, Any>)[key] = Json.Mapper.convertValue(value as Any)
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

    val DELIMITER = "."

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
    fun getRef(source: Document): String {
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


