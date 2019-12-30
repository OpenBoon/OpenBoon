package com.zorroa.auth.server.repository

import com.zorroa.auth.server.domain.ApiKey
import com.zorroa.auth.server.domain.ApiKeyFilter
import java.util.UUID
import javax.persistence.EntityManager
import javax.persistence.TypedQuery
import javax.persistence.criteria.CriteriaBuilder
import javax.persistence.criteria.Predicate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository("apiKeyRepository")
interface ApiKeyRepository : JpaRepository<ApiKey, UUID> {

    fun findAllByProjectId(projectId: UUID): List<ApiKey>

    fun findByProjectIdAndKeyId(keyId: UUID, projectId: UUID): ApiKey
}

/**
 * A repository class for doing custom queries.
 */
interface ApiKeySearchRepository {

    /**
     * Find a single result. Throws if there is not one result and one result only.
     */
    fun findOne(filter: ApiKeyFilter): ApiKey
}

@Repository
class ApiKeySearchRepositoryImpl : ApiKeySearchRepository {

    @Autowired
    lateinit var entityManager: EntityManager

    override fun findOne(filter: ApiKeyFilter): ApiKey {
        return builQuery(filter).singleResult
    }

    fun builQuery(filter: ApiKeyFilter): TypedQuery<ApiKey> {
        val cb = entityManager.criteriaBuilder
        val criteria = cb.createQuery(ApiKey::class.java)
        val root = criteria.from(ApiKey::class.java)
        val where = mutableListOf<Predicate>()

        filter.keyIds?.let {
            val ic: CriteriaBuilder.In<UUID> = cb.`in`(root.get("keyId"))
            it.forEach { v ->
                ic.value(v)
            }
            where.add(ic)
        }

        filter.projectIds?.let {
            val ic: CriteriaBuilder.In<UUID> = cb.`in`(root.get("projectId"))
            it.forEach { v ->
                ic.value(v)
            }
            where.add(ic)
        }

        filter.names?.let {
            val ic: CriteriaBuilder.In<String> = cb.`in`(root.get("name"))
            it.forEach { v ->
                ic.value(v)
            }
            where.add(ic)
        }

        return entityManager.createQuery(criteria.select(root).where(*where.toTypedArray()))
    }
}
