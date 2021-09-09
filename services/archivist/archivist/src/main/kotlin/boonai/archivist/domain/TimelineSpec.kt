package boonai.archivist.domain

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import org.elasticsearch.action.bulk.BulkResponse
import java.math.BigDecimal
import java.security.MessageDigest
import java.util.Base64
import java.util.UUID

@ApiModel("ClipSpec", description = "Properties for defining a video clip.")
class TimelineClipSpec(

    @ApiModelProperty("The starting point of the video clip")
    val start: BigDecimal,

    @ApiModelProperty("The stopping point of the video clip")
    val stop: BigDecimal,

    @ApiModelProperty("The content contained in the video clip.")
    val content: List<String>,

    @ApiModelProperty("The confidence score that the content is correct.")
    val score: Double,

    @ApiModelProperty("A Bbox assocated with the clip, if any.")
    val bbox: List<Double>? = null
)

@ApiModel("TrackSpec", description = "Properties for defining a timeline Track. Tracks contain clips.")
class TrackSpec(

    @ApiModelProperty("The name of the track")
    val name: String,

    @ApiModelProperty("The list of clips in the track.")
    val clips: List<TimelineClipSpec>
)

@ApiModel("TimelineSpec", description = "A TimelineSpec is used to batch create video clips.")
class TimelineSpec(

    @ApiModelProperty("The AssetId to attach clips to.")
    val assetId: String,

    @ApiModelProperty("The name of the timeline")
    val name: String,

    @ApiModelProperty("A list of tracks.")
    val tracks: List<TrackSpec>,

    @ApiModelProperty("Replace all tracks for the timeline with this new timeline.")
    val replace: Boolean
)

@ApiModel("CreateClipFailure", description = "A clip creation error.")
class CreateClipFailure(
    @ApiModelProperty("The ID of clip that failed.")
    val id: String,

    @ApiModelProperty("The failure message.")
    val message: String,
)

@ApiModel("CreateTimelineResponse", description = "The response sent after creating video clips with a timeline")
class CreateTimelineResponse(

    @ApiModelProperty("The AssetId")
    var assetId: String,

    @ApiModelProperty("The number of clips created")
    var created: Long = 0,

    @ApiModelProperty("The number of clips that failed to be created")
    var failed: MutableList<CreateClipFailure> = mutableListOf(),

    @ApiModelProperty("The jobId for the analysis job if created.")
    var jobId: UUID? = null,

    @ApiModelProperty("The taskId for the analysis task if created.")
    var taskId: UUID? = null

) {
    fun handleBulkResponse(rsp: BulkResponse) {
        if (rsp.hasFailures()) {
            created += rsp.items.count { !it.isFailed }
            rsp.items.filter { it.isFailed }.forEach {
                failed.add(CreateClipFailure(it.id, it.failureMessage))
            }
        } else {
            created += rsp.items.size
        }
    }
}

/**
 * A class for determining a video clip unique Id.
 */
class ClipIdBuilder(
    val asset: Asset,
    val timeline: String,
    val track: String,
    val start: BigDecimal,
    val stop: BigDecimal
) {

    fun buildId(): String {
        /**
         * Nothing about the order of these statements
         * can ever change or duplicate assets will be
         * created.
         */
        val digester = MessageDigest.getInstance("SHA-256")
        digester.update(asset.id.toByteArray())
        digester.update(timeline.toByteArray())
        digester.update(track.toByteArray())
        digester.update(start.toString().toByteArray())
        digester.update(stop.toString().toByteArray())

        // Clamp the size to 32, 48 is bit much and you still
        // get much better resolution than a UUID.  We could
        // also up it on shared indexes but probably not necessary.
        return Base64.getUrlEncoder()
            .encodeToString(digester.digest()).trim('=').substring(0, 32)
    }
}
