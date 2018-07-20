package com.zorroa.common.util

fun getPublicUrl() : String {
    val env =System.getenv()
    return if (env.get("GCLOUD_PROJECT") != null) {
        val project = env["GCLOUD_PROJECT"]
        val service = env["GAE_SERVICE"]
        "https://$service-dot-$project.appspot.com"
    } else {
        "http://localhost:8080"
    }
}
