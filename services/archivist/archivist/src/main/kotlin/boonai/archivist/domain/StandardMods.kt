package boonai.archivist.domain

/**
 * Standard Docker containers.
 */
object StandardContainers {
    const val CORE = "boonai/plugins-core"
    const val ANALYSIS = "boonai/plugins-analysis"
    const val TRAIN = "boonai/plugins-train"
}

object Category {
    const val GOOGLE_VIDEO = "Google Video Intelligence"
    const val GOOGLE_VISION = "Google Vision"
    const val GOOGLE_S2TEXT = "Google Speech-To-Text"
    const val GOOGLE_DLP = "Google Data Loss Prevention"
    const val AWS_REK = "Amazon Rekognition"
    const val AWS_TRANS = "Amazon Transcribe"
    const val AZURE_VISION = "Azure Computer Vision"
    const val BOONAI_TL = "Boon AI Timeline Extraction"
    const val BOONAI_STD = "Boon AI Visual Intelligence"
    const val CLARIFAI_STD = "Clarifai Public"
    const val TRAINED = "Custom Models"
}

object Provider {
    const val CLARIFAI = "Clarifai"
    const val BOONAI = "Boon AI"
    const val GOOGLE = "Google"
    const val AMAZON = "Amazon"
    const val CUSTOM = "Custom"
    const val MICROSOFT = "Microsoft"
}

object ModelObjective {
    const val LABEL_DETECTION = "Label Detection"
    const val OBJECT_DETECTION = "Object Detection"
    const val LANDMARK_DETECTION = "Landmark Detection"
    const val LOGO_DETECTION = "Logo Detection"
    const val FACE_RECOGNITION = "Face Recognition"
    const val FACE_DETECTION = "Face Detection"
    const val CLIPIFIER = "Video Clip Generator"
    const val EXPLICIT_DETECTION = "Explicit Detection"
    const val TEXT_DETECTION = "Text Detection (OCR)"
    const val IMAGE_TEXT_DETECTION = "Image Text Detection"
    const val SPEECH_RECOGNITION = "Speech Recognition"
    const val IMAGE_DESCRIPTION = "Image Description"
    const val IMAGE_SEGMENTATION = "Image Segmentation"
}

/**
 * Return a list of the standard pipeline modules.
 */
fun getStandardModules(): List<PipelineModSpec> {
    return listOf(
        PipelineModSpec(
            "boonai-extract-layers",
            "Extract all layers in multilayer or multi-page image formats such as tiff and psd as as " +
                "separate assets",
            Provider.BOONAI,
            Category.BOONAI_TL,
            ModelObjective.CLIPIFIER,
            listOf(FileType.Images),
            listOf(
                ModOp(
                    ModOpType.SET_ARGS,
                    mapOf("extract_image_pages" to true),
                    OpFilter(OpFilterType.REGEX, ".*FileImportProcessor")
                )
            ),
            true
        ),
        PipelineModSpec(
            "boonai-object-detection",
            "Detect everyday objects in images and documents.",
            Provider.BOONAI,
            Category.BOONAI_STD,
            ModelObjective.OBJECT_DETECTION,
            listOf(FileType.Images),
            listOf(
                ModOp(
                    ModOpType.APPEND,
                    listOf(
                        ProcessorRef(
                            "boonai_analysis.boonai.ZviObjectDetectionProcessor",
                            StandardContainers.ANALYSIS
                        )
                    )
                )
            ),
            true
        ),
        PipelineModSpec(
            "boonai-face-detection",
            "Detect faces in images and documents.",
            Provider.BOONAI,
            Category.BOONAI_STD,
            ModelObjective.FACE_RECOGNITION,
            listOf(FileType.Images),
            listOf(
                ModOp(
                    ModOpType.APPEND,
                    listOf(
                        ProcessorRef(
                            "boonai_analysis.boonai.ZviFaceDetectionProcessor",
                            StandardContainers.ANALYSIS
                        )
                    )
                )
            ),
            true
        ),
        PipelineModSpec(
            "boonai-label-detection",
            "Generate keyword labels for images and documents.",
            Provider.BOONAI,
            Category.BOONAI_STD,
            ModelObjective.LABEL_DETECTION,
            listOf(FileType.Images),
            listOf(
                ModOp(
                    ModOpType.APPEND,
                    listOf(
                        ProcessorRef(
                            "boonai_analysis.boonai.ZviLabelDetectionProcessor",
                            StandardContainers.ANALYSIS
                        )
                    )
                )
            ),
            true
        ),
        PipelineModSpec(
            "boonai-text-detection",
            "Utilize OCR technology to detect text in documents.",
            Provider.BOONAI,
            Category.BOONAI_STD,
            ModelObjective.TEXT_DETECTION,
            listOf(FileType.Images),
            listOf(
                ModOp(
                    ModOpType.APPEND,
                    listOf(
                        ProcessorRef(
                            "boonai_analysis.boonai.ZviOcrProcessor",
                            StandardContainers.ANALYSIS
                        )
                    )
                ),
                ModOp(
                    ModOpType.SET_ARGS,
                    mapOf("ocr" to true),
                    OpFilter(OpFilterType.REGEX, ".*FileImportProcessor")
                )
            ),
            true
        ),
        PipelineModSpec(
            "clarifai-label-detection",
            "Recognize over 11,000 concepts including objects, themes, moods and more.",
            Provider.CLARIFAI,
            Category.CLARIFAI_STD,
            ModelObjective.LABEL_DETECTION,
            listOf(FileType.Images, FileType.Videos),
            listOf(
                ModOp(
                    ModOpType.APPEND,
                    listOf(
                        ProcessorRef(
                            "boonai_analysis.clarifai.ClarifaiLabelDetectionProcessor",
                            StandardContainers.ANALYSIS
                        ),
                        ProcessorRef(
                            "boonai_analysis.clarifai.ClarifaiVideoLabelDetectionProcessor",
                            StandardContainers.ANALYSIS
                        )
                    )
                )
            ),
            true
        ),
        PipelineModSpec(
            "clarifai-food-detection",
            "Recognize more than 1,000 food items and dishes in images down to the ingredient level.",
            Provider.CLARIFAI,
            Category.CLARIFAI_STD,
            ModelObjective.LABEL_DETECTION,
            listOf(FileType.Images, FileType.Videos),
            listOf(
                ModOp(
                    ModOpType.APPEND,
                    listOf(
                        ProcessorRef(
                            "boonai_analysis.clarifai.ClarifaiFoodDetectionProcessor",
                            StandardContainers.ANALYSIS
                        ),
                        ProcessorRef(
                            "boonai_analysis.clarifai.ClarifaiVideoFoodDetectionProcessor",
                            StandardContainers.ANALYSIS
                        )
                    )
                )
            ),
            true
        ),
        PipelineModSpec(
            "clarifai-apparel-detection",
            "Detect items of clothing or fashion-related items. ",
            Provider.CLARIFAI,
            Category.CLARIFAI_STD,
            ModelObjective.LABEL_DETECTION,
            listOf(FileType.Images, FileType.Videos),
            listOf(
                ModOp(
                    ModOpType.APPEND,
                    listOf(
                        ProcessorRef(
                            "boonai_analysis.clarifai.ClarifaiApparelDetectionProcessor",
                            StandardContainers.ANALYSIS
                        ),
                        ProcessorRef(
                            "boonai_analysis.clarifai.ClarifaiVideoApparelDetectionProcessor",
                            StandardContainers.ANALYSIS
                        )
                    )
                )
            ),
            true
        ),
        PipelineModSpec(
            "clarifai-travel-detection",
            "Recognize specific features of residential, hotel, and travel-related properties.",
            Provider.CLARIFAI,
            Category.CLARIFAI_STD,
            ModelObjective.LABEL_DETECTION,
            listOf(FileType.Images, FileType.Videos),
            listOf(
                ModOp(
                    ModOpType.APPEND,
                    listOf(
                        ProcessorRef(
                            "boonai_analysis.clarifai.ClarifaiTravelDetectionProcessor",
                            StandardContainers.ANALYSIS
                        ),
                        ProcessorRef(
                            "boonai_analysis.clarifai.ClarifaiVideoTravelDetectionProcessor",
                            StandardContainers.ANALYSIS
                        )
                    )
                )
            ),
            true
        ),
        PipelineModSpec(
            "clarifai-wedding-detection",
            "Recognize over 400 concepts related to weddings including bride, groom, flowers and more.",
            Provider.CLARIFAI,
            Category.CLARIFAI_STD,
            ModelObjective.LABEL_DETECTION,
            listOf(FileType.Images, FileType.Videos),
            listOf(
                ModOp(
                    ModOpType.APPEND,
                    listOf(
                        ProcessorRef(
                            "boonai_analysis.clarifai.ClarifaiWeddingDetectionProcessor",
                            StandardContainers.ANALYSIS
                        ),
                        ProcessorRef(
                            "boonai_analysis.clarifai.ClarifaiVideoWeddingDetectionProcessor",
                            StandardContainers.ANALYSIS
                        )
                    )
                )
            ),
            true
        ),
        PipelineModSpec(
            "clarifai-nsfw-detection",
            "Identify different levels of nudity in visual content and automatically moderate or filter offensive content.",
            Provider.CLARIFAI,
            Category.CLARIFAI_STD,
            ModelObjective.EXPLICIT_DETECTION,
            listOf(FileType.Images, FileType.Videos),
            listOf(
                ModOp(
                    ModOpType.APPEND,
                    listOf(
                        ProcessorRef(
                            "boonai_analysis.clarifai.ClarifaiExplicitDetectionProcessor",
                            StandardContainers.ANALYSIS
                        ),
                        ProcessorRef(
                            "boonai_analysis.clarifai.ClarifaiVideoExplicitDetectionProcessor",
                            StandardContainers.ANALYSIS
                        )
                    )
                )
            ),
            true
        ),
        PipelineModSpec(
            "clarifai-unsafe-detection",
            "Detect if an image contains concepts such as gore, drugs, explicit nudity or suggestive nudity.",
            Provider.CLARIFAI,
            Category.CLARIFAI_STD,
            ModelObjective.EXPLICIT_DETECTION,
            listOf(FileType.Images, FileType.Videos),
            listOf(
                ModOp(
                    ModOpType.APPEND,
                    listOf(
                        ProcessorRef(
                            "boonai_analysis.clarifai.ClarifaiModerationDetectionProcessor",
                            StandardContainers.ANALYSIS
                        ),
                        ProcessorRef(
                            "boonai_analysis.clarifai.ClarifaiVideoModerationDetectionProcessor",
                            StandardContainers.ANALYSIS
                        )
                    )
                )
            ),
            true
        ),
        PipelineModSpec(
            "clarifai-weapon-detection",
            "Identify and classify weapons in images and videos.",
            Provider.CLARIFAI,
            Category.CLARIFAI_STD,
            ModelObjective.EXPLICIT_DETECTION,
            listOf(FileType.Images, FileType.Videos),
            listOf(
                ModOp(
                    ModOpType.APPEND,
                    listOf(
                        ProcessorRef(
                            "boonai_analysis.clarifai.ClarifaiWeaponDetectionProcessor",
                            StandardContainers.ANALYSIS
                        ),
                        ProcessorRef(
                            "boonai_analysis.clarifai.ClarifaiVideoWeaponDetectionProcessor",
                            StandardContainers.ANALYSIS
                        )
                    )
                )
            ),
            true
        ),
        PipelineModSpec(
            "clarifai-logo-detection",
            "Identify up to 500 company brands and logos.",
            Provider.CLARIFAI,
            Category.CLARIFAI_STD,
            ModelObjective.LOGO_DETECTION,
            listOf(FileType.Images, FileType.Videos),
            listOf(
                ModOp(
                    ModOpType.APPEND,
                    listOf(
                        ProcessorRef(
                            "boonai_analysis.clarifai.ClarifaiLogoDetectionProcessor",
                            StandardContainers.ANALYSIS
                        ),
                        ProcessorRef(
                            "boonai_analysis.clarifai.ClarifaiVideoLogoDetectionProcessor",
                            StandardContainers.ANALYSIS
                        )
                    )
                )
            ),
            true
        ),
        PipelineModSpec(
            "clarifai-face-detection",
            "Detect if an image contains human faces and coordinate locations of where those faces appear with a bounding box.",
            Provider.CLARIFAI,
            Category.CLARIFAI_STD,
            ModelObjective.FACE_DETECTION,
            listOf(FileType.Images, FileType.Videos),
            listOf(
                ModOp(
                    ModOpType.APPEND,
                    listOf(
                        ProcessorRef(
                            "boonai_analysis.clarifai.ClarifaiFaceDetectionProcessor",
                            StandardContainers.ANALYSIS
                        ),
                        ProcessorRef(
                            "boonai_analysis.clarifai.ClarifaiVideoFaceDetectionProcessor",
                            StandardContainers.ANALYSIS
                        )
                    )
                )
            ),
            true
        ),
        PipelineModSpec(
            "clarifai-celebrity-detection",
            "Detect whether images contain the face(s) of celebrities.",
            Provider.CLARIFAI,
            Category.CLARIFAI_STD,
            ModelObjective.FACE_DETECTION,
            listOf(FileType.Images, FileType.Videos),
            listOf(
                ModOp(
                    ModOpType.APPEND,
                    listOf(
                        ProcessorRef(
                            "boonai_analysis.clarifai.ClarifaiCelebrityDetectionProcessor",
                            StandardContainers.ANALYSIS
                        ),
                        ProcessorRef(
                            "boonai_analysis.clarifai.ClarifaiVideoCelebrityDetectionProcessor",
                            StandardContainers.ANALYSIS
                        )
                    )
                )
            ),
            true
        ),
        PipelineModSpec(
            "clarifai-texture-detection",
            "Identify textures and patterns within an image including glacial, ice, metallic, veined, feathers and more.",
            Provider.CLARIFAI,
            Category.CLARIFAI_STD,
            ModelObjective.LABEL_DETECTION,
            listOf(FileType.Images, FileType.Videos),
            listOf(
                ModOp(
                    ModOpType.APPEND,
                    listOf(
                        ProcessorRef(
                            "boonai_analysis.clarifai.ClarifaiTexturesDetectionProcessor",
                            StandardContainers.ANALYSIS
                        ),
                        ProcessorRef(
                            "boonai_analysis.clarifai.ClarifaiVideoTexturesDetectionProcessor",
                            StandardContainers.ANALYSIS
                        )
                    )
                )
            ),
            true
        ),
        PipelineModSpec(
            "clarifai-room-types-detection",
            "Classify images based on the environment in which they were taken. Recognize common scenes in rooms and around homes.",
            Provider.CLARIFAI,
            Category.CLARIFAI_STD,
            ModelObjective.LABEL_DETECTION,
            listOf(FileType.Images, FileType.Videos),
            listOf(
                ModOp(
                    ModOpType.APPEND,
                    listOf(
                        ProcessorRef(
                            "boonai_analysis.clarifai.ClarifaiRoomTypesDetectionProcessor",
                            StandardContainers.ANALYSIS
                        ),
                        ProcessorRef(
                            "boonai_analysis.clarifai.ClarifaiVideoRoomTypesDetectionProcessor",
                            StandardContainers.ANALYSIS
                        )
                    )
                )
            ),
            true
        ),
        PipelineModSpec(
            "clarifai-demographics-detection",
            "Predict age, gender, and multicultural appearance for each detected face based on facial characteristics.",
            Provider.CLARIFAI,
            Category.CLARIFAI_STD,
            ModelObjective.LABEL_DETECTION,
            listOf(FileType.Images, FileType.Videos),
            listOf(
                ModOp(
                    ModOpType.APPEND,
                    listOf(
                        ProcessorRef(
                            "boonai_analysis.clarifai.ClarifaiGenderDetectionProcessor",
                            StandardContainers.ANALYSIS
                        ),
                        ProcessorRef(
                            "boonai_analysis.clarifai.ClarifaiEthnicityDetectionProcessor",
                            StandardContainers.ANALYSIS
                        ),
                        ProcessorRef(
                            "boonai_analysis.clarifai.ClarifaiAgeDetectionProcessor",
                            StandardContainers.ANALYSIS
                        ),
                        ProcessorRef(
                            "boonai_analysis.clarifai.ClarifaiVideoGenderDetectionProcessor",
                            StandardContainers.ANALYSIS
                        ),
                        ProcessorRef(
                            "boonai_analysis.clarifai.ClarifaiVideoEthnicityDetectionProcessor",
                            StandardContainers.ANALYSIS
                        ),
                        ProcessorRef(
                            "boonai_analysis.clarifai.ClarifaiVideoAgeDetectionProcessor",
                            StandardContainers.ANALYSIS
                        ),
                    )
                )
            ),
            true
        ),
        PipelineModSpec(
            "gcp-label-detection",
            "Detect and extract information about entities in " +
                "an image, across a broad group of categories.",
            Provider.GOOGLE,
            Category.GOOGLE_VISION,
            ModelObjective.LABEL_DETECTION,
            listOf(FileType.Images),
            listOf(
                ModOp(
                    ModOpType.APPEND,
                    listOf(
                        ProcessorRef(
                            "boonai_analysis.google.CloudVisionDetectLabels",
                            StandardContainers.ANALYSIS
                        )
                    )
                )
            ),
            true
        ),
        PipelineModSpec(
            "gcp-object-detection",
            "Detect and extract multiple objects in an image.",
            Provider.GOOGLE,
            Category.GOOGLE_VISION,
            ModelObjective.OBJECT_DETECTION,
            listOf(FileType.Images),
            listOf(
                ModOp(
                    ModOpType.APPEND,
                    listOf(
                        ProcessorRef(
                            "boonai_analysis.google.CloudVisionDetectObjects",
                            StandardContainers.ANALYSIS
                        )
                    )
                )
            ),
            true
        ),
        PipelineModSpec(
            "gcp-logo-detection",
            "Detect popular product logos within an image.",
            Provider.GOOGLE,
            Category.GOOGLE_VISION,
            ModelObjective.LOGO_DETECTION,
            listOf(FileType.Images),
            listOf(
                ModOp(
                    ModOpType.APPEND,
                    listOf(
                        ProcessorRef(
                            "boonai_analysis.google.CloudVisionDetectLogos",
                            StandardContainers.ANALYSIS
                        )
                    )
                )
            ),
            true
        ),
        PipelineModSpec(
            "gcp-image-text-detection",
            "Detect text within a photographic image.",
            Provider.GOOGLE,
            Category.GOOGLE_VISION,
            ModelObjective.IMAGE_TEXT_DETECTION,
            listOf(FileType.Images),
            listOf(
                ModOp(
                    ModOpType.APPEND,
                    listOf(
                        ProcessorRef(
                            "boonai_analysis.google.CloudVisionDetectImageText",
                            StandardContainers.ANALYSIS
                        )
                    )
                ),
                ModOp(
                    ModOpType.SET_ARGS,
                    mapOf("ocr" to true),
                    OpFilter(OpFilterType.REGEX, ".*FileImportProcessor")
                )
            ),
            true
        ),
        PipelineModSpec(
            "gcp-document-text-detection",
            "Utilize OCR technology to detect text in documents.",
            Provider.GOOGLE,
            Category.GOOGLE_VISION,
            ModelObjective.TEXT_DETECTION,
            listOf(FileType.Images),
            listOf(
                ModOp(
                    ModOpType.APPEND,
                    listOf(
                        ProcessorRef(
                            "boonai_analysis.google.CloudVisionDetectDocumentText",
                            StandardContainers.ANALYSIS
                        )
                    )
                ),
                ModOp(
                    ModOpType.SET_ARGS,
                    mapOf("ocr" to true),
                    OpFilter(OpFilterType.REGEX, ".*FileImportProcessor")
                )
            ),
            true
        ),
        PipelineModSpec(
            "gcp-landmark-detection",
            "Detect popular natural and man-made structures within an image.",
            Provider.GOOGLE,
            Category.GOOGLE_VISION,
            ModelObjective.LANDMARK_DETECTION,
            listOf(FileType.Images),
            listOf(
                ModOp(
                    ModOpType.APPEND,
                    listOf(
                        ProcessorRef(
                            "boonai_analysis.google.CloudVisionDetectLandmarks",
                            StandardContainers.ANALYSIS
                        )
                    )
                )
            ),
            true
        ),
        PipelineModSpec(
            "gcp-video-label-detection",
            "Detect labels within a video.",
            Provider.GOOGLE,
            Category.GOOGLE_VIDEO,
            ModelObjective.LABEL_DETECTION,
            listOf(FileType.Videos),
            listOf(
                ModOp(
                    ModOpType.APPEND_MERGE,
                    listOf(
                        ProcessorRef(
                            "boonai_analysis.google.AsyncVideoIntelligenceProcessor",
                            StandardContainers.ANALYSIS,
                            mapOf("detect_labels" to true)
                        )
                    )
                )
            ),
            true
        ),
        PipelineModSpec(
            "gcp-video-logo-detection",
            "Detect logos within a video.",
            Provider.GOOGLE,
            Category.GOOGLE_VIDEO,
            ModelObjective.LOGO_DETECTION,
            listOf(FileType.Videos),
            listOf(
                ModOp(
                    ModOpType.APPEND_MERGE,
                    listOf(
                        ProcessorRef(
                            "boonai_analysis.google.AsyncVideoIntelligenceProcessor",
                            StandardContainers.ANALYSIS,
                            mapOf("detect_logos" to true)
                        )
                    )
                )
            ),
            true
        ),
        PipelineModSpec(
            "gcp-video-object-detection",
            "Detect objects within a video.",
            Provider.GOOGLE,
            Category.GOOGLE_VIDEO,
            ModelObjective.OBJECT_DETECTION,
            listOf(FileType.Videos),
            listOf(
                ModOp(
                    ModOpType.APPEND_MERGE,
                    listOf(
                        ProcessorRef(
                            "boonai_analysis.google.AsyncVideoIntelligenceProcessor",
                            StandardContainers.ANALYSIS,
                            mapOf("detect_objects" to true)
                        )
                    )
                )
            ),
            true
        ),
        PipelineModSpec(
            "gcp-video-explicit-detection",
            "Detect explicit content in videos.",
            Provider.GOOGLE,
            Category.GOOGLE_VIDEO,
            ModelObjective.EXPLICIT_DETECTION,
            listOf(FileType.Videos),
            listOf(
                ModOp(
                    ModOpType.APPEND_MERGE,
                    listOf(
                        ProcessorRef(
                            "boonai_analysis.google.AsyncVideoIntelligenceProcessor",
                            StandardContainers.ANALYSIS,
                            mapOf("detect_explicit" to true)
                        )
                    )
                )
            ),
            true
        ),
        PipelineModSpec(
            "gcp-video-text-detection",
            "Detect and extract text from video using OCR",
            Provider.GOOGLE,
            Category.GOOGLE_VIDEO,
            ModelObjective.TEXT_DETECTION,
            listOf(FileType.Videos),
            listOf(
                ModOp(
                    ModOpType.APPEND_MERGE,
                    listOf(
                        ProcessorRef(
                            "boonai_analysis.google.AsyncVideoIntelligenceProcessor",
                            StandardContainers.ANALYSIS,
                            mutableMapOf("detect_text" to true)
                        )
                    )
                )
            ),
            true
        ),
        PipelineModSpec(
            "gcp-video-speech-transcription",
            "Convert audio to text by applying powerful neural network models.",
            Provider.GOOGLE,
            Category.GOOGLE_VIDEO,
            ModelObjective.SPEECH_RECOGNITION,
            listOf(FileType.Videos),
            listOf(
                ModOp(
                    ModOpType.APPEND_MERGE,
                    listOf(
                        ProcessorRef(
                            "boonai_analysis.google.AsyncVideoIntelligenceProcessor",
                            StandardContainers.ANALYSIS,
                            mutableMapOf("detect_speech" to true)
                        )
                    )
                )
            ),
            true
        ),
        PipelineModSpec(
            "gcp-speech-to-text",
            "Convert audio to text by applying powerful neural network models. Support. for 120 languages.",
            Provider.GOOGLE,
            Category.GOOGLE_S2TEXT,
            ModelObjective.SPEECH_RECOGNITION,
            listOf(FileType.Videos),
            listOf(
                ModOp(
                    ModOpType.APPEND,
                    listOf(
                        ProcessorRef(
                            "boonai_analysis.google.AsyncSpeechToTextProcessor",
                            StandardContainers.ANALYSIS,
                            mutableMapOf()
                        )
                    )
                )
            ),
            true
        ),
        PipelineModSpec(
            "gcp-dlp",
            "Use Data Loss Prevention (DLP) to extract names, dates and addresses from scanned documents.",
            Provider.GOOGLE,
            Category.GOOGLE_DLP,
            ModelObjective.TEXT_DETECTION,
            listOf(FileType.Images),
            listOf(
                ModOp(
                    ModOpType.APPEND,
                    listOf(
                        ProcessorRef(
                            "boonai_analysis.google.CloudDLPDetectEntities",
                            StandardContainers.ANALYSIS,
                            mutableMapOf()
                        )
                    )
                ),
                ModOp(
                    ModOpType.SET_ARGS,
                    mapOf("ocr" to true),
                    OpFilter(OpFilterType.REGEX, ".*FileImportProcessor")
                )
            ),
            true
        ),
        PipelineModSpec(
            "aws-label-detection",
            "Generate keyword labels for images and documents.",
            Provider.AMAZON,
            Category.AWS_REK,
            ModelObjective.LABEL_DETECTION,
            listOf(FileType.Images, FileType.Videos),
            listOf(
                ModOp(
                    ModOpType.APPEND,
                    listOf(
                        ProcessorRef(
                            "boonai_analysis.aws.RekognitionLabelDetection",
                            StandardContainers.ANALYSIS
                        ),
                        ProcessorRef(
                            "boonai_analysis.aws.video.RekognitionLabelDetection",
                            StandardContainers.ANALYSIS
                        )
                    )
                )
            ),
            true
        ),
        PipelineModSpec(
            "aws-face-detection",
            "Detect faces using Amazon AWS Rekognition",
            Provider.AMAZON,
            Category.AWS_REK,
            ModelObjective.FACE_DETECTION,
            listOf(FileType.Images),
            listOf(
                ModOp(
                    ModOpType.APPEND,
                    listOf(
                        ProcessorRef(
                            "boonai_analysis.aws.RekognitionFaceDetection",
                            StandardContainers.ANALYSIS
                        )
                    )
                )
            ),
            true
        ),
        PipelineModSpec(
            "aws-unsafe-detection",
            "Detect unsafe content using Amazon AWS Rekognition",
            Provider.AMAZON,
            Category.AWS_REK,
            ModelObjective.EXPLICIT_DETECTION,
            listOf(FileType.Images, FileType.Videos),
            listOf(
                ModOp(
                    ModOpType.APPEND,
                    listOf(
                        ProcessorRef(
                            "boonai_analysis.aws.RekognitionUnsafeDetection",
                            StandardContainers.ANALYSIS
                        ),
                        ProcessorRef(
                            "boonai_analysis.aws.video.RekognitionUnsafeDetection",
                            StandardContainers.ANALYSIS
                        )
                    )
                )
            ),
            true
        ),
        PipelineModSpec(
            "aws-text-detection",
            "Detect text within an image using Amazon AWS Rekognition",
            Provider.AMAZON,
            Category.AWS_REK,
            ModelObjective.IMAGE_TEXT_DETECTION,
            listOf(FileType.Images),
            listOf(
                ModOp(
                    ModOpType.APPEND,
                    listOf(
                        ProcessorRef(
                            "boonai_analysis.aws.RekognitionTextDetection",
                            StandardContainers.ANALYSIS
                        )
                    )
                )
            ),
            true
        ),
        PipelineModSpec(
            "aws-celebrity-detection",
            "Recognizes thousands of celebrities in a wide range of categories.",
            Provider.AMAZON,
            Category.AWS_REK,
            ModelObjective.FACE_RECOGNITION,
            listOf(FileType.Images, FileType.Videos),
            listOf(
                ModOp(
                    ModOpType.APPEND,
                    listOf(
                        ProcessorRef(
                            "boonai_analysis.aws.RekognitionCelebrityDetection",
                            StandardContainers.ANALYSIS
                        ),
                        ProcessorRef(
                            "boonai_analysis.aws.video.RekognitionCelebrityDetection",
                            StandardContainers.ANALYSIS
                        )
                    )
                )
            ),
            true
        ),
        PipelineModSpec(
            "aws-black-frame-detection",
            "With Amazon Rekognition Video, you can detect such black frame sequences to automate ad insertion, " +
                "package content for VOD, and demarcate various program segments or scenes. ",
            Provider.AMAZON,
            Category.AWS_REK,
            ModelObjective.LABEL_DETECTION,
            listOf(FileType.Videos),
            listOf(
                ModOp(
                    ModOpType.APPEND,
                    listOf(
                        ProcessorRef(
                            "boonai_analysis.aws.video.BlackFramesVideoDetectProcessor",
                            StandardContainers.ANALYSIS
                        )
                    )
                )
            ),
            true
        ),
        PipelineModSpec(
            "aws-end-credits-detection",
            "Amazon Rekognition Video helps you automatically identify the exact frames " +
                "where the closing credits start and end for a movie or TV show.",
            Provider.AMAZON,
            Category.AWS_REK,
            ModelObjective.LABEL_DETECTION,
            listOf(FileType.Videos),
            listOf(
                ModOp(
                    ModOpType.APPEND,
                    listOf(
                        ProcessorRef(
                            "boonai_analysis.aws.video.EndCreditsVideoDetectProcessor",
                            StandardContainers.ANALYSIS
                        )
                    )
                )
            ),
            true
        ),
        PipelineModSpec(
            "aws-transcribe",
            "Amazon Transcribe uses a deep learning process called automatic speech recognition (ASR) " +
                "to convert speech to text quickly and accurately.",
            Provider.AMAZON,
            Category.AWS_TRANS,
            ModelObjective.SPEECH_RECOGNITION,
            listOf(FileType.Videos),
            listOf(
                ModOp(
                    ModOpType.APPEND,
                    listOf(
                        ProcessorRef(
                            "boonai_analysis.aws.AmazonTranscribeProcessor",
                            StandardContainers.ANALYSIS
                        )
                    )
                )
            ),
            true
        ),
        PipelineModSpec(
            "azure-object-detection",
            "Identify and tag visual features in an image with a bounding box, from a set of thousands of recognizable objects and living things.",
            Provider.MICROSOFT,
            Category.AZURE_VISION,
            ModelObjective.OBJECT_DETECTION,
            listOf(FileType.Images, FileType.Videos),
            listOf(
                ModOp(
                    ModOpType.APPEND,
                    listOf(
                        ProcessorRef(
                            "boonai_analysis.azure.AzureVisionObjectDetection",
                            StandardContainers.ANALYSIS
                        ),
                        ProcessorRef(
                            "boonai_analysis.azure.AzureVideoObjectDetection",
                            StandardContainers.ANALYSIS
                        )
                    )
                )
            ),
            true
        ),
        PipelineModSpec(
            "azure-label-detection",
            "Identify and tag visual features in an image, from a set of thousands of recognizable objects, living things, scenery, and actions.",
            Provider.MICROSOFT,
            Category.AZURE_VISION,
            ModelObjective.LABEL_DETECTION,
            listOf(FileType.Images, FileType.Videos),
            listOf(
                ModOp(
                    ModOpType.APPEND,
                    listOf(
                        ProcessorRef(
                            "boonai_analysis.azure.AzureVisionLabelDetection",
                            StandardContainers.ANALYSIS
                        ),
                        ProcessorRef(
                            "boonai_analysis.azure.AzureVideoLabelDetection",
                            StandardContainers.ANALYSIS
                        )
                    )
                )
            ),
            true
        ),
        PipelineModSpec(
            "azure-image-description-detection",
            "Analyze an image and generate a human-readable sentence that describes its contents. ",
            Provider.MICROSOFT,
            Category.AZURE_VISION,
            ModelObjective.IMAGE_DESCRIPTION,
            listOf(FileType.Images, FileType.Videos),
            listOf(
                ModOp(
                    ModOpType.APPEND,
                    listOf(
                        ProcessorRef(
                            "boonai_analysis.azure.AzureVisionImageDescriptionDetection",
                            StandardContainers.ANALYSIS
                        ),
                        ProcessorRef(
                            "boonai_analysis.azure.AzureVideoImageDescriptionDetection",
                            StandardContainers.ANALYSIS
                        )
                    )
                )
            ),
            true
        ),
        PipelineModSpec(
            "azure-celebrity-detection",
            "Recognizes thousands of celebrities in a wide range of categories.",
            Provider.MICROSOFT,
            Category.AZURE_VISION,
            ModelObjective.FACE_RECOGNITION,
            listOf(FileType.Images, FileType.Videos),
            listOf(
                ModOp(
                    ModOpType.APPEND,
                    listOf(
                        ProcessorRef(
                            "boonai_analysis.azure.AzureVisionCelebrityDetection",
                            StandardContainers.ANALYSIS
                        ),
                        ProcessorRef(
                            "boonai_analysis.azure.AzureVideoCelebrityDetection",
                            StandardContainers.ANALYSIS
                        )
                    )
                )
            ),
            true
        ),
        PipelineModSpec(
            "azure-landmark-detection",
            "Detect popular natural and man-made structures within an image",
            Provider.MICROSOFT,
            Category.AZURE_VISION,
            ModelObjective.LANDMARK_DETECTION,
            listOf(FileType.Images, FileType.Videos),
            listOf(
                ModOp(
                    ModOpType.APPEND,
                    listOf(
                        ProcessorRef(
                            "boonai_analysis.azure.AzureVisionLandmarkDetection",
                            StandardContainers.ANALYSIS
                        ),
                        ProcessorRef(
                            "boonai_analysis.azure.AzureVideoLandmarkDetection",
                            StandardContainers.ANALYSIS
                        )
                    )
                )
            ),
            true
        ),
        PipelineModSpec(
            "azure-logo-detection",
            "Brand detection is a specialized mode of object detection that uses a database of thousands of global logos to identify commercial brands.",
            Provider.MICROSOFT,
            Category.AZURE_VISION,
            ModelObjective.LOGO_DETECTION,
            listOf(FileType.Images, FileType.Videos),
            listOf(
                ModOp(
                    ModOpType.APPEND,
                    listOf(
                        ProcessorRef(
                            "boonai_analysis.azure.AzureVisionLogoDetection",
                            StandardContainers.ANALYSIS
                        ),
                        ProcessorRef(
                            "boonai_analysis.azure.AzureVideoLogoDetection",
                            StandardContainers.ANALYSIS
                        )
                    )
                )
            ),
            true
        ),
        PipelineModSpec(
            "azure-category-detection",
            "In addition to tags and a description, generates the taxonomy-based categories detected in an image.",
            Provider.MICROSOFT,
            Category.AZURE_VISION,
            ModelObjective.LABEL_DETECTION,
            listOf(FileType.Images, FileType.Videos),
            listOf(
                ModOp(
                    ModOpType.APPEND,
                    listOf(
                        ProcessorRef(
                            "boonai_analysis.azure.AzureVisionCategoryDetection",
                            StandardContainers.ANALYSIS
                        ),
                        ProcessorRef(
                            "boonai_analysis.azure.AzureVideoCategoryDetection",
                            StandardContainers.ANALYSIS
                        )
                    )
                )
            ),
            true
        ),
        PipelineModSpec(
            "azure-explicit-detection",
            "Detect adult material in images so that developers can restrict the display of these images in their software.",
            Provider.MICROSOFT,
            Category.AZURE_VISION,
            ModelObjective.EXPLICIT_DETECTION,
            listOf(FileType.Images, FileType.Videos),
            listOf(
                ModOp(
                    ModOpType.APPEND,
                    listOf(
                        ProcessorRef(
                            "boonai_analysis.azure.AzureVisionExplicitContentDetection",
                            StandardContainers.ANALYSIS
                        ),
                        ProcessorRef(
                            "boonai_analysis.azure.AzureVideoExplicitContentDetection",
                            StandardContainers.ANALYSIS
                        )
                    )
                )
            ),
            true
        ),
        PipelineModSpec(
            "azure-face-detection",
            "Detect human faces within an image.",
            Provider.MICROSOFT,
            Category.AZURE_VISION,
            ModelObjective.FACE_RECOGNITION,
            listOf(FileType.Images, FileType.Videos),
            listOf(
                ModOp(
                    ModOpType.APPEND,
                    listOf(
                        ProcessorRef(
                            "boonai_analysis.azure.AzureVisionFaceDetection",
                            StandardContainers.ANALYSIS
                        ),
                        ProcessorRef(
                            "boonai_analysis.azure.AzureVideoFaceDetection",
                            StandardContainers.ANALYSIS
                        )
                    )
                )
            ),
            true
        ),
        PipelineModSpec(
            "azure-text-detection",
            "Optical Character Recognition (OCR) capabilities that extract printed or handwritten text from images. ",
            Provider.MICROSOFT,
            Category.AZURE_VISION,
            ModelObjective.TEXT_DETECTION,
            listOf(FileType.Images, FileType.Videos),
            listOf(
                ModOp(
                    ModOpType.APPEND,
                    listOf(
                        ProcessorRef(
                            "boonai_analysis.azure.AzureVisionTextDetection",
                            StandardContainers.ANALYSIS
                        ),
                        ProcessorRef(
                            "boonai_analysis.azure.AzureVideoTextDetection",
                            StandardContainers.ANALYSIS
                        )
                    )
                ),
                ModOp(
                    ModOpType.SET_ARGS,
                    mapOf("ocr" to true),
                    OpFilter(OpFilterType.REGEX, ".*FileImportProcessor")
                )
            ),
            true
        )
    )
}
