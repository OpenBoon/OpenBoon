package com.zorroa.archivist.domain

/**
 * Standard Docker containers.
 */
object StandardContainers {
    const val CORE = "zmlp/plugins-core"
    const val ANALYSIS = "zmlp/plugins-analysis"
}

/**
 * Return a list of the standard pipeline modules.
 */
fun getStandardModules(): List<PipelineModSpec> {
    return listOf(
        PipelineModSpec(
            "zvi-doc-page-extraction",
            "Extract all pages in MS Office/PDF documents into separate assets.",
            listOf(
                ModOp(
                    ModOpType.SET_ARGS,
                    mapOf("extract_pages" to true),
                    OpFilter(OpFilterType.REGEX, ".*OfficeImporter")
                )
            )
        ),
        PipelineModSpec(
            "zvi-image-layer-extraction",
            "Extract all layers in multi page image formats such as tiff and psd as as " +
                "separate assets",
            listOf(
                ModOp(
                    ModOpType.SET_ARGS,
                    mapOf("extract_pages" to true),
                    OpFilter(OpFilterType.REGEX, ".*ImageImporter")
                )
            )
        ),
        PipelineModSpec(
            "zvi-video-shot-extraction",
            "Break video files into individual assets based on a shot detection algorithm.",
            listOf(
                ModOp(
                    ModOpType.APPEND,
                    listOf(
                        ProcessorRef("zmlp_core.clipify.ShotDetectionVideoClipifier",
                            StandardContainers.CORE)
                    )
                )
            )
        ),
        PipelineModSpec(
            "zvi-object-detection",
            "Detect everyday objects in images, video, and documents.",
            listOf(
                ModOp(
                    ModOpType.APPEND,
                    listOf(
                        ProcessorRef("zmlp_analysis.detect.ZmlpObjectDetectionProcessor",
                            StandardContainers.ANALYSIS)
                    )
                )
            )
        ),
        PipelineModSpec(
            "zvi-label-detection",
            "Generate keyword labels for image, video, and documents.",
            listOf(
                ModOp(
                    ModOpType.APPEND,
                    listOf(
                        ProcessorRef("zmlp_analysis.mxnet.ZviLabelDetectionResNet152",
                            StandardContainers.ANALYSIS)
                    )
                )
            )
        ),
        PipelineModSpec(
            "clarifai-predict",
            "Clarifai prediction API, standard model.",
            listOf(
                ModOp(
                    ModOpType.APPEND,
                    listOf(
                        ProcessorRef("zmlp_analysis.clarifai.ClarifaiPredictProcessor",
                            StandardContainers.ANALYSIS)
                    )
                )
            )
        ),
        PipelineModSpec(
            "gcp-label-detection",
            "Utilize Google Cloud Vision label detection to detect and extract information about " +
                "entities in an image, across a broad group of categories.",
            listOf(
                ModOp(
                    ModOpType.APPEND,
                    listOf(
                        ProcessorRef("zmlp_analysis.google.CloudVisionDetectLabels",
                            StandardContainers.ANALYSIS)
                    )
                )
            )
        ),
        PipelineModSpec(
            "gcp-object-detection",
            "Utilize Google Cloud Vision label detection to detect and extract information about " +
                "entities in an image, across a broad group of categories.",
            listOf(
                ModOp(
                    ModOpType.APPEND,
                    listOf(
                        ProcessorRef("zmlp_analysis.google.CloudVisionDetectLabels",
                            StandardContainers.ANALYSIS)
                    )
                )
            )
        ),
        PipelineModSpec(
            "zvi-disable-analysis",
            "Disable all non-core processors",
            listOf(
                ModOp(
                    ModOpType.REMOVE,
                    null,
                    OpFilter(OpFilterType.NOT_REGEX, "zmlp_core.*")
                )
            )
        )
    )
}
