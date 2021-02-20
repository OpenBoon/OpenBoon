package boonai.authserver.repository

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import boonai.authserver.domain.ApiKey
import boonai.authserver.domain.ApiKeyFilter
import boonai.authserver.domain.ValidationKey
import boonai.common.apikey.SigningKey
import boonai.common.service.security.EncryptionService
import boonai.common.service.security.getProjectId
import java.util.UUID
import javax.annotation.PostConstruct
import javax.persistence.EntityManager
import javax.sql.DataSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataRetrievalFailureException
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.util.concurrent.TimeUnit

@Repository("apiKeyRepository")
interface ApiKeyRepository : JpaRepository<ApiKey, UUID> {

    fun findAllByProjectId(projectId: UUID): List<ApiKey>

    fun findAllByProjectIdAndHidden(projectId: UUID, hidden: Boolean): List<ApiKey>

    fun findByProjectIdAndId(id: UUID, projectId: UUID): ApiKey

    fun findByAccessKey(accesKey: String): ApiKey?

    @Modifying(clearAutomatically = true)
    @Query("update ApiKey api set api.enabled = ?1 where api.id = ?2")
    fun updateEnabledById(enabled: Boolean, id: UUID)

    @Modifying(clearAutomatically = true)
    fun deleteByProjectId(projectId: UUID)
}

/**
 * A repository class for doing custom queries.
 */
interface ApiKeyCustomRepository {

    /**
     * Get the signing key for the given key Id.  Uses authed users project.
     */
    fun getSigningKey(id: UUID): SigningKey

    /**
     * Get the signing key for the given key name.  Uses authed users project.
     */
    fun getSigningKey(name: String): SigningKey

    /**
     * Load the validation key by the unique accessKey value.  This is used for authentication.
     */
    fun getValidationKey(accessKey: String): ValidationKey

    /**
     * Find a single result. Throws if there is not one result and one result only.
     */
    fun findOne(filter: ApiKeyFilter): ApiKey

    /**
     * Search for ApiKey using an [ApiKeyFilter]
     */
    fun search(filter: ApiKeyFilter): PagedList<ApiKey>

    /**
     * Invalidate key cache.
     */
    fun invalidateCache(accessKey: String)
}

@Repository
class ApiKeyCustomRepositoryImpl(
    val dataSource: DataSource,
    val encryptionService: EncryptionService
) : ApiKeyCustomRepository {

    lateinit var jdbc: JdbcTemplate

    @Autowired
    lateinit var entityManager: EntityManager

    private val validationKeyCache = CacheBuilder.newBuilder()
        .initialCapacity(128)
        .maximumSize(1024)
        .concurrencyLevel(8)
        .expireAfterWrite(10, TimeUnit.SECONDS)
        .build(object : CacheLoader<String, ValidationKey>() {
            @Throws(Exception::class)
            override fun load(accessKey: String): ValidationKey {
                return jdbc.queryForObject(GET_VALIDATION_KEY, validationKeyMapper, accessKey)
                    ?: throw DataRetrievalFailureException("Invalid API Key")
            }
        })

    @PostConstruct
    fun init() {
        jdbc = JdbcTemplate(dataSource)
    }

    override fun invalidateCache(accessKey: String) {
        validationKeyCache.invalidate(accessKey)
    }

    override fun getSigningKey(id: UUID): SigningKey {
        return jdbc.queryForObject(
            "$GET_SIGNING_KEY WHERE project_id=? AND pk_api_key=?", signingKeyMapper, getProjectId(), id
        )
            ?: throw DataRetrievalFailureException("Invalid API Key")
    }

    override fun getSigningKey(name: String): SigningKey {
        return jdbc.queryForObject(
            "$GET_SIGNING_KEY WHERE project_id=? AND name=?", signingKeyMapper, getProjectId(), name
        )
            ?: throw DataRetrievalFailureException("Invalid API Key")
    }

    override fun getValidationKey(accessKey: String): ValidationKey {
        try {
            return validationKeyCache.get(accessKey)
        } catch (e: Exception) {
            throw DataRetrievalFailureException("Invalid API Key")
        }
    }

    override fun findOne(filter: ApiKeyFilter): ApiKey {
        val pager = JpaQuery(entityManager, filter, ApiKey::class.java)
        return entityManager.createQuery(pager.getQuery()).singleResult
    }

    override fun search(filter: ApiKeyFilter): PagedList<ApiKey> {
        val query = JpaQuery(entityManager, filter, ApiKey::class.java)
        val page = filter.page

        val list = entityManager.createQuery(query.getQuery())
            .setFirstResult(page.from)
            .setMaxResults(page.size)
            .resultList

        val count = entityManager.createQuery(query.getQueryForCount()).singleResult
        return PagedList(page.withTotal(count), list)
    }

    private val validationKeyMapper = RowMapper { rs, _ ->
        ValidationKey(
            rs.getObject("pk_api_key") as UUID,
            rs.getObject("project_id") as UUID,
            rs.getString("access_key"),
            rs.getString("secret_key"),
            rs.getString("name"),
            rs.getString("permissions").split(',').toSet()
        )
    }

    private val signingKeyMapper = RowMapper { rs, _ ->
        SigningKey(rs.getString(1), encryptionService.decryptString(rs.getString(2), ApiKey.CRYPT_VARIANCE))
    }

    companion object {
        const val GET_SIGNING_KEY = "SELECT access_key, secret_key FROM api_key"
        const val GET_VALIDATION_KEY =
            "SELECT pk_api_key, project_id, name, permissions, access_key, secret_key " +
                "FROM api_key WHERE access_key=? and enabled = true"
    }
}
