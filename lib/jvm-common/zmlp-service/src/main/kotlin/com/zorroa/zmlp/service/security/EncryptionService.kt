package com.zorroa.zmlp.service.security

import com.zorroa.zmlp.service.storage.SystemStorageService
import com.zorroa.zmlp.util.Json
import java.util.UUID
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.crypto.encrypt.Encryptors

/**
 * A service for encrypting stings using keys in system storage.  The
 * 'variance' arg is used to dynamically add additional key data.
 * If you encrypt with a variance of 100, you must decrypt with
 * a variance of 100.
 */
interface EncryptionService {

    /**
     * Fetch a project encryption key for the authed user.
     */
    fun getProjectKey(variance: Int): String

    /**
     * Fetch a project encryption key for the given project.
     */
    fun getProjectKey(projectId: UUID, variance: Int): String

    /**
     * Fetch a project encryption key for the authed user.
     */
    fun encryptString(value: String, variance: Int): String

    /**
     * Fetch a project encryption key for the authed user.
     */
    fun decryptString(value: String, variance: Int): String

    /**
     * Fetch a project encryption key for the authed user.
     */
    fun encryptString(projectId: UUID, value: String, variance: Int): String

    /**
     * Fetch a project encryption key for the authed user.
     */
    fun decryptString(projectId: UUID, value: String, variance: Int): String
}

class EncryptionServiceImpl : EncryptionService {

    @Autowired
    lateinit var systemStorageService: SystemStorageService

    override fun getProjectKey(projectId: UUID, variance: Int): String {
        val keys = systemStorageService.fetchObject(
            "projects/$projectId/keys.json", Json.LIST_OF_STRING
        )

        val keySize = keys.size
        val mod1 = Math.floorMod(projectId.leastSignificantBits, keySize)
        val mod2 = Math.floorMod(projectId.mostSignificantBits, keySize)
        val mod3 = Math.floorMod(variance, keySize)

        val key = StringBuilder(256)
        key.append(keys[mod1])
        key.append(keys[mod2])
        key.append(keys[mod3])
        if (mod3 > keySize / 2) {
            key.append(keys.first())
        } else {
            key.append(keys.last())
        }
        return key.toString()
    }

    override fun getProjectKey(variance: Int): String {
        return getProjectKey(getProjectId(), variance)
    }

    override fun encryptString(value: String, variance: Int): String {
        val key = getProjectKey(variance)
        return Encryptors.text(key, getProjectId().toString().replace("-", "")).encrypt(value)
    }

    override fun decryptString(value: String, variance: Int): String {
        val key = getProjectKey(variance)
        return Encryptors.text(key, getProjectId().toString().replace("-", "")).decrypt(value)
    }

    override fun encryptString(projectId: UUID, value: String, variance: Int): String {
        val key = getProjectKey(projectId, variance)
        return Encryptors.text(key, projectId.toString().replace("-", "")).encrypt(value)
    }

    override fun decryptString(projectId: UUID, value: String, variance: Int): String {
        val key = getProjectKey(projectId, variance)
        return Encryptors.text(key, projectId.toString().replace("-", "")).decrypt(value)
    }
}
