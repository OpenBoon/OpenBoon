package com.zorroa.archivist.repository

import com.zorroa.archivist.domain.Credentials
import com.zorroa.archivist.domain.DataSet
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface DataSetDao : JpaRepository<DataSet, UUID> {
    fun getOneByProjectIdAndId(projectId: UUID, id: UUID): DataSet
    fun getOneByProjectIdAndName(projectId: UUID, name: String): DataSet
}