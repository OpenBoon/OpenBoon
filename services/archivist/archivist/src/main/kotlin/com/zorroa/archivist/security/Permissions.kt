package com.zorroa.archivist.security

import java.util.UUID

object Role {
    /**
     * Superadmins can impersonate API calls to any project.
     */
    val SUPERADMIN = "SuperAdmin"

    /**
     * Project admins can launch jobs, create pipelines, etc.
     */
    val PROJADMIN = "ProjectAdmin"

    /**
     * Project admins can launch jobs, create pipelines, etc.
     */
    val JOBRUNNER = "JobRunner"
}

object Perm {

    /**
     * Can search/get assets.
     */
    val ASSETS_READ = "AssetsRead"

    /**
     * Import asset data.
     */
    val ASSETS_IMPORT = "AssetsImport"

    /**
     * Store files assoicated with an asset.
     */
    val STORAGE_CREATE = "StorageCreate"

    /**
     * Store files assoicated with an asset.
     */
    val STORAGE_ADMIN = "StorageAdmin"

    /**
     * Can hit server monitoring endpoints.
     */
    val MONITOR_SERVER = "MonitorServer"
}

object KnownKeys {

    /**
     * The job runner key has the correct perms to run a job.
     */
    val JOB_RUNNER = "job-runner"

    /**
     * The background-thread key doesn't actually exist but is an
     * identifier used for running threads tied to a project.
     */
    val BACKGROUND_THREAD = "background-thread"

    /**
     * A special key ID used for when we forge a key for a
     * background thread.
     */
    val SUKEY = UUID.fromString("00000000-1234-1234-1234-000000000000")
}