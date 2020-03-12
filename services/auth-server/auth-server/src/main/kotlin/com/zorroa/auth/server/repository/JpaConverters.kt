package com.zorroa.auth.server.repository

import javax.persistence.AttributeConverter

class EncryptedConverter : AttributeConverter<String, String> {

    override fun convertToDatabaseColumn(value: String): String {
        return value
    }

    override fun convertToEntityAttribute(value: String): String {
        return "ENCRYPTED"
    }
}
