package boonai.archivist.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@ApiModel("TrainingSetQuery", description = "A simple query for a training set.")
class TrainingSetQuery(
    @ApiModelProperty("The Dataset Id")
    val datasetId: UUID,
    @ApiModelProperty("LabelScopes to filer by.")
    val scopes: List<LabelScope>?,
    @ApiModelProperty("Labels to filter by.")
    val labels: List<String>?
)

@ApiModel("Update Asset By Query Request", description = "Request structure to update an Asset.")
class UpdateAssetsByQueryRequest(

    @ApiModelProperty("A query to select assets")
    val query: Map<String, Any>? = null,

    @ApiModelProperty("A script to run on the doc")
    val script: Map<String, Any>? = null
)

@ApiModel("Update Asset Request", description = "Request structure to update an Asset.")
class UpdateAssetRequest(

    @ApiModelProperty("Key/value pairs to be updated.")
    val doc: Map<String, Any>? = null,

    @ApiModelProperty("A script to run on the doc")
    val script: Map<String, Any>? = null
)

@ApiModel(
    "Batch Upload Assets Request",
    description = "Defines the properties required to batch upload a list of assets."
)
class BatchUploadAssetsRequest(

    @ApiModelProperty("A list of AssetSpec objects which define the Assets starting metadata.")
    var assets: List<AssetSpec>,

    @ApiModelProperty(
        "Set to true if the assets should undergo " +
            "further analysis, or false to stay in the provisioned state."
    )
    val analyze: Boolean = true,

    @ApiModelProperty("The pipeline modules to execute if any, otherwise utilize the default Pipeline.")
    val modules: List<String>? = null,

    @ApiModelProperty("A list of available credentials for the analysis job.")
    val credentials: Set<String>? = null

) {

    lateinit var files: Array<MultipartFile>
}

@ApiModel(
    "Batch Create Assets Request",
    description = "Defines the properties necessary to provision a batch of assets."
)
class BatchCreateAssetsRequest(

    @ApiModelProperty("The list of assets to be created")
    val assets: List<AssetSpec>,

    @ApiModelProperty(
        "Set to true if the assets should undergo " +
            "further analysis, or false to stay in the provisioned state."
    )
    val analyze: Boolean = true,

    @ApiModelProperty("The pipeline modules to execute if any, otherwise utilize the default Pipeline.")
    val modules: List<String>? = null,

    @ApiModelProperty("A list of available credentials for the analysis job.")
    val credentials: Set<String>? = null,

    @JsonIgnore
    @ApiModelProperty("The taskId that is creating the assets via expand.", hidden = true)
    val task: InternalTask? = null,

    @JsonIgnore
    @ApiModelProperty("The initial state of the asset", hidden = true)
    val state: AssetState = AssetState.Pending

)

@ApiModel(
    "Batch Create Assets Response",
    description = "The response returned after provisioning assets."
)
class BatchCreateAssetsResponse(

    @ApiModelProperty("A map of failed asset ids to error message")
    val failed: List<BatchIndexFailure>,

    @ApiModelProperty("A list of asset Ids created.")
    val created: List<String>,

    @ApiModelProperty("The assets that already existed.")
    val exists: Collection<String>,

    @ApiModelProperty("The ID of the analysis job, if analysis was selected")
    var jobId: UUID? = null
) {

    @ApiModelProperty("The total number of assets to be updated.")
    val totalUpdated = created.size + exists.size
}

@ApiModel(
    "Batch Process Asset Search Request",
    description = "Batch reprocess and asset search."
)
class ReprocessAssetSearchRequest(

    @ApiModelProperty("An ElasticSearch query to process.  All assets will be processed that match the search.")
    val search: Map<String, Any>,

    @ApiModelProperty("The modules to apply.")
    val modules: List<String>,

    @ApiModelProperty("The number of assets to run per batch.")
    val batchSize: Int = 128,

    @ApiModelProperty("A name for the job")
    val name: String? = null,

    @ApiModelProperty("Set to true to kill a job with the same name")
    val replace: Boolean = false,

    @ApiModelProperty("The number of assets to run per batch.")
    val dependOnJobIds: List<UUID>? = null,

    @ApiModelProperty("Filter by file types, defaults to all types.")
    var fileTypes: List<FileType> = FileType.allTypes(),

    @ApiModelProperty("Include standard modules in pipeline.")
    val includeStandard: Boolean = true,

    @ApiModelProperty("Settings for the task.")
    val settings: Map<String, Any>? = null

)

@ApiModel(
    "Batch Process Asset Search Response",
    description = "The reponse to a ReprocessAssetSearchRequest"
)
class ReprocessAssetSearchResponse(

    @ApiModelProperty("The job running the reprocess")
    val job: Job,

    @ApiModelProperty("The number of assets expected to be reprocessed.")
    val assetCount: Long
)

@ApiModel(
    "UpdateAssetLabelsRequest",
    description = "Request to add /remove labels"
)
class UpdateAssetLabelsRequest(

    @ApiModelProperty("The labels to add.  Supplying a new label for an existing Model overwrites it.")
    val add: Map<String, List<Label>>? = null,

    @ApiModelProperty("The labels to remove.")
    val remove: Map<String, List<Label>>? = null
)

@ApiModel(
    "BatchDeleteAssetsRequest",
    description = "Response for batch deleting of assets."
)
class BatchDeleteAssetsRequest(

    @ApiModelProperty("The assets that were removed")
    val assetIds: Set<String>
)

@ApiModel(
    "BatchDeleteAssetsResponse",
    description = "Response for batch deleting of assets."
)
class BatchDeleteAssetResponse(

    @ApiModelProperty("The assets that were removed")
    val deleted: List<String>,

    @ApiModelProperty("The assets that failed to be removed.")
    val failed: List<String>
)

@ApiModel(
    "Batch Index Failure",
    description = "Describes a failure to add or update an asset to the ES index."
)
class BatchIndexFailure(

    @ApiModelProperty("The asset Id that failed.")
    val assetId: String,
    @ApiModelProperty("The URI of the asset.")
    val uri: String?,
    @ApiModelProperty("The error message")
    val message: String
)

@ApiModel(
    "Batch Index Response",
    description = "The response for indexing a batch of assets."
)
class BatchIndexResponse(

    @ApiModelProperty("The IDs that succeeded.")
    val indexed: List<String>,

    @ApiModelProperty("A list of failures")
    val failed: List<BatchIndexFailure>,

    @ApiModelProperty("Transient deletion status")
    val transient: List<String>
)

@ApiModel(
    "Generic Batch Update Response",
    description = "A generic response for various batch asset update operations."
)
class GenericBatchUpdateResponse(

    @ApiModelProperty("The number of assets updated")
    val updated: Long? = null,

    @ApiModelProperty("The number of assets created.")
    val created: Long? = null,

    @ApiModelProperty("The number of assets deleted.")
    val deleted: Long? = null
)

@ApiModel(
    "Batch Update Custom Field Request",
    description = "Update custom fields on the given assets."
)
class BatchUpdateCustomFieldsRequest(

    val update: Map<String, Map<String, Any>>

) {
    fun size(): Int {
        return update.size
    }
}

@ApiModel(
    "Batch Update Custom Field Response",
    description = "Update custom fields on the given assets."
)
class BatchUpdateResponse(

    val failed: Map<String, String>

) {
    val success = failed.isEmpty()
}
