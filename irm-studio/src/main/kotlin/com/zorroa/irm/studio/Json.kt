package com.zorroa.irm.studio

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.zorroa.studio.sdk.ProcessorRef
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

object Json {

    var GENERIC_MAP: TypeReference<Map<String, Any>> = object : TypeReference<Map<String, Any>>() {

    }
    var STRING_MAP: TypeReference<Map<String, String>> = object : TypeReference<Map<String, String>>() {

    }
    var SET_OF_INTS: TypeReference<Set<Int>> = object : TypeReference<Set<Int>>() {

    }
    var SET_OF_STRINGS: TypeReference<Set<String>> = object : TypeReference<Set<String>>() {

    }
    var LIST_OF_INTS: TypeReference<List<Int>> = object : TypeReference<List<Int>>() {

    }
    var LIST_OF_STRINGS: TypeReference<List<String>> = object : TypeReference<List<String>>() {

    }
    var LIST_OF_OBJECTS: TypeReference<List<Any>> = object : TypeReference<List<Any>>() {

    }
    var SET_OF_UUIDS: TypeReference<Set<UUID>> = object : TypeReference<Set<UUID>>() {

    }
    var LIST_OF_UUIDS: TypeReference<List<UUID>> = object : TypeReference<List<UUID>>() {

    }
    var LIST_OF_PREFS: TypeReference<List<ProcessorRef>> = object : TypeReference<List<ProcessorRef>>() {

    }

    val Mapper = ObjectMapper()

    init {
        configureObjectMapper(Mapper)
    }

    fun configureObjectMapper(mapper: ObjectMapper): ObjectMapper {
        mapper.registerModule(KotlinModule())
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
        mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false)
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        mapper.configure(SerializationFeature.WRITE_ENUMS_USING_INDEX, true)
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
        mapper.configure(MapperFeature.USE_GETTERS_AS_SETTERS, false)
        mapper.dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z")
        return mapper
    }

    fun prettyString(`object`: Any): String {
        try {
            return Mapper.writerWithDefaultPrettyPrinter().writeValueAsString(`object`)
        } catch (e: JsonProcessingException) {
            throw IllegalArgumentException(
                    "Failed to serialize object, unexpected: $e", e)
        }

    }

    fun serializeToString(`object`: Any): String {
        try {
            return Mapper.writeValueAsString(`object`)
        } catch (e: JsonProcessingException) {
            throw IllegalArgumentException(
                    "Failed to serialize object, unexpected: $e", e)
        }

    }

    fun hash(`object`: Any): Int {
        try {
            val value = Mapper.writeValueAsString(`object`)
            return value.hashCode()
        } catch (e: JsonProcessingException) {
            throw IllegalArgumentException(
                    "Failed to serialize object, unexpected: $e", e)
        }

    }

    fun serialize(`object`: Any): ByteArray {
        try {
            return Mapper.writeValueAsBytes(`object`)
        } catch (e: JsonProcessingException) {
            throw IllegalArgumentException(
                    "Failed to serialize object, unexpected: $e", e)
        }

    }

    fun serializeToString(`object`: Any?, onNull: String): String {
        if (`object` == null) {
            return onNull
        }
        try {
            return Mapper.writeValueAsString(`object`)
        } catch (e: JsonProcessingException) {
            throw IllegalArgumentException(
                    "Failed to serialize object, unexpected: $e", e)
        }

    }

    fun serialize(`object`: Any?, onNull: String): ByteArray {
        if (`object` == null) {
            return onNull.toByteArray()
        }
        try {
            return Mapper.writeValueAsBytes(`object`)
        } catch (e: JsonProcessingException) {
            throw IllegalArgumentException(
                    "Failed to serialize object, unexpected: $e", e)
        }

    }

    fun <T> deserialize(data: ByteArray, valueType: Class<T>): T {
        try {
            return Mapper.readValue(data, valueType)
        } catch (e: IOException) {
            throw IllegalArgumentException(
                    "Failed to unserialize object, unexpected $e", e)
        }

    }

    fun <T> deserialize(data: ByteArray, valueType: TypeReference<T>): T {
        try {
            return Mapper.readValue(data, valueType)
        } catch (e: IOException) {
            throw IllegalArgumentException(
                    "Failed to unserialize object, unexpected $e", e)
        }

    }

    fun <T> deserialize(data: String, valueType: Class<T>): T {
        try {
            return Mapper.readValue(data, valueType)
        } catch (e: IOException) {
            throw IllegalArgumentException(
                    "Failed to unserialize object, unexpected $e", e)
        }

    }

    fun <T> deserialize(data: String, valueType: TypeReference<T>): T {
        try {
            return Mapper.readValue(data, valueType)
        } catch (e: IOException) {
            throw IllegalArgumentException(
                    "Failed to unserialize object, unexpected $e", e)
        }

    }
}
