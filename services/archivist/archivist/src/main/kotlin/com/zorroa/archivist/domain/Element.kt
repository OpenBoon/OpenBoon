package com.zorroa.archivist.domain

import com.fasterxml.jackson.core.type.TypeReference
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel(
    "Element", description = "Defines a unique entity with an asset's visual representation, " +
        "for example an object or a face. Elements are unique by a specific type, rect, and analysis." +
        "labels. "
)
class Element(

    @ApiModelProperty("The type of element, allowed to be arbitrary but 'object' and 'face' are most common.")
    val type: String,
    @ApiModelProperty("The type of analysis that produced the element")
    val analysis: String,
    @ApiModelProperty("The rectangle of the element, if any.")
    val rect: List<Int>? = null,
    @ApiModelProperty("Labels, if any, generated by the element analysis.")
    val labels: List<String>? = null,
    @ApiModelProperty("An optional prediction score if a prediction was made.")
    val score: Double? = null,
    @ApiModelProperty("The region of the image the element is in, NW, SW, SE, SW, and CENTER.")
    val regions: List<String>? = null,
    @ApiModelProperty("An optional file name assocated with the element, which will be in the 'files' namespace")
    val proxy: String? = null,
    @ApiModelProperty("A similarity vector")
    val vector: String? = null
) {
    /**
     * Elements with the same type, labels, rect and file are considered unique.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Element

        if (type != other.type) return false
        if (analysis != other.analysis) return false
        if (rect != other.rect) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + (analysis.hashCode())
        result = 31 * result + (rect?.joinToString(",")?.hashCode() ?: 0)
        return result
    }

    companion object {
        val JSON_SET_OF: TypeReference<Set<Element>> = object : TypeReference<Set<Element>>() {}
    }
}
