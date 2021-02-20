package boonai.archivist.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import boonai.archivist.repository.KDaoFilter
import boonai.archivist.util.FileUtils
import boonai.archivist.util.JdbcUtils
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.math.BigDecimal
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

/**
 * All properties needed to create a Project
 */
@ApiModel("Project Spec", description = "All properties necessary to create a Project.")
class ProjectSpec(

    @ApiModelProperty("A unique name of the project.")
    val name: String,

    @ApiModelProperty("The project tier unlocks extra capabilities.")
    val tier: ProjectTier = ProjectTier.PREMIER,

    @ApiModelProperty("The projected size of a project.")
    val size: ProjectSize = ProjectSize.SMALL,
    /**
     * Allow unittests to specify a project Id. Not allowed
     * for REST clients.
     */
    @ApiModelProperty("An optional unique ID for the project.")
    val id: UUID? = null
)

@ApiModel("Project Size", description = "Specifies ")
enum class ProjectSize(val shards: Int, val replicas: Int) {
    XSMALL(1, 0),
    SMALL(2, 1),
    MEDIUM(3, 1),
    LARGE(5, 1),
    XLARGE(7, 1)
}

@ApiModel("Project Tier", description = "Specifies in which tier is the Project in.")
enum class ProjectTier {
    @ApiModelProperty("Allows the use of Essentials Modules")
    ESSENTIALS,

    @ApiModelProperty("Allows the use of Premier Modules")
    PREMIER
}

@ApiModel("Project Tier Update", description = "Set new Tier State")
class ProjectTierUpdate(
    @ApiModelProperty("Project Tier value to be updated")
    val tier: ProjectTier
)

@ApiModel("Project Rename", description = "Update Project Name")
class ProjectNameUpdate(
    @ApiModelProperty("New Project Name")
    val name: String
)

/**
 * Projects represent unique groups of resources provided by ZMLP.
 */
@Entity
@Table(name = "project")
@ApiModel("Project", description = "A ZMLP Project")
class Project(
    @Id
    @Column(name = "pk_project")
    @ApiModelProperty("The Unique ID of the project.")
    val id: UUID,

    @Column(name = "str_name")
    @ApiModelProperty("The name of the Project")
    val name: String,

    @Column(name = "time_created")
    @ApiModelProperty("The time the Project was created..")
    val timeCreated: Long,

    @Column(name = "time_modified")
    @ApiModelProperty("The last time the Project was modified.")
    val timeModified: Long,

    @Column(name = "actor_created")
    @ApiModelProperty("The actor which created this Project")
    val actorCreated: String,

    @Column(name = "actor_modified")
    @ApiModelProperty("The actor that last made the last modification the project.")
    val actorModified: String,

    @Column(name = "enabled")
    @ApiModelProperty("Set if the project is enabled")
    val enabled: Boolean,

    @Column(name = "int_tier")
    @ApiModelProperty("Project Tier")
    val tier: ProjectTier,

    @Column(name = "pk_pipeline_default", nullable = true)
    @ApiModelProperty("The default Pipeline for this project")
    val defaultPipelineId: UUID? = null,

    @Column(name = "pk_index_route", nullable = true)
    @ApiModelProperty("The default Index for this project")
    val indexRouteId: UUID? = null

) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Project

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}

@ApiModel("Project Filter", description = "Search filter for finding Projects")
class ProjectFilter(

    /**
     * A list of unique Project IDs.
     */
    @ApiModelProperty("The Project IDs to match.")
    val ids: List<UUID>? = null,

    /**
     * A list of unique Project names.
     */
    @ApiModelProperty("The project names to match")
    val names: List<String>? = null

) : KDaoFilter() {
    @JsonIgnore
    override val sortMap: Map<String, String> = mapOf(
        "name" to "str_name",
        "timeCreated" to "time_created",
        "timeModified" to "time_modified",
        "id" to "pk_project"
    )

    @JsonIgnore
    override fun build() {

        if (sort.isNullOrEmpty()) {
            sort = listOf("name:asc")
        }

        ids?.let {
            addToWhere(JdbcUtils.inClause("project.pk_project", it.size))
            addToValues(it)
        }

        names?.let {
            addToWhere(JdbcUtils.inClause("project.str_name", it.size))
            addToValues(it)
        }
    }
}

@ApiModel("Project Quotas", description = "Project ingest limits and counters")
class ProjectQuotas(

    @ApiModelProperty("The maximum amount of video that can be ingested, in seconds. ")
    val videoSecondsMax: Long,

    @ApiModelProperty("The current amount of video ingested, in seconds. ")
    val videoSecondsCount: BigDecimal,

    @ApiModelProperty("The current number of deleted videos, in seconds. ")
    val deletedVideoSecondsCount: BigDecimal,

    @ApiModelProperty("The maximum number of pages ingested.")
    val pageMax: Long,

    @ApiModelProperty("The current number of pages ingested.")
    val pageCount: Long,

    @ApiModelProperty("The current number of deleted pages.")
    val deletedPageCount: Long
)

@ApiModel("ProjectQuotasTimeSeriesEntry", description = "Quota gauges rolled up to the hour.")
class ProjectQuotasTimeSeriesEntry(
    @ApiModelProperty("The date of this interval.")
    val timestamp: Long,
    @ApiModelProperty("The amount of video ingested during this interval.")
    val videoSecondsCount: BigDecimal,
    @ApiModelProperty("Th number of pages ingested during this interval.")
    val pageCount: Long,
    @ApiModelProperty("The number of unique video files ingested during this interval.")
    val videoFileCount: Long,
    @ApiModelProperty("The number of unique document files ingested during this interval.")
    val documentFileCount: Long,
    @ApiModelProperty("The number of unique image files ingested during this interval.")
    val imageFileCount: Long,
    @ApiModelProperty("The number of unique video clips ingested during this interval.")
    val videoClipCount: Long,
    @ApiModelProperty("The amount of deleted video during this interval.")
    val deletedVideoSecondsCount: BigDecimal,
    @ApiModelProperty("The number of deleted video files during this interval.")
    val deletedVideoFileCount: Long,
    @ApiModelProperty("The number of deleted document files during this interval.")
    val deletedDocumentFileCount: Long,
    @ApiModelProperty("The number of deleted image files during this interval.")
    val deletedImageFileCount: Long,
    @ApiModelProperty("The number of deleted video clips during this interval.")
    val deletedVideoClipCount: Long,
    @ApiModelProperty("Th number of deleted Pages during this interval.")
    val deletedPageCount: Long
)

/**
 * ProjectQuotaCounters increments a set of counters.  This class is used
 * when batch processing.
 */
class ProjectQuotaCounters {

    var videoLength: Double = 0.0
    var pageCount: Int = 0
    var videoClipCount: Int = 0

    var videoFileCount: Int = 0
    var imageFileCount: Int = 0
    var documentFileCount: Int = 0

    var deletedVideoLength: Double = 0.0
    var deletedVideoFileCount: Int = 0
    var deletedDocumentFileCount: Int = 0
    var deletedImageFileCount: Int = 0
    var deletedVideoClipCount: Int = 0
    var deletedPageCount: Int = 0

    /**
     * Introspect the asset and increment the internal counters.
     */
    fun count(asset: Asset) {

        when (asset.getAttr<String>("media.type")) {
            "video" -> {
                val length = asset.getAttr("media.length", Double::class.java)
                    ?: throw IllegalArgumentException("Video has no length property")
                videoLength += length
                videoFileCount += 1
                videoClipCount += 1
            }
            "document" -> {
                pageCount += 1
                documentFileCount += 1
            }
            "image" -> {
                pageCount += 1
                imageFileCount += 1
            }
            else -> {
                throw IllegalArgumentException("The asset has no media.type property.")
            }
        }
    }

    /**
     * Introspect the asset and increment the internal counters for deletion.
     */
    fun countForDeletion(asset: Asset) {
        val mediaType = asset.getAttr<String>("media.type") ?: FileExtResolver.getType(
            FileUtils.extension(asset.getAttr("source.path", String::class.java))
        )

        when (mediaType) {
            "video" -> {
                val length = asset.getAttr("media.length", Double::class.java)
                    ?: throw IllegalArgumentException("Video has no length property")
                deletedVideoLength += length
                deletedVideoFileCount += 1
                deletedVideoClipCount += 1
            }
            "document" -> {
                deletedPageCount += 1
                deletedDocumentFileCount += 1
            }
            "image" -> {
                deletedPageCount += 1
                deletedImageFileCount += 1
            }
            else -> {
                throw IllegalArgumentException("The asset has no media.type property.")
            }
        }
    }
}
