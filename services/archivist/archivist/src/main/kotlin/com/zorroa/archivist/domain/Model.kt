package com.zorroa.archivist.domain

/**
 * Type of models that can be trained.
 */
enum class ModelType(
    val processor: String,
    val args: Map<String, Any>,
    val moduleName: String
) {
    FAST_CLASSIFICATON("zmlp_train.kmeans.KMeansTrainer", mapOf(), "custom-{}-fast-classification"),
    TF2_XFER_RESNET152(
        "zmlp_train.tf2.TensorflowTransferLearningTrainer",
        mapOf(
            "model_type" to "resnet_v2",
            "min_concepts" to 2,
            "min_examples" to 5,
            "train-test-ratio" to 3
        ),
        "custom-{}-label-detection-resnet152"
    ),
    TF2_XFER_VGG16(
        "zmlp_train.tf2.TensorflowTransferLearningTrainer",
        mapOf(
            "model_type" to "vgg16",
            "min_concepts" to 2,
            "min_examples" to 5,
            "train-test-ratio" to 3
        ),
        "custom-{}-label-detection-vgg16"
    ),
    TF2_XFER_MOBILENET2(
        "zmlp_train.tf2.TensorflowTransferLearningTrainer",
        mapOf(
            "model_type" to "mobilenet_v2",
            "min_concepts" to 2,
            "min_examples" to 5,
            "train-test-ratio" to 3
        ),
        "custom-{}-label-detection-mobilenet2"
    )
}

class ModelSpec(
    val type: ModelType
)
