package com.zorroa.archivist.util

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.text.SimpleDateFormat
import java.util.Arrays
import java.util.UUID
import java.util.concurrent.ThreadLocalRandom
import java.util.stream.IntStream

object StaticUtils {

    val mapper = jacksonObjectMapper()

    fun init() {
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
        mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false)
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        mapper.configure(SerializationFeature.WRITE_ENUMS_USING_INDEX, true)
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
        mapper.configure(MapperFeature.USE_GETTERS_AS_SETTERS, false)
        mapper.dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z")
    }

    val UUID_REGEXP = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$", RegexOption.IGNORE_CASE) }

private const val SYMBOLS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0987654321"

fun randomString(length: Int = 16): String {
    val random = ThreadLocalRandom.current()
    val buf = CharArray(length)
    for (i in 0 until length) {
        buf[i] = SYMBOLS[random.nextInt(SYMBOLS.length)]
    }
    return String(buf)
}

/**
 * Extension function for printing UUID chars
 */
fun UUID.prefix(size: Int = 8): String {
    return this.toString().substring(0, size)
}

/**
 * Extension function to check if a string is a UUID
 */
fun String.isUUID(): Boolean = StaticUtils.UUID_REGEXP.matches(this)

/**
 * Compute a Hash from a Float Array
 */
fun hashFeatures(features: DoubleArray): String {
    val hash = StringBuilder()
    val doubleArray =
        IntStream.range(0, features.size)
            .mapToDouble { i: Int -> features[i].toDouble() }
            .toArray()

    Arrays.stream(doubleArray)
        .map { f: Double -> (f * 16.0) }
        .map { f: Double -> Math.max(0.0, f) }
        .map { f: Double -> Math.min(15.0, f) }
        .map { f: Double -> (f + 65) }
        .forEach { f: Double -> hash.append(f.toChar()) }
    return hash.toString()
}
