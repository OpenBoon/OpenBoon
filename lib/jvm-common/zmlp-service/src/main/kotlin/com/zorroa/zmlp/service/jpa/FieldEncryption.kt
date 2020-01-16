package com.zorroa.zmlp.service.jpa

import com.zorroa.zmlp.service.security.getProjectId
import com.zorroa.zmlp.service.storage.SystemStorageService
import com.zorroa.zmlp.util.Json
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.security.crypto.encrypt.Encryptors
import org.springframework.stereotype.Component
import org.springframework.util.ReflectionUtils
import org.springframework.util.ReflectionUtils.FieldCallback
import java.lang.reflect.Field
import java.util.UUID

object EncryptionUtils {

    fun getPropertyIndex(name: String, properties: Array<String>): Int {
        for (i in properties.indices) {
            if (name == properties[i]) {
                return i
            }
        }
        throw IllegalArgumentException("No property was found for name $name")
    }
}

@Component
class FieldEncryption {

    @Autowired
    lateinit var systemStorageService: SystemStorageService

    fun encrypt(
        state: Array<Any>,
        propertyNames: Array<String>,
        entity: Any
    ) {
        ReflectionUtils.doWithFields(
            entity.javaClass,
            FieldCallback { field: Field -> encryptField(field, state, propertyNames) },
            ReflectionUtils.FieldFilter {
                AnnotationUtils.findAnnotation(it, Encrypted::class.java) != null
            }
        )
    }

    private fun encryptField(
        field: Field,
        state: Array<Any>,
        propertyNames: Array<String>
    ) {
        val propertyIndex: Int = EncryptionUtils.getPropertyIndex(field.name, propertyNames)
        val currentValue = state[propertyIndex]
        check(currentValue is String) { "Encrypted annotation was used on a non-String field" }

        val pid = getProjectId()
        val key = fetchProjectKey(pid)
        state[propertyIndex] = Encryptors.text(key, pid.toString()).encrypt(currentValue.toString())
    }

    fun decrypt(entity: Any) {
        ReflectionUtils.doWithFields(
            entity.javaClass,
            FieldCallback { field: Field -> decryptField(field, entity) },
            ReflectionUtils.FieldFilter {
                AnnotationUtils.findAnnotation(it, Encrypted::class.java) != null
            }
        )
    }

    private fun decryptField(field: Field, entity: Any) {
        field.isAccessible = true
        val value = ReflectionUtils.getField(field, entity)
        check(value is String) { "Encrypted annotation was used on a non-String field" }

        val pid = getProjectId()
        val key = fetchProjectKey(pid)

        val clearValue = Encryptors.text(key, getProjectId().toString()).decrypt(value.toString())
        ReflectionUtils.setField(field, entity, clearValue)
    }

    fun fetchProjectKey(pid: UUID) : String {
        val keys = systemStorageService.fetchObject(
            "projects/$pid/keys.json", Json.LIST_OF_STRING)
        // If this ever changes, things will break.
        val mod1 = (pid.leastSignificantBits % keys.size).toInt()
        val mod2 = (pid.mostSignificantBits % keys.size).toInt()
        return "${keys[mod1]}_${keys[mod2].reversed()}_${keys.last()}}"
    }
}