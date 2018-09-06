package com.zorroa.common.server

interface NetworkEnvironment {
    fun getPublicUrl(service: String) : String
    fun getBucket(name: String) : String
}

class GoogleAppEngineEnvironment(private val project: String, private val hostTable: Map<String, String>) : NetworkEnvironment {

    override fun getPublicUrl(service: String) : String {
        return hostTable.getOrDefault(service,  "https://$service-dot-$project.appspot.com")
    }

    override fun getBucket(name: String) : String {
        return "$project-$name"
    }
}

class StaticVmEnvironment(private val project: String, private val hostTable: Map<String, String>) : NetworkEnvironment {

    override fun getPublicUrl(service: String) : String {
        return hostTable.getOrDefault(service, "https://$service-$project.zorroa.com")
    }

    override fun getBucket(name: String) : String {
        return "$project-$name"
    }
}

class DockerComposeEnvironment(private val hostTable: Map<String, String>) : NetworkEnvironment {

    override fun getPublicUrl(service: String): String {
        return  hostTable.getOrDefault(service, "http://$service")
    }

    override fun getBucket(name: String): String {
        return name
    }
}
