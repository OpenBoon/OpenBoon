package boonai.archivist.service

import boonai.archivist.domain.ArgRequiredException
import boonai.archivist.domain.ArgSchema
import boonai.archivist.domain.ArgTypeException
import boonai.archivist.domain.ArgUnknownException
import boonai.archivist.domain.ArgumentType
import boonai.common.service.storage.SystemStorageException
import boonai.common.service.storage.SystemStorageService
import boonai.common.util.Json
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import java.io.FileNotFoundException

interface ArgValidationService {
    fun getArgSchema(name: String): ArgSchema
    fun validateArgs(schema: String, args: Map<String, Any>)
    fun validateArgs(schema: ArgSchema, args: Map<String, Any>)
    fun validateArgsUnknownOnly(schema: ArgSchema, args: Map<String, Any>)
    fun validateArgsUnknownOnly(schema: String, args: Map<String, Any>)
    fun buildArgs(schema: ArgSchema, args: Map<String, Any>): MutableMap<String, Any?>
    fun buildArgs(schema: String, args: Map<String, Any>): MutableMap<String, Any?>
}

@Service
class ArgValidationServiceImpl : ArgValidationService {

    @Autowired
    lateinit var systemStorage: SystemStorageService

    override fun validateArgs(name: String, args: Map<String, Any>) {
        val schema = getArgSchema(name)
        validateArgs(schema, args)
    }

    override fun validateArgs(schema: ArgSchema, args: Map<String, Any>) {
        for ((argName, argument) in schema.args) {
            if (argument.type == ArgumentType.Object) {
                validateArgs(
                    ArgSchema(argument.args),
                    (args[argName] ?: mapOf<String, Any>()) as Map<String, Any>
                )
            } else {
                val value = args[argName]
                if (value == null) {
                    if (argument.required) {
                        if (argument.defaultValue == null) {
                            throw ArgRequiredException("A value must be set for $argName , was null")
                        }
                    }
                } else {
                    if (!argument.type.isValid(value)) {
                        throw ArgTypeException("The value for $argName must be a ${argument.type}")
                    }
                }
            }
        }

        // Now do the reverse
        val names = schema.args.keys
        for (argName in args.keys) {
            if (argName !in names) {
                throw ArgUnknownException("$argName is not a valid option.")
            }
        }
    }

    override fun validateArgsUnknownOnly(schema: ArgSchema, args: Map<String, Any>) {
        for ((argName, argument) in schema.args) {
            if (argument.type == ArgumentType.Object) {
                validateArgs(
                    ArgSchema(argument.args),
                    (args[argName] ?: mapOf<String, Any>()) as Map<String, Any>
                )
            }
        }

        // Now do the reverse
        val names = schema.args.keys
        for (argName in args.keys) {
            if (argName !in names) {
                throw ArgUnknownException("$argName is not a valid option.")
            }
        }
    }

    override fun validateArgsUnknownOnly(name: String, args: Map<String, Any>) {
        val schema = getArgSchema(name)
        validateArgsUnknownOnly(schema, args)
    }

    override fun buildArgs(schema: ArgSchema, args: Map<String, Any>): MutableMap<String, Any?> {
        val result = mutableMapOf<String, Any?>()
        buildArgs(schema, args, result)
        return result
    }

    override fun buildArgs(schema: String, args: Map<String, Any>): MutableMap<String, Any?> {
        val result = mutableMapOf<String, Any?>()
        buildArgs(getArgSchema(schema), args, result)
        return result
    }

    fun buildArgs(schema: ArgSchema, args: Map<String, Any>, result: MutableMap<String, Any?>) {
        for ((argName, argument) in schema.args) {
            if (argument.type == ArgumentType.Object) {
                val nestedResult = mutableMapOf<String, Any?>()
                val existingArgs = (args[argName] ?: mapOf<String, Any>()) as Map<String, Any>
                nestedResult.putAll(existingArgs)
                result[argName] = nestedResult

                buildArgs(
                    ArgSchema(argument.args),
                    existingArgs,
                    nestedResult
                )
            } else {
                var value = args[argName]
                if (value == null) {
                    value = argument.defaultValue
                }
                result[argName] = value
            }
        }
    }

    override fun getArgSchema(name: String): ArgSchema {
        val fixed = name.trim('/').replace('.', '_')
        val path = "args/$fixed.json"
        return loadArgSchemaFromBucket(path)
            ?: loadArgSchemaFromResource(path)
            ?: ArgSchema.empty()
    }

    fun loadArgSchemaFromBucket(path: String): ArgSchema? {
        return try {
            systemStorage.fetchObject(path, ArgSchema::class.java)
        } catch (e: SystemStorageException) {
            null
        }
    }

    fun loadArgSchemaFromResource(path: String): ArgSchema? {
        return try {
            return Json.Mapper.readValue(ClassPathResource(path).inputStream, ArgSchema::class.java)
        } catch (e: FileNotFoundException) {
            null
        }
    }
}
