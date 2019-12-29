package com.zorroa.auth.server.repository

import javax.persistence.AttributeConverter
import javax.persistence.Converter

@Converter
class StringListConverter : AttributeConverter<List<String>, String> {

    override fun convertToDatabaseColumn(list: List<String>): String {
        return list.joinToString(",")
    }

    override fun convertToEntityAttribute(joined: String): List<String> {
        return joined.split(",").map { it.trim() }
    }
}

@Converter
class StringSetConverter : AttributeConverter<Set<String>, String> {

    override fun convertToDatabaseColumn(list: Set<String>): String {
        return list.joinToString(",")
    }

    override fun convertToEntityAttribute(joined: String): Set<String> {
        return joined.split(",").map { it.trim() }.toSet()
    }
}
