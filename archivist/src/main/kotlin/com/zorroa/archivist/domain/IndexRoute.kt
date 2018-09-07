package com.zorroa.common.domain

import java.util.*

data class IndexRoute (
        val id: UUID,
        val name : String,
        val clusterUrl: String,
        val indexName: String,
        val routingKey : String?
)
{

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val route = o as IndexRoute
        return id === route.id
    }

    override fun hashCode(): Int {
        return Objects.hash(id)
    }
}

