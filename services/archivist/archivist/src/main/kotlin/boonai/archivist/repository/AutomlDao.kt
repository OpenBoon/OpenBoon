package boonai.archivist.repository

import boonai.archivist.domain.AutomlSession
import boonai.archivist.domain.AutomlSessionState
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface AutomlDao : JpaRepository<AutomlSession, UUID> {

    fun findByState(state: AutomlSessionState): List<AutomlSession>

    @Modifying(clearAutomatically = true)
    @Query("update AutomlSession s set s.state = 2, s.error = ?1 WHERE s.id = ?2")
    fun setError(error: String, id: UUID): Int

    @Modifying(clearAutomatically = true)
    @Query("update AutomlSession s set s.state = 1, s.automlModel = ?1 WHERE s.id = ?2 AND s.state = 0")
    fun setFinished(automlModel: String, id: UUID): Int
}
