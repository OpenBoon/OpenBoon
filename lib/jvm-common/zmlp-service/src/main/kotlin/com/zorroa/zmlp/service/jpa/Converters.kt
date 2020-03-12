package com.zorroa.zmlp.service.jpa

import javax.persistence.AttributeConverter
import javax.persistence.Converter

@Converter
class EncryptedConverter : AttributeConverter<String, String> {

    override fun convertToDatabaseColumn(value: String): String {
        return value
    }

    override fun convertToEntityAttribute(value: String): String {
        return HIDDEN_TEXT
    }

    companion object {
        const val HIDDEN_TEXT = "<ENCRYPTED>"
    }
}

class StringSetConverter : AttributeConverter<Set<String>, String> {

    override fun convertToDatabaseColumn(list: Set<String>): String {
        return list.joinToString(",")
    }

    override fun convertToEntityAttribute(joined: String): Set<String> {
        return joined.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }
}

class StringListConverter : AttributeConverter<List<String>, String> {

    override fun convertToDatabaseColumn(list: List<String>?): String? {
        return list?.joinToString(",") ?: null
    }

    override fun convertToEntityAttribute(joined: String?): List<String>? {
        return if (joined == null) {
            listOf()
        } else {
            return joined.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toList()
        }
    }
}
