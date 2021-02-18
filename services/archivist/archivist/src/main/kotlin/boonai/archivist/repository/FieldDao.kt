package boonai.archivist.repository

import boonai.archivist.domain.Field
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface FieldDao : JpaRepository<Field, UUID> {

    fun getAllByProjectId(projectId: UUID): List<Field>
    fun getByProjectIdAndId(projectId: UUID, id: UUID): Field
    fun getByProjectIdAndName(projectId: UUID, name: String): Field
}
