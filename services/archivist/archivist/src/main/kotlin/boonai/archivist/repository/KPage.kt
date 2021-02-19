package boonai.archivist.repository

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.util.stream.Stream

@ApiModel("KPage", description = "Used to describe results page when paginating responses.")
class KPage constructor(from: Int?, size: Int? = pageSizeDefault) {

    @ApiModelProperty("Result index to start from.")
    var from: Int = from ?: 0

    @ApiModelProperty("Number of results per page.")
    var size: Int

    @ApiModelProperty("Disable paging", hidden = true)
    var disabled = false

    init {
        this.size = (size ?: pageSizeDefault).coerceAtMost(100)
    }

    var totalCount: Long = 0

    constructor() : this(0, pageSizeDefault)

    companion object {
        const val pageSizeDefault: Int = 50
    }
}

@ApiModel("KPaged List", description = "Paginated list of items.")
class KPagedList<T> : Iterable<T> {

    @ApiModelProperty("List of items to paginate.")
    var list: List<T>

    @ApiModelProperty("Current page.")
    var page: KPage

    @ApiModelProperty("Aggregations to apply.")
    var aggregations: Map<String, Any>? = null

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
