from boonai_analysis.clarifai.images import labels as labels_images
from boonai_analysis.clarifai.util import AbstractClarifaiVideoProcessor
from boonflow.analysis import LabelDetectionAnalysis

__all__ = [
    'ClarifaiVideoLabelDetectionProcessor',
    'ClarifaiVideoFoodDetectionProcessor',
    'ClarifaiVideoTravelDetectionProcessor',
    'ClarifaiVideoApparelDetectionProcessor',
    'ClarifaiVideoWeddingDetectionProcessor',
    'ClarifaiVideoExplicitDetectionProcessor',
    'ClarifaiVideoModerationDetectionProcessor',
    'ClarifaiVideoTexturesDetectionProcessor',
    'ClarifaiVideoAgeDetectionProcessor',
    'ClarifaiVideoGenderDetectionProcessor',
    'ClarifaiVideoEthnicityDetectionProcessor',
    'ClarifaiVideoRoomTypesDetectionProcessor'
]


class AbstractClarifaiVideoLabelProcessor(AbstractClarifaiVideoProcessor):
    """
    This base class is used for all Clarifai features.  Subclasses
    only have to implement the "predict(asset, image) method.
    """
    def set_analysis(self, extractor, clip_tracker):
        """ Set up ClipTracker and Asset Detection Analysis

        Args:
            extractor: ShotBasedFrameExtractor
            clip_tracker: ClipTracker

        Returns:
            (tuple): asset detection analysis, clip_tracker
        """
        analysis = LabelDetectionAnalysis(collapse_labels=True)
        for time_ms, path in extractor:
            response = self.predict(path)
            concepts = response.outputs[0].data.concepts
            if not concepts:
                continue
            predictions = {}
            for concept in concepts:
                predictions[concept.name] = concept.value
                analysis.add_label_and_score(concept.name, concept.value)
            clip_tracker.append(time_ms, predictions)
        return analysis, clip_tracker


class ClarifaiVideoLabelDetectionProcessor(AbstractClarifaiVideoLabelProcessor):
    """ Clarifai label detection"""
    attribute_name = 'label-detection'
    image_client_class = labels_images.ClarifaiLabelDetectionProcessor


class ClarifaiVideoFoodDetectionProcessor(AbstractClarifaiVideoLabelProcessor):
    """ Clarifai food detection"""
    attribute_name = 'food-detection'
    image_client_class = labels_images.ClarifaiFoodDetectionProcessor


class ClarifaiVideoTravelDetectionProcessor(AbstractClarifaiVideoLabelProcessor):
    """ Clarifai travel detection"""
    attribute_name = 'travel-detection'
    image_client_class = labels_images.ClarifaiTravelDetectionProcessor


class ClarifaiVideoApparelDetectionProcessor(AbstractClarifaiVideoLabelProcessor):
    """ Clarifai apparel detection"""
    attribute_name = 'apparel-detection'
    image_client_class = labels_images.ClarifaiApparelDetectionProcessor


class ClarifaiVideoWeddingDetectionProcessor(AbstractClarifaiVideoLabelProcessor):
    """ Clarifai wedding detection"""
    attribute_name = 'wedding-detection'
    image_client_class = labels_images.ClarifaiWeddingDetectionProcessor


class ClarifaiVideoExplicitDetectionProcessor(AbstractClarifaiVideoLabelProcessor):
    """ Clarifai explicit detection"""
    attribute_name = 'nsfw-detection'
    image_client_class = labels_images.ClarifaiExplicitDetectionProcessor


class ClarifaiVideoModerationDetectionProcessor(AbstractClarifaiVideoLabelProcessor):
    """ Clarifai moderation detection"""
    attribute_name = 'unsafe-detection'
    image_client_class = labels_images.ClarifaiModerationDetectionProcessor


class ClarifaiVideoTexturesDetectionProcessor(AbstractClarifaiVideoLabelProcessor):
    """ Clarifai textures detection"""
    attribute_name = 'texture-detection'
    image_client_class = labels_images.ClarifaiTexturesDetectionProcessor


class ClarifaiVideoAgeDetectionProcessor(AbstractClarifaiVideoLabelProcessor):
    """ Clarifai age detection"""
    attribute_name = 'age-detection'
    image_client_class = labels_images.ClarifaiAgeDetectionProcessor


class ClarifaiVideoGenderDetectionProcessor(AbstractClarifaiVideoLabelProcessor):
    """ Clarifai gender detection"""
    attribute_name = 'gender-detection'
    image_client_class = labels_images.ClarifaiGenderDetectionProcessor


class ClarifaiVideoEthnicityDetectionProcessor(AbstractClarifaiVideoLabelProcessor):
    """ Clarifai ethnicity detection"""
    attribute_name = 'ethnicity-detection'
    image_client_class = labels_images.ClarifaiEthnicityDetectionProcessor


class ClarifaiVideoRoomTypesDetectionProcessor(AbstractClarifaiVideoLabelProcessor):
    """ Clarifai room types detection"""
    attribute_name = 'room-types-detection'
    image_client_class = labels_images.ClarifaiRoomTypesDetectionProcessor
