package com.zorroa.auth.repository

import com.zorroa.auth.domain.ApiKey
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository("apiKeyRepository")
interface ApiKeyRepository : JpaRepository<ApiKey, UUID> {

    fun findAllByProjectId(projectId: UUID) : List<ApiKey>

    fun findByKeyIdAndProjectId(keyId: UUID, projectId: UUID) : ApiKey

}
