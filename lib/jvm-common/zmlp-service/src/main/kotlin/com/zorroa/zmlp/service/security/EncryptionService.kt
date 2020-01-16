package com.zorroa.zmlp.service.security

import com.zorroa.zmlp.service.storage.SystemStorageService
import com.zorroa.zmlp.util.Json
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.crypto.encrypt.Encryptors
import org.springframework.stereotype.Component
import java.util.UUID

interface EncryptionService {
    fun getProjectKey(plus: Int): String
    fun getProjectKey(projectId: UUID, plus: Int): String
    fun encryptString(value: String, plus: Int): String
    fun decryptString(value: String, plus: Int): String
}


class EncryptionServiceImpl : EncryptionService {

    @Autowired
    lateinit var systemStorageService: SystemStorageService

    override fun getProjectKey(projectId: UUID, plus: Int): String {
        val keys = systemStorageService.fetchObject(
            "projects/$projectId/keys.json", Json.LIST_OF_STRING
        )

        val mod1 = (projectId.leastSignificantBits % keys.size).toInt()
        val mod2 = (projectId.mostSignificantBits % keys.size).toInt()

        val key = StringBuilder(256)
        key.append(keys[mod1])
        key.append(keys[mod2].reversed())
        if (plus % 2 == 0) {
            key.append(keys[plus % 15])
        } else {
            key.append(keys[plus % 15].reversed())
        }
        return key.toString()
    }

    override fun getProjectKey(plus: Int): String {
        return getProjectKey(getProjectId(), plus)
    }

    override fun encryptString(value: String, plus: Int): String {
        val key = getProjectKey(plus)
        return Encryptors.text(key, getProjectId().toString()).encrypt(value)
    }

    override fun decryptString(value: String, plus: Int): String {
        val key = getProjectKey(plus)
        return Encryptors.text(key, getProjectId().toString()).decrypt(value)
    }
}