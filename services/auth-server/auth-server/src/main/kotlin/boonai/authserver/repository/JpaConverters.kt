package boonai.authserver.repository

import javax.persistence.AttributeConverter

class JpaConverters

class EncryptedConverter : AttributeConverter<String, String> {

    override fun convertToDatabaseColumn(value: String): String {
        return value
    }

    override fun convertToEntityAttribute(value: String): String {
        return "ENCRYPTED"
    }
}
