package com.zorroa.archivist.domain

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.security.MessageDigest
import java.util.Base64

@ApiModel(
    "Clip", description = "Defines a subsection of an Asset that was processed, " +
        "for example a page of a document."
)
class Clip(

    @ApiModelProperty("The type of clip, this is typically 'page' or 'scene' but can be anything")
    val type: String,

    @ApiModelProperty("The starting point of the clip")
    val start: Float,

    @ApiModelProperty("The ending point of a clip.")
    val stop: Float,

    @ApiModelProperty("An optional timeline name for the clip in case case multiple [ClipSpec] configurations collide.")
    val timeline: String? = null
) {
    init {
        if (stop - start < 0f) {
            throw IllegalStateException("Clip stop cannot be behind clip start.")
        }
    }

    @ApiModelProperty("The length of the the clip, this is auto-calculated")
    val length = if (stop - start == 0f) {
        1f
    } else {
        val x = (stop - start)
        0.01f * kotlin.math.ceil(x * 100.0f)
    }

    @ApiModelProperty("A unique identifier for a related set of clips on a specific timeline.  This can be overridden")
    var pile: String? = null

    @ApiModelProperty("The asset ID used for the clip source", hidden = true)
    var sourceAssetId: String? = null

    /**
     * Generate a clip pile id.  The group id is built using:
     *
     * - source asset id
     * - type
     * - timeline
     *
     * If any of these values are null then they are skipped.
     */
    fun putInPile(assetId: String): String {
        /**
         * Modifying this logic may break clip grouping
         */
        val digester = MessageDigest.getInstance("SHA")
        digester.update(assetId.toByteArray())
        digester.update(type.toByteArray())
        timeline?.let {
            digester.update(it.toByteArray())
        }
        sourceAssetId = assetId
        val pileId = Base64.getUrlEncoder()
            .encodeToString(digester.digest()).trim('=')
        pile = pileId
        return pileId
    }
}
