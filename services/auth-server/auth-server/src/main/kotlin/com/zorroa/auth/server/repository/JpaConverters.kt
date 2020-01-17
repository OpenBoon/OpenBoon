package com.zorroa.auth.server.repository

import javax.persistence.AttributeConverter

class StringSetConverter : AttributeConverter<Set<String>, String> {

    override fun convertToDatabaseColumn(list: Set<String>): String {
        return list.joinToString(",")
    }

    override fun convertToEntityAttribute(joined: String): Set<String> {
        return joined.split(",").map { it.trim() }.toSet()
    }
}

class EncryptedConverter : AttributeConverter<String, String> {

    override fun convertToDatabaseColumn(value: String): String {
        return value
    }

    override fun convertToEntityAttribute(value: String): String {
        return "ENCRYPTED"
    }
}

