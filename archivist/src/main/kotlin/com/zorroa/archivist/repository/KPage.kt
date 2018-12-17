package com.zorroa.common.repository

import com.zorroa.archivist.search.Scroll
import java.util.*
import java.util.stream.Stream

class KPage constructor(from: Int?, size: Int?=pageSizeDefault) {

    var from : Int = from ?: 0
    var size : Int

    init {
        this.size = size ?: pageSizeDefault
    }

    var totalCount : Long = 0

    constructor(): this(0, pageSizeDefault)

    companion object {
        const val pageSizeDefault : Int = 50
    }
}

class KPagedList<T> : Iterable<T> {

    var list: List<T>
    var page: KPage

    var aggregations: Map<String, Any>? = null
    var scroll: Scroll? = null

    constructor() {
        list = ArrayList()
        page = KPage()
    }

    constructor(count: Long, page: KPage, list: List<T>) {
        this.page = page
        this.page.totalCount = count
        this.list = list
    }

    constructor(page: KPage, list: List<T>) {
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

