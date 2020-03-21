package com.zorroa.archivist.domain

/**
 * Standard Docker containers.
 */
object StandardContainers {
    const val CORE = "zmlp/plugins-core"
    const val ANALYSIS = "zmlp/plugins-analysis"
}

object ModStandards {
    const val CLARIFAI = "Clarifai"
    const val ZORROA = "Zorroa"
    const val ZORROA_VINT = "Visual Intelligence"
    const val GOOGLE = "Google"
    const val GOOGLE_VISION = "Google Vision"
}

/**
 * Return a list of the standard pipeline modules.
 */
fun getStandardModules(): List<PipelineModSpec> {
    return listOf(
        PipelineModSpec(
            "zvi-document-page-extraction",
            "Extract all pages in MS Office/PDF documents into separate assets.",
            ModStandards.ZORROA,
            ModStandards.ZORROA_VINT,
            listOf(SupportedMedia.Documents),
            listOf(
                ModOp(
                    ModOpType.SET_ARGS,
                    mapOf("extract_pages" to true),
                    OpFilter(OpFilterType.REGEX, ".*OfficeImporter")
                )
            )
        ),
        PipelineModSpec(
            "zvi-image-page-extraction",
            "Extract all layers in multi page image formats such as tiff and psd as as " +
                "separate assets",
            ModStandards.ZORROA,
            ModStandards.ZORROA_VINT,
            listOf(SupportedMedia.Images),
            listOf(
                ModOp(
                    ModOpType.SET_ARGS,
                    mapOf("extract_pages" to true),
                    OpFilter(OpFilterType.REGEX, ".*ImageImporter")
                )
            )
        ),
        PipelineModSpec(
            "zvi-video-shot-timeline",
            "Break video files into individual assets based on a shot detection algorithm.",
            ModStandards.ZORROA,
            ModStandards.ZORROA_VINT,
            listOf(SupportedMedia.Video),
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
            ModStandards.ZORROA,
            ModStandards.ZORROA_VINT,
            listOf(SupportedMedia.Images, SupportedMedia.Documents, SupportedMedia.Video),
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
            ModStandards.ZORROA,
            ModStandards.ZORROA_VINT,
            listOf(SupportedMedia.Images, SupportedMedia.Documents, SupportedMedia.Video),
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
            "clarifai-predict-general",
            "Clarifai prediction API with general model.",
            ModStandards.CLARIFAI,
            "Clarifai General Model",
            listOf(SupportedMedia.Images, SupportedMedia.Documents, SupportedMedia.Video),
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
            ModStandards.GOOGLE,
            ModStandards.GOOGLE_VISION,
            listOf(SupportedMedia.Images, SupportedMedia.Documents, SupportedMedia.Video),
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
            ModStandards.GOOGLE,
            ModStandards.GOOGLE_VISION,
            listOf(SupportedMedia.Images, SupportedMedia.Documents, SupportedMedia.Video),
            listOf(
                ModOp(
                    ModOpType.APPEND,
                    listOf(
                        ProcessorRef("zmlp_analysis.google.CloudVisionDetectLabels",
                            StandardContainers.ANALYSIS)
                    )
                )
            )
        )
    )
}
