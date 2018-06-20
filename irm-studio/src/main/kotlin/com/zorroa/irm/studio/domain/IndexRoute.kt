package com.zorroa.irm.studio.domain

import java.util.*

data class IndexRoute (
        val id: UUID,
        val name : String,
        val clusterUrl: String,
        val indexName: String,
        val routingKey : String?
)
{
    fun getCompanyId(): Long {
        return name.toLong()
    }
}

