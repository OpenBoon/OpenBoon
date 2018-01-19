package com.zorroa.archivist.domain

import com.zorroa.sdk.search.AssetSearch

enum class RequestType {
    ExportAccess,
    WriteAccess
}

class RequestSpec {

    var search : AssetSearch? = null
    var type : List<RequestType>? = null
    var count: Int = 0
    var comment : String? = null

}

class Request {
    var id : Int? = null
    var search : AssetSearch? = null
    var types : Array<RequestType>? = null
    var userCreated : UserBase? = null
    var timeCreated : Long? = null
}
