package boonai.archivist.repository

import boonai.archivist.domain.BoonLib
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface BoonLibDao : JpaRepository<BoonLib, UUID>
