package com.zorroa.archivist.repository

import com.zorroa.archivist.domain.PipelineMod
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface PipelineModDao : JpaRepository<PipelineMod, UUID> {

    /**
     * Get a [PipelineMod] by name or return null.
     */
    fun getByName(name: String): PipelineMod?

    /**
     * Get the list of mods by Id.  It may be better to use the [PipelineModService.getByIds]
     * method which throws if all Ids are not found.
     */
    fun findByIdIn(ids: Collection<UUID>): List<PipelineMod>

    /**
     * Get the list of mods by name.  It may be better to use the [PipelineModService.getByNames]
     * method which throws if all Ids are not found.
     */
    fun findByNameIn(names: Collection<String>): List<PipelineMod>
}
