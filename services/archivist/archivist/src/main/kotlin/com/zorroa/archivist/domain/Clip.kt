package com.zorroa.archivist.domain

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.math.BigDecimal
import java.security.MessageDigest
import java.util.Base64

@ApiModel(
    "Clip",
    description = "Defines a subsection of an Asset that was processed, " +
        "for example a page of a document."
)
class Clip(

    @ApiModelProperty("The type of clip, this is typically 'page' or 'scene' but can be anything")
    val type: String,

    start: BigDecimal,

    stop: BigDecimal,

    @ApiModelProperty("An optional track name for the clip in case case multiple [ClipSpec] configurations collide.")
    val track: String? = null
) {

    @ApiModelProperty("The starting point of the clip")
    val start: BigDecimal = start.setScale(3, java.math.RoundingMode.HALF_UP)

    @ApiModelProperty("The ending point of a clip.")
    val stop: BigDecimal = stop.setScale(3, java.math.RoundingMode.HALF_UP)

    @ApiModelProperty("The length of the the clip, this is auto-calculated")
    val length: BigDecimal = if (this.start == this.stop) {
        BigDecimal.ONE.setScale(3, java.math.RoundingMode.HALF_UP)
    } else {
        (this.stop - this.start).setScale(3, java.math.RoundingMode.HALF_UP)
    }

    init {
        if (stop - start < BigDecimal.ZERO) {
            throw IllegalStateException("Clip stop cannot be behind clip start.")
        }
    }

    @ApiModelProperty("A unique identifier for a related set of clips on a specific track.  This can be overridden")
    var pile: String? = null

    @ApiModelProperty("The asset ID used for the clip source", hidden = true)
    var sourceAssetId: String? = null

    /**
     * Generate a clip pile id.  The group id is built using:
     *
     * - source asset id
     * - type
     * - track
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
        track?.let {
            digester.update(it.toByteArray())
        }
        sourceAssetId = assetId
        val pileId = Base64.getUrlEncoder()
            .encodeToString(digester.digest()).trim('=')
        pile = pileId
        return pileId
    }

    companion object {

        /**
         * The track name of a full video clip.
         */
        const val TRACK_FULL = "full"
    }
}
