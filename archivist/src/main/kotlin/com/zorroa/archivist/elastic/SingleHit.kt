package com.zorroa.archivist.elastic

import org.elasticsearch.action.get.GetResponse
import org.elasticsearch.search.SearchHit

class SingleHit  {

    val id : String
    val source: Map<String, Any>
    val version : Long
    val type : String
    val score : Float

    constructor(rsp: GetResponse) {
        id = rsp.id
        source = rsp.sourceAsMap
        version = rsp.version
        type = rsp.type
        score = 0.0f
    }

    constructor(rsp: SearchHit) {
        id = rsp.id
        source = rsp.sourceAsMap
        version = rsp.version
        type = rsp.type
        score = rsp.score
    }

}
