package com.zorroa.archivist.domain

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.Base64

@ApiModel("Clip", description = "Defines a subsection of an Asset to process, " +
    "for example a page of a document.")
class Clip(

    @ApiModelProperty("The type of clip, this is typically 'page' or 'scene' but can be anything")
    val type: String,

    @ApiModelProperty("The starting point of the clip")
    val start: Float,

    @ApiModelProperty("The ending point of a clip.")
    val stop: Float,

    @ApiModelProperty("The type of clip, this is typically 'page' or 'scene' but can be anything")
    val timeline: String? = null
)
{
    /**
     * Calculate the clip length.  If the length is 0 then make it 1
     */
    val length = if (stop - start == 0f) { 1f } else { stop - start}

    /**
     * The pile Id.
     */
    var pile: String? = null

    /**
     * Generate a clip group id.  The group id is built using:
     *
     * - type
     * - timeline
     * - source.path
     * - source.filesize
     * - media.timeCreated
     *
     * If any of these values are null then they are skipped.
     */
    fun generatePileId(asset: Asset) : String {
        val path = asset.getAttr<String?>("source.path")
        val size = asset.getAttr<Int?>("source.filesize")
        val time = asset.getAttr<String?>("media.timeCreated")

        /**
         * Modifying this logic may break clip grouping
         */
        val digester = MessageDigest.getInstance("SHA")
        digester.update(type.toByteArray())
        timeline?.let {
            digester.update(it.toByteArray())
        }

        path?.let {
            digester.update(path.toByteArray())
        }
        size?.let {
            val buf = ByteBuffer.allocate(4)
            buf.putInt(it)
            digester.update(buf.array())
        }
        time?.let {
            digester.update(it.toByteArray())
        }
        return Base64.getUrlEncoder()
            .encodeToString(digester.digest()).trim('=')
    }

    fun setGeneratedProperties(asset: Asset) {
        this.pile = generatePileId(asset)
    }
}
