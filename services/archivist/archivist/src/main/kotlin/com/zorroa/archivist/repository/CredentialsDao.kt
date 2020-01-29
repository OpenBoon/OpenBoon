package com.zorroa.archivist.repository

import com.zorroa.archivist.domain.Credentials
import com.zorroa.archivist.security.getProjectId

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface CredentialsDao : JpaRepository<Credentials, UUID> {
    fun getOneByProjectIdAndId(projectId: UUID, id: UUID): Credentials
}

interface CredentialsCustomDao {
    fun getEncryptedBlob(id: UUID): String
    fun updateEncryptedBlob(id: UUID, encryptedText: String): Boolean
}

@Repository
class CredentialsCustomDaoImpl : CredentialsCustomDao, AbstractDao() {

    override fun getEncryptedBlob(id: UUID): String {
        return jdbc.queryForObject(GET_BLOB, String::class.java, getProjectId(), id)
    }

    override fun updateEncryptedBlob(id: UUID, encryptedText: String): Boolean {
        return jdbc.update("UPDATE credentials SET str_blob=? WHERE pk_project=? AND pk_credentials=?",
            encryptedText, getProjectId(), id) == 1
    }

    companion object {
        const val GET_BLOB =
            "SELECT str_blob FROM credentials WHERE pk_project=? AND pk_credentials=?"
    }
}
