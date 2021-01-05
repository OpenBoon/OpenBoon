package com.zorroa.archivist.domain

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.index.query.QueryBuilders
import java.math.BigDecimal

@ApiModel(
    "Clip",
    description = "A class to represent video clips."
)
class Clip(

    @ApiModelProperty("The unique ID of the clip.")
    val id: String,

    @ApiModelProperty("The AssetId the clip belongs to.")
    val assetId: String,

    @ApiModelProperty("The Timeline the clip is part of.")
    val timeline: String,

    @ApiModelProperty("The Track the clip is part of.")
    val track: String,

    @ApiModelProperty("The starting point of the video clip")
    val start: BigDecimal,

    @ApiModelProperty("The stopping point of the video clip")
    val stop: BigDecimal,

    @ApiModelProperty("The content contained in the video clip.")
    val content: List<String>,

    @ApiModelProperty("The confidence score that the content is correct.")
    val score: Double
)

class ClipSpec(

    @ApiModelProperty("The AssetId the clip belongs to.")
    val assetId: String,

    @ApiModelProperty("The Timeline the clip is part of.")
    val timeline: String,

    @ApiModelProperty("The Track the clip is part of.")
    val track: String,

    @ApiModelProperty("The starting point of the video clip")
    val start: BigDecimal,

    @ApiModelProperty("The stopping point of the video clip")
    val stop: BigDecimal,

    @ApiModelProperty("The content contained in the video clip.")
    val content: List<String>,

    @ApiModelProperty("The confidence score that the content is correct.")
    val score: Double = 1.0
)

class WebVTTFilter(

    val assetId: String,

    val timelines: List<String>? = null,

    val tracks: List<String>? = null,

    val content: List<String>? = null
) {

    fun getQuery(): QueryBuilder {
        val query = QueryBuilders.boolQuery()
        query.filter().add(QueryBuilders.termQuery("clip.assetId", assetId))

        timelines?.let { it ->
            query.filter().add(QueryBuilders.termsQuery("clip.timeline", it))
        }

        tracks?.let { it ->
            query.filter().add(QueryBuilders.termsQuery("clip.track", it))
        }

        content?.let { tl ->
            val subq = QueryBuilders.boolQuery()
            query.filter().add(subq)
            tl.forEach {
                subq.should(QueryBuilders.matchQuery("clip.content", it))
            }
        }

        return query
    }
}
