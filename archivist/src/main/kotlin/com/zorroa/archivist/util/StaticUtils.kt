package com.zorroa.archivist.util

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.text.SimpleDateFormat

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

    val UUID_REGEXP = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$")
}

inline fun  <E: Any, T: Collection<E>> T?.whenNullOrEmpty(func: () -> Unit): Unit {
    if (this == null || this.isEmpty()) {
        func()
    }
}

