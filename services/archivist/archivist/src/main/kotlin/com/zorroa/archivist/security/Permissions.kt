package com.zorroa.archivist.security

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
    val JOBRUNNER = "ProjectAdmin"
}

object Perm {

    /**
     * Can search/get assets.
     */
    val READ_ASSETS = "ReadAssets"

    /**
     * Import asset data.
     */
    val IMPORT_ASSETS = "ImportAssets"

    /**
     * Can hit server monitoring endpoints.
     */
    val MONITOR_SERVER = "MonitorServer"
}
