package com.zorroa.common.schema

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*

class MediaClip {

    /**
     * The parent ID of the clip.  Usually references another asset.
     */
    private var parent: String? = null

    /**
     * The type of clip.  For example: image, page, video, flipbook.
     */
    private var type: String? = null

    /**
     * The name of the clip. Optional
     */
    private var name: String? = null

    /**
     * An optional timeline name.
     */
    private var timeline: String? = null

    private var start: BigDecimal? = null

    private var stop: BigDecimal? = null

    /**
     * The duration of the clip.
     */
    private var length: BigDecimal? = null

    constructor() {}

    @JvmOverloads constructor(parent: String, type: String, start: String, stop: String = start) : this(parent, type, BigDecimal(start), BigDecimal(stop)) {}

    constructor(parent: String, type: String, start: Double, stop: Double) : this(parent, type, BigDecimal(start.toString()), BigDecimal(stop.toString())) {}


    @JvmOverloads constructor(parent: String, type: String, start: Int, stop: Int = start) : this(parent, type, BigDecimal(start), BigDecimal(stop)) {}

    @JvmOverloads constructor(parent: String, type: String, start: BigDecimal, stop: BigDecimal = start) {
        this.parent = parent
        this.type = type
        this.start = start.setScale(3, RoundingMode.DOWN)
        this.stop = stop.setScale(3, RoundingMode.DOWN)
        this.length = this.stop!!.subtract(this.start!!).add(BigDecimal.ONE)
    }

    fun getParent(): String? {
        return parent
    }

    fun setParent(parent: String): MediaClip {
        this.parent = parent
        return this
    }

    fun getType(): String? {
        return type
    }

    fun setType(type: String): MediaClip {
        this.type = type
        return this
    }

    fun getTimeline(): String? {
        return timeline
    }

    fun setTimeline(timeline: String): MediaClip {
        this.timeline = timeline
        return this
    }

    fun getStart(): BigDecimal? {
        return start
    }

    fun setStart(start: BigDecimal): MediaClip {
        this.start = start.setScale(3, RoundingMode.DOWN)
        return this
    }

    fun getStop(): BigDecimal? {
        return stop
    }

    fun setStop(stop: BigDecimal): MediaClip {
        this.stop = stop.setScale(3, RoundingMode.DOWN)
        return this
    }

    fun getLength(): BigDecimal? {
        return length
    }

    fun setLength(length: BigDecimal): MediaClip {
        this.length = length.setScale(3, RoundingMode.DOWN)
        return this
    }

    fun getName(): String? {
        return name
    }

    fun setName(name: String): MediaClip {
        this.name = name
        return this
    }
}

class MediaSchema {

    // General properties
    var width: Int? = null
    var height: Int? = null
    var pages: Int? = null
    var attrs: Map<String, Any>? = null
    var orientation: String? = null
    var timeCreated: Date? = null
    var timeModified: Date? = null

    // Video properties
    var duration: BigDecimal? = null
    var frames: Int? = null
    var frameRate: BigDecimal? = null

    // Video and Sound
    var audioBitRate: Int? = null
    var audioChannels: Int? = null

    // Titles
    var title: String? = null
    var description: String? = null
    var author: String? = null

    // String content of a document, can be large.
    var content: String? = null

    // The clip if any
    var clip: MediaClip? = null

    // Any keywords extracted from media
    var keywords: MutableSet<String>? = null
}
