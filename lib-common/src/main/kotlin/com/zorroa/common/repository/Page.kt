package com.zorroa.common.repository

import com.zorroa.common.search.Scroll
import java.util.*
import java.util.stream.Stream



class Page constructor(var from: Int, var size: Int=pageSizeDefault) {

    var totalCount : Long = 0

    constructor(): this(0, pageSizeDefault)

    companion object {
        const val pageSizeDefault : Int = 50
    }
}

class KPagedList<T> : Iterable<T> {

    var list: List<T>
    var page: Page

    var aggregations: Map<String, Any>? = null
    var scroll: Scroll? = null

    constructor() {
        list = ArrayList()
        page = Page()
    }

    constructor(page: Page, list: List<T>) {
        this.page = page
        this.list = list
    }

    fun size(): Int {
        return list.size
    }

    operator fun get(idx: Int): T {
        return list[idx]
    }

    fun stream(): Stream<T> {
        return list.stream()
    }


    override fun iterator(): Iterator<T> {
        return list.iterator()
    }
}

