package com.zorroa.archivist.repository

import com.zorroa.archivist.domain.PipelineMod
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface PipelineModDao : JpaRepository<PipelineMod, UUID> {
    fun getByName(name: String): PipelineMod?
    fun findByIdIn(ids: Collection<UUID>): List<PipelineMod>
}
