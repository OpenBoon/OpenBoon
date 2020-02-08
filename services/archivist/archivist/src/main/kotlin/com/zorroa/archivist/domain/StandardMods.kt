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
            "zmlp-doc-page-extraction",
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
            "zmlp-image-page-extraction",
            "Extract all pages or layers in multi page image formats such as tiff and psd as as " +
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
            "zmlp-video-shot-detection",
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
            "zmlp-objects",
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
            "zmlp-labels",
            "Generate keyword labels for image, video, and documents.",
            listOf(
                ModOp(
                    ModOpType.APPEND,
                    listOf(
                        ProcessorRef("zmlp_analysis.mxnet.processors.ResNetClassifyProcessor",
                            StandardContainers.ANALYSIS)
                    )
                )
            )
        ),
        PipelineModSpec(
            "zmlp-faces",
            "Detect face bounding boxes",
            listOf(
                ModOp(
                    ModOpType.APPEND,
                    listOf(
                        ProcessorRef("zmlp_analysis.face.ZmlpFaceDetectionProcessor",
                            StandardContainers.ANALYSIS)
                    )
                )
            )
        )
    )
}
