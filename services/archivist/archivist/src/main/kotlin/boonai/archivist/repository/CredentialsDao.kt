package boonai.archivist.repository

import boonai.archivist.domain.Credentials
import boonai.archivist.domain.CredentialsType
import boonai.archivist.security.getProjectId

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface CredentialsDao : JpaRepository<Credentials, UUID> {
    fun getOneByProjectIdAndId(projectId: UUID, id: UUID): Credentials
    fun getOneByProjectIdAndName(projectId: UUID, name: String): Credentials
}

interface CredentialsCustomDao {
    fun getEncryptedBlob(id: UUID): String
    fun getEncryptedBlobByJob(jobId: UUID, type: CredentialsType): String
    fun setEncryptedBlob(id: UUID, encryptedText: String): Boolean
}

@Repository
class CredentialsCustomDaoImpl : CredentialsCustomDao, AbstractDao() {

    override fun getEncryptedBlob(id: UUID): String {
        return jdbc.queryForObject(GET_BLOB, String::class.java, getProjectId(), id)
    }

    override fun getEncryptedBlobByJob(jobId: UUID, type: CredentialsType): String {
        return jdbc.queryForObject(
            GET_BLOB_BY_JOB, String::class.java,
            jobId, type.ordinal, getProjectId()
        )
    }

    override fun setEncryptedBlob(id: UUID, encryptedText: String): Boolean {
        return jdbc.update(
            "UPDATE credentials SET str_blob=? WHERE pk_project=? AND pk_credentials=?",
            encryptedText, getProjectId(), id
        ) == 1
    }

    companion object {
        const val GET_BLOB =
            "SELECT str_blob FROM credentials WHERE pk_project=? AND pk_credentials=?"

        const val GET_BLOB_BY_DS =
            "SELECT " +
                "str_blob " +
                "FROM " +
                "credentials INNER JOIN x_credentials_datasource ON " +
                "(credentials.pk_credentials = x_credentials_datasource.pk_credentials) " +
                "WHERE " +
                "x_credentials_datasource.pk_datasource=? " +
                "AND " +
                "x_credentials_datasource.int_type=? " +
                "AND " +
                "credentials.pk_project=?"

        const val GET_BLOB_BY_JOB =
            "SELECT " +
                "str_blob " +
                "FROM " +
                "credentials INNER JOIN x_credentials_job ON " +
                "(credentials.pk_credentials = x_credentials_job.pk_credentials) " +
                "WHERE " +
                "x_credentials_job.pk_job=? " +
                "AND " +
                "x_credentials_job.int_type=? " +
                "AND " +
                "credentials.pk_project=?"
    }
}
