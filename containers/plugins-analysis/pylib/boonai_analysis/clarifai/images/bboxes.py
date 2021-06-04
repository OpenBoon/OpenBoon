import cv2
from boonai_analysis.clarifai.util import AbstractClarifaiProcessor
from boonflow.analysis import LabelDetectionAnalysis
from boonflow.proxy import get_proxy_level_path

from boonflow import FileTypes

__all__ = [
    'ClarifaiFaceDetectionProcessor',
    'ClarifaiLogoDetectionProcessor',
    'ClarifaiWeaponDetectionProcessor',
    'ClarifaiCelebrityDetectionProcessor'
]


class AbstractClarifaiBboxProcessor(AbstractClarifaiProcessor):
    """
    This base class is used for all Microsoft Computer Vision features.  Subclasses
    only have to implement the "predict(asset, image) method.
    """
    file_types = FileTypes.images | FileTypes.documents

    def process(self, frame):
        asset = frame.asset
        p_path = get_proxy_level_path(asset, 1)
        im = cv2.imread(p_path)
        h, w, _ = im.shape
        response = self.predict(p_path)
        regions = response.outputs[0].data.regions
        if not regions:
            return
        analysis = LabelDetectionAnalysis()
        for region in regions:
            box = region.region_info.bounding_box
            bbox = [box.left_col, box.top_row, box.right_col, box.bottom_row]
            concepts = region.data.concepts[0]
            analysis.add_label_and_score(concepts.name, concepts.value, bbox=bbox)
        asset.add_analysis(self.attribute, analysis)


class ClarifaiFaceDetectionProcessor(AbstractClarifaiBboxProcessor):
    """ Clarifai face detection"""
    attribute_name = 'face-detection'
    model_id = 'f76196b43bbd45c99b4f3cd8e8b40a8a'


class ClarifaiLogoDetectionProcessor(AbstractClarifaiBboxProcessor):
    """ Clarifai logo detection"""
    attribute_name = 'logo-detection'
    model_id = 'c443119bf2ed4da98487520d01a0b1e3'


class ClarifaiWeaponDetectionProcessor(AbstractClarifaiBboxProcessor):
    """ Clarifai logo detection"""
    attribute_name = 'weapon-detection'
    model_id = '6afba5f2e2787adfc0f71dcfca3eb364'


class ClarifaiCelebrityDetectionProcessor(AbstractClarifaiBboxProcessor):
    attribute_name = 'celebrity-detection'
    model_id = 'e466caa0619f444ab97497640cefc4dc'
