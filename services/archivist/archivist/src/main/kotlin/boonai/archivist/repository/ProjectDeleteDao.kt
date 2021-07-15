package boonai.archivist.repository

import org.springframework.stereotype.Repository
import java.util.UUID

interface ProjectDeleteDao {

    fun deleteProjectRelatedObjects(projectId: UUID)

    companion object {
        val tables = listOf(
            "index_route",
            "dataset",
            "project_quota",
            "project_quota_time_series",
            "field",
            "module",
            "credentials",
            "pipeline",
            "model",
            "job",
            "datasource",
            "webhook",
            "project"
        )
    }
}

@Repository
class ProjectDeleteCustomDao : ProjectDeleteDao, AbstractDao() {

    private fun deleteXModuleDataSourceByProject(projectId: UUID) {
        jdbc.update(
            "DELETE FROM x_module_datasource " +
                "WHERE pk_module IN (SELECT pk_module FROM module WHERE pk_project = ?) " +
                "OR pk_datasource in (SELECT pk_datasource FROM datasource WHERE pk_project = ?)",
            projectId, projectId
        )
    }

    private fun deleteXModulePipelineByProject(projectId: UUID) {
        jdbc.update(
            "DELETE FROM x_module_pipeline " +
                "WHERE pk_module IN (SELECT pk_module FROM module WHERE pk_project = ?) " +
                "OR pk_pipeline IN (SELECT pk_pipeline FROM pipeline WHERE pk_project = ?)",
            projectId, projectId
        )
    }

    private fun deleteXCredentialsDatasourceByProject(projectId: UUID) {
        jdbc.update(
            "DELETE FROM x_credentials_datasource " +
                "WHERE pk_credentials IN (SELECT pk_credentials FROM credentials WHERE pk_project = ?) " +
                "OR pk_datasource IN (SELECT pk_datasource FROM datasource WHERE pk_project = ?)",
            projectId, projectId
        )
    }

    private fun deleteXCredentialsJob(projectId: UUID) {
        jdbc.update(
            "DELETE FROM x_credentials_job " +
                "WHERE pk_credentials IN (SELECT pk_credentials FROM credentials WHERE pk_project = ?) " +
                "OR pk_job IN (SELECT pk_job from JOB WHERE pk_project = ?)",
            projectId, projectId
        )
    }

    private fun deleteTaskDependency(projectId: UUID) {
        jdbc.update(
            "DELETE FROM depend " +
                "WHERE pk_job_depend_on IN (SELECT pk_job FROM job WHERE pk_project = ?) " +
                "OR pk_job_depend_er IN (SELECT pk_job FROM job WHERE pk_project = ?)",
            projectId, projectId
        )
    }

    private fun deleteJobStatByProject(projectId: UUID) {
        jdbc.update(
            "DELETE FROM job_stat where pk_job in " +
                "(select pk_job from job where pk_project = ?)",
            projectId
        )
    }

    private fun deleteTaskByProject(projectId: UUID) {
        jdbc.update(
            "DELETE FROM task where pk_job in " +
                "(select pk_job from job where pk_project = ?)",
            projectId
        )
    }

    private fun deleteTaskErrorByProject(projectId: UUID) {
        jdbc.update(
            "DELETE FROM task_error where pk_job in " +
                "(select pk_job from job where pk_project = ?)",
            projectId
        )
    }

    private fun deleteTaskStatByProject(projectId: UUID) {
        jdbc.update(
            "DELETE FROM task_stat where pk_job in " +
                "(select pk_job from job where pk_project = ?)",
            projectId
        )
    }

    override fun deleteProjectRelatedObjects(projectId: UUID) {
        // Index route should be closed and deleted separately
        deleteXModuleDataSourceByProject(projectId)
        deleteXModulePipelineByProject(projectId)
        deleteXCredentialsDatasourceByProject(projectId)
        deleteXCredentialsJob(projectId)

        deleteTaskDependency(projectId)
        deleteJobStatByProject(projectId)
        deleteTaskByProject(projectId)
        deleteTaskErrorByProject(projectId)
        deleteTaskStatByProject(projectId)

        ProjectDeleteDao.tables.forEach {
            jdbc.update("DELETE from $it WHERE pk_project=?", projectId)
        }
    }
}
