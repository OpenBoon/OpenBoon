package boonai.common.util

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import java.text.SimpleDateFormat

inline fun <reified T : Any> ObjectMapper.readValueOrNull(content: String?): T? {
    return if (content == null) {
        null
    } else {
        readValue(content, jacksonTypeRef<T>())
    }
}

object Json {

    val GENERIC_MAP: TypeReference<Map<String, Any>> = object : TypeReference<Map<String, Any>>() {}
    val ENV_MAP: TypeReference<Map<String, String>> = object : TypeReference<Map<String, String>>() {}
    val MUTABLE_MAP: TypeReference<MutableMap<String, Any>> = object : TypeReference<MutableMap<String, Any>>() {}
    val LIST_OF_GENERIC_MAP: TypeReference<List<Map<String, Any>>> = object : TypeReference<List<Map<String, Any>>>() {}
    val LIST_OF_STRING: TypeReference<List<String>> = object : TypeReference<List<String>>() {}

    val Mapper = jacksonObjectMapper()

    init {
        configureObjectMapper(Mapper)
    }

    fun configureObjectMapper(mapper: ObjectMapper): ObjectMapper {
        mapper.registerModule(KotlinModule())
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        mapper.configure(SerializationFeature.WRITE_ENUMS_USING_INDEX, false)
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
        mapper.configure(MapperFeature.USE_GETTERS_AS_SETTERS, false)
        mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true)
        mapper.dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
        return mapper
    }

    fun prettyPrint(any: Any) {
        try {
            println(Mapper.writerWithDefaultPrettyPrinter().writeValueAsString(any))
        } catch (e: JsonProcessingException) {
            throw IllegalArgumentException(
                "Failed to serialize object, unexpected: $e", e
            )
        }
    }

    fun prettyString(value: Any): String {
        return Mapper.writerWithDefaultPrettyPrinter().writeValueAsString(value)
    }

    fun hash(value: Any): Int {
        val newValue = Mapper.writeValueAsString(value)
        return newValue.hashCode()
    }

    fun serializeToString(value: Any): String {
        return Mapper.writeValueAsString(value)
    }

    fun serializeToString(value: Any?, onNull: String?): String? {
        return if (value == null) {
            onNull
        } else {
            Mapper.writeValueAsString(value)
        }
    }

    fun serialize(value: Any?, onNull: String?): ByteArray? {
        if (value == null) {
            return onNull?.toByteArray()
        }
        return Mapper.writeValueAsBytes(value)
    }

    fun serialize(value: Any): ByteArray? {
        return Mapper.writeValueAsBytes(value)
    }
}
