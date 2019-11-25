package com.zorroa.archivist.util

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.apache.commons.lang3.RandomStringUtils
import org.apache.tika.Tika
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.text.SimpleDateFormat
import java.util.Random

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

    val UUID_REGEXP = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$",
            RegexOption.IGNORE_CASE)

    val tika = Tika()
}

/**
 * Copy the contents of the given InputStream to the OutputStream.  Utilizes
 * NIO streams for max performance.
 *
 * @param input The src input stream
 * @param output The dst output stream
 * @param buffer_size: The size of the copy buffer, defaults to 16384
 */
fun copyInputToOuput(input: InputStream, output: OutputStream, buffer_size: Int = 16384): Long {
    val inputChannel = Channels.newChannel(input)
    val outputChannel = Channels.newChannel(output)
    var size = 0L

    inputChannel.use { ic ->
        outputChannel.use { oc ->
            val buffer = ByteBuffer.allocateDirect(buffer_size)
            while (ic.read(buffer) != -1) {
                buffer.flip()
                size += oc.write(buffer)
                buffer.clear()
            }
        }
    }
    return size
}

inline fun <E : Any, T : Collection<E>> T?.whenNullOrEmpty(func: () -> Unit) {
    if (this == null || this.isEmpty()) {
        func()
    }
}

private const val SYMBOLS = "abcdefghijklmnopqrstuvwxyz0987654321"

fun randomString(length: Int=16): String {
    val random = Random()
    val buf = CharArray(length)
    for (i in 0 until length) {
        buf[i] = SYMBOLS[random.nextInt(SYMBOLS.length)]
    }
    return String(buf)
}

