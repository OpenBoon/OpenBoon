package boonai.archivist.service

import boonai.archivist.domain.Credentials
import boonai.archivist.domain.CredentialsSpec
import boonai.archivist.domain.CredentialsType
import boonai.archivist.domain.CredentialsUpdate
import boonai.archivist.repository.CredentialsCustomDao
import boonai.archivist.repository.CredentialsDao
import boonai.archivist.repository.UUIDGen
import boonai.archivist.security.getProjectId
import boonai.archivist.security.getZmlpActor
import boonai.archivist.util.isUUID
import boonai.common.service.logging.LogAction
import boonai.common.service.logging.LogObject
import boonai.common.service.logging.event
import boonai.common.service.security.EncryptionService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext

interface CredentialsService {
    fun create(spec: CredentialsSpec): Credentials
    fun get(id: UUID): Credentials
    fun get(name: String): Credentials
    fun delete(id: UUID)
    fun update(id: UUID, update: CredentialsUpdate): Credentials
    fun getDecryptedBlob(id: UUID): String
    fun getDecryptedBlobByJob(jobId: UUID, type: CredentialsType): String
    fun setEncryptedBlob(id: UUID, type: CredentialsType, clearText: String)
    fun getAll(idsOrNames: Collection<String>?): List<Credentials>
}

@Service
@Transactional
class CredentialsServiceImpl(
    val credentialsDao: CredentialsDao,
    val credentialsCustomDao: CredentialsCustomDao,
    val encryptionService: EncryptionService
) : CredentialsService {

    @PersistenceContext
    lateinit var entityManager: EntityManager

    override fun create(spec: CredentialsSpec): Credentials {
        val id = UUIDGen.uuid1.generate()
        val time = System.currentTimeMillis()
        val actor = getZmlpActor()

        val creds = Credentials(
            id,
            getProjectId(),
            spec.name,
            spec.type,
            time,
            time,
            actor.toString(),
            actor.toString()
        )

        val created = credentialsDao.saveAndFlush(creds)
        setEncryptedBlob(id, spec.type, spec.blob)
        logger.event(LogObject.CREDENTIALS, LogAction.CREATE, mapOf("newCredentialsId" to id))
        return created
    }

    @Transactional(readOnly = true)
    override fun get(id: UUID): Credentials {
        return credentialsDao.getOneByProjectIdAndId(getProjectId(), id)
    }

    @Transactional(readOnly = true)
    override fun get(name: String): Credentials {
        return credentialsDao.getOneByProjectIdAndName(getProjectId(), name)
    }

    @Transactional(readOnly = true)
    override fun getAll(idsOrNames: Collection<String>?): List<Credentials> {
        if (idsOrNames == null) {
            return listOf()
        }
        // TODO: make 1 query
        return idsOrNames.map {
            if (it.isUUID()) {
                get(UUID.fromString(it))
            } else {
                get(it)
            }
        }
    }

    override fun delete(id: UUID) {
        logger.event(LogObject.CREDENTIALS, LogAction.DELETE, mapOf("credentialsId" to id))
        credentialsDao.delete(get(id))
    }

    override fun update(id: UUID, update: CredentialsUpdate): Credentials {
        val current = get(id)
        entityManager.detach(current)

        update.blob?.let {
            current.type.validate(it)
            val cryptedText = encryptionService.encryptString(it, Credentials.CRYPT_VARIANCE)
            credentialsCustomDao.setEncryptedBlob(id, cryptedText)
        }

        logger.event(LogObject.CREDENTIALS, LogAction.UPDATE, mapOf("credentialsId" to id))
        val creds = credentialsDao.save(current.getUpdated(update))
        return get(creds.id)
    }

    override fun setEncryptedBlob(id: UUID, type: CredentialsType, clearText: String) {
        type.validate(clearText)
        val cryptedText = encryptionService.encryptString(clearText, Credentials.CRYPT_VARIANCE)
        credentialsCustomDao.setEncryptedBlob(id, cryptedText)
    }

    @Transactional(readOnly = true)
    override fun getDecryptedBlob(id: UUID): String {
        logger.event(LogObject.CREDENTIALS, LogAction.DECRYPT, mapOf("credentialsId" to id))
        return encryptionService.decryptString(
            credentialsCustomDao.getEncryptedBlob(id), Credentials.CRYPT_VARIANCE
        )
    }

    @Transactional(readOnly = true)
    override fun getDecryptedBlobByJob(jobId: UUID, type: CredentialsType): String {
        logger.event(
            LogObject.CREDENTIALS, LogAction.DECRYPT,
            mapOf("jobId" to jobId, "credentialsType" to type.name)
        )
        return encryptionService.decryptString(
            credentialsCustomDao.getEncryptedBlobByJob(jobId, type), Credentials.CRYPT_VARIANCE
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CredentialsServiceImpl::class.java)
    }
}
