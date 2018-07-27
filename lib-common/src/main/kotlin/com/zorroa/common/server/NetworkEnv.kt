package com.zorroa.common.server

/**
 * Return the URL to self.
 */
fun getPublicUrl() : String {
    val env = System.getenv()
    return if (env.get("GCLOUD_PROJECT") != null) {
        val project = env["GCLOUD_PROJECT"]
        val service = env["GAE_SERVICE"]
        "https://$service-dot-$project.appspot.com"
    } else {
        "http://localhost:8080"
    }
}

/**
 * Return the URL to the given microservice
 */
fun getPublicUrl(service: String) : String {
    val env= System.getenv()
    return if (env.get("GCLOUD_PROJECT") != null) {
        val project = env["GCLOUD_PROJECT"]
        "https://$service-dot-$project.appspot.com"
    } else {
        "http://localhost:8080"
    }
}

fun getJobDataBucket() : String {
    val env= System.getenv()
    return if (env.get("GCLOUD_PROJECT") != null) {
        val project = env["GCLOUD_PROJECT"]
        return "$project-zorroa-job-data"
    }
    else {
        "zorroa-job-data"
    }
}
