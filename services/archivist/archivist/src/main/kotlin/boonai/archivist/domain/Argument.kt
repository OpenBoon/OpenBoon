package boonai.archivist.domain

import boonai.archivist.util.validateUrl

enum class ArgumentType {
    String,
    Integer,
    Float,
    Bool,
    List,
    Object,
    Url;

    fun isValid(obj: Any?): Boolean {
        if (obj == null) {
            return true
        }

        return when (this) {
            String -> {
                if (obj is CharSequence) {
                    obj.length < MAX_STR_LENGTH
                } else {
                    false
                }
            }
            Integer -> {
                obj is Int || obj is Long
            }
            Float -> {
                obj is kotlin.Float || obj is Double
            }
            Bool -> {
                obj is Boolean
            }
            List -> {
                obj is kotlin.collections.List<*>
            }
            Object -> {
                obj is Map<*, *>
            }
            Url -> {
                return if (obj is CharSequence) {
                    try {
                        validateUrl(obj.toString(), false)
                        true
                    } catch (e: Exception) {
                        false
                    }
                } else {
                    false
                }
            }
        }
    }

    companion object {
        const val MAX_STR_LENGTH = 256
    }
}

open class ArgValidationException(msg: String) : ArchivistException(msg)
class ArgRequiredException(msg: String) : ArgValidationException(msg)
class ArgTypeException(msg: String) : ArgValidationException(msg)
class ArgUnknownException(msg: String) : ArgValidationException(msg)

class ArgSchema(
    args: Map<String, Argument>?
) {
    constructor(vararg pairs: Pair<String, Argument>) : this(pairs.toMap())

    val args: MutableMap<String, Argument> = mutableMapOf()
    init {
        this.args.putAll(args ?: emptyMap())
    }

    fun add(name: String, arg: Argument): ArgSchema {
        args.put(name, arg)
        return this
    }

    companion object {
        fun empty(): ArgSchema {
            return ArgSchema()
        }
    }
}

class ItemType(
    val type: ArgumentType,
    val minItems: Int = 0,
    val uniqueItems: Boolean = false
)

class Argument(
    val type: ArgumentType,
    val defaultValue: Any? = null,
    val description: String? = null,
    val required: Boolean = false,
    val itemsType: ItemType? = null,
    var args: MutableMap<String, Argument>? = null
) {
    fun subArgs(vararg pairs: Pair<String, Argument>): Argument {
        if (args == null) {
            args = mutableMapOf()
        }
        args?.let { it.putAll(pairs) }
        return this
    }
}
