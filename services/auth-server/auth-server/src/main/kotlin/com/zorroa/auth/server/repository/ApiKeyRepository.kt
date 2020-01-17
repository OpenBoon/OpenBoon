package com.zorroa.auth.server.repository

import com.zorroa.auth.server.domain.ApiKey
import com.zorroa.auth.server.domain.ApiKeyFilter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID
import javax.persistence.EntityManager

@Repository("apiKeyRepository")
interface ApiKeyRepository : JpaRepository<ApiKey, UUID> {

    fun findAllByProjectId(projectId: UUID): List<ApiKey>

    fun findByProjectIdAndId(id: UUID, projectId: UUID): ApiKey

    fun findByAccessKey(accesKey: String): ApiKey?
}

/**
 * A repository class for doing custom queries.
 */
interface ApiKeyCustomRepository {

    /**
     * Find a single result. Throws if there is not one result and one result only.
     */
    fun findOne(filter: ApiKeyFilter): ApiKey

    fun search(filter: ApiKeyFilter): PagedList<ApiKey>
}

@Repository
class ApiKeyCustomRepositoryImpl : ApiKeyCustomRepository {

    @Autowired
    lateinit var entityManager: EntityManager

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
}
