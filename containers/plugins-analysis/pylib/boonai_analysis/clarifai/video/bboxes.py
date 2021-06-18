import logging

from boonai_analysis.clarifai.images import bboxes as bboxes_images
from boonai_analysis.clarifai.util import AbstractClarifaiVideoProcessor
from boonflow.analysis import LabelDetectionAnalysis

logger = logging.getLogger(__name__)

__all__ = [
    'ClarifaiVideoFaceDetectionProcessor',
    'ClarifaiVideoLogoDetectionProcessor',
    'ClarifaiVideoWeaponDetectionProcessor',
    'ClarifaiVideoCelebrityDetectionProcessor'
]


class AbstractClarifaiVideoBboxProcessor(AbstractClarifaiVideoProcessor):
    """
    This base class is used for all Microsoft Computer Vision features.  Subclasses
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
            try:
                regions = response.outputs[0].data.regions
            except KeyError:
                continue
            if not regions:
                continue
            for region in regions:
                concept = region.data.concepts[0]
                prediction = {concept.name: concept.value}
                clip_tracker.append(time_ms, prediction)
                analysis.add_label_and_score(concept.name, concept.value)
        return analysis, clip_tracker


class ClarifaiVideoFaceDetectionProcessor(AbstractClarifaiVideoBboxProcessor):
    """ Clarifai face detection"""
    attribute_name = 'face-detection'
    image_client_class = bboxes_images.ClarifaiFaceDetectionProcessor


class ClarifaiVideoLogoDetectionProcessor(AbstractClarifaiVideoBboxProcessor):
    """ Clarifai logo detection"""
    attribute_name = 'logo-detection'
    image_client_class = bboxes_images.ClarifaiLogoDetectionProcessor


class ClarifaiVideoWeaponDetectionProcessor(AbstractClarifaiVideoBboxProcessor):
    """ Clarifai weapon detection"""
    attribute_name = 'weapon-detection'
    image_client_class = bboxes_images.ClarifaiWeaponDetectionProcessor


class ClarifaiVideoCelebrityDetectionProcessor(AbstractClarifaiVideoBboxProcessor):
    """ Clarifai celebrity detection"""
    attribute_name = 'celebrity-detection'
    image_client_class = bboxes_images.ClarifaiCelebrityDetectionProcessor
