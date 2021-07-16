package boonai.archivist.domain

/**
 * Minimum properties to determine of an Asset needs reprocessing.
 */
class ProcessorMetric(

    /**
     * The processor class name
     */
    val processor: String,

    /**
     * The module the processor was added from.
     */
    val module: String,

    /**
     * The argument checksum
     */
    val checksum: Long,

    /**
     * The type of error, if any
     */
    val error: String?
)

/**
 * Class for storing various Asset metrics.
 */
class AssetMetrics(

    /**
     * Pipeline metrics for an asset.
     */
    val pipeline: List<ProcessorMetric>?
)

/**
 * Sent to metrics service
 */
class AssetMetricsEvent(
    val asset_id: String,
    val asset_path: String?,
    val asset_type: String?,
    val services: Set<String>?,
    val length: Double?
)
