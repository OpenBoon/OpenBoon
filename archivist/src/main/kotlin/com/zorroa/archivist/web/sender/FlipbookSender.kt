package com.zorroa.archivist.web.sender

import com.zorroa.archivist.service.SearchService
import com.zorroa.sdk.domain.Document
import com.zorroa.sdk.search.AssetSearch
import com.zorroa.sdk.search.AssetSearchOrder
import com.zorroa.sdk.util.Json
import javax.servlet.http.HttpServletResponse

data class Frame(val id: String, val frame: Int)

class FlipBook {
    val frames : MutableList<Frame> = mutableListOf()

    fun getSize() : Int {
        return frames.size
    }
}

class FlipbookSender constructor(
        val assetId: String,
        val searchService : SearchService) {

    fun serveResource(response: HttpServletResponse) {
        response.contentType = "application/json"
        response.setHeader("Content-Disposition", "inline;filename=\"$assetId.zfb")

        val search = AssetSearch()
        search.size = 100
        search.addToFilter().addToTerms("source.clip.parent.raw", assetId)
        search.order = listOf(AssetSearchOrder("source.clip.frame.start").setAscending(true))
        search.fields = arrayOf("_id",  "source.clip.frame")

        val result = FlipBook()
        val rsp = searchService.search(search)
        for (hit in rsp.hits.hits) {
            val doc = Document(hit.source)
            result.frames.add(Frame(hit.id, doc.getAttr("source.clip.frame.start")))
        }

        Json.Mapper.writeValue(response.outputStream, result)
    }
}
