package com.zorroa.archivist.util

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.apache.mxnet.Context
import org.apache.mxnet.DataBatch
import org.apache.mxnet.Model
import org.apache.mxnet.javaapi.DType
import org.apache.mxnet.javaapi.DataDesc
import org.apache.mxnet.javaapi.Image
import org.apache.mxnet.javaapi.Shape
import org.apache.mxnet.module.Module
import org.elasticsearch.common.util.FloatArray
import org.omg.CORBA.Environment
import scala.Option
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

    val UUID_REGEXP = Regex(
        "^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$",
        RegexOption.IGNORE_CASE
    )
}

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
 * Convert a Byte Array to a MXNet Model Hash using generated attributes
 */
fun assetToHash(modelPath: String, bytes: ByteArray): String {

    // Prepare data
    var nd = Image.imDecode(bytes, 1, false)
    nd = Image.imResize(nd, 224, 224)
    nd = org.apache.mxnet.javaapi.NDArray.transpose(nd, Shape(intArrayOf(2, 0, 1)), null)[0] // HWC to CHW
    nd = org.apache.mxnet.javaapi.NDArray.expand_dims(nd, 0, null)[0] // Add N -> NCHW
    nd = nd.asType(DType.Float32()) // Inference with Float32
    // Get Params
    val contextArray =
        arrayOf(Context.defaultCtx())
    val symbol =
        Model.loadCheckpoint(modelPath, 0)
    val flatten0_output = symbol._1().internals["flatten0_output"]
    val module = Module(
        flatten0_output,
        Module.`$lessinit$greater$default$2`(),
        Module.`$lessinit$greater$default$3`(),
        contextArray,
        Module.`$lessinit$greater$default$5`(),
        Option.empty()
    )
    val dataDesc = DataDesc(
        "data",
        Shape(intArrayOf(1, 3, 224, 224)),
        DType.Float32(),
        "NCHW"
    )
    module.bind(false, false, false, dataDesc.dataDesc())
    module.setParams(symbol._2(), symbol._3(), false, true, false)
    val builder = DataBatch.Builder()
    builder.setData(nd.nd())
    module.forward(builder.build(), Option.empty())
    val outputsMerged = module.outputsMerged
    val elem = outputsMerged.toVector().getElem(0, 0)
    return hashFeatures(elem.toFloat64Array())
}

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
