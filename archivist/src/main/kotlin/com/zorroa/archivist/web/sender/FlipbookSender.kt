package com.zorroa.archivist.web.sender

import com.zorroa.archivist.service.SearchService
import com.zorroa.sdk.search.AssetSearch
import com.zorroa.sdk.util.Json
import javax.servlet.http.HttpServletResponse

data class Frame(
        val id: String,
        val frame: Int,
        val filename: String)

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
        response.setHeader("Content-Disposition", "inline;filename=\"flipbook-$assetId.json")

        val search = AssetSearch()
        search.size = 100
        search.addToFilter().addToTerms("media.clip.parent.raw", assetId)
        search.addToFilter().addToTerms("media.clip.type.raw", "flipbook")
        search.fields = arrayOf("_id",  "source", "media")

        val result = FlipBook()
        for (doc in searchService.scanAndScroll(search, 0)) {
            result.frames.add(Frame(doc.id,
                    doc.getAttr("media.clip.start"),
                    doc.getAttr("source.filename")))
        }
        result.frames.sortBy { it.frame }
        Json.Mapper.writeValue(response.outputStream, result)
    }
}
