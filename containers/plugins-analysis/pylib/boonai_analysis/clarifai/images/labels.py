from boonai_analysis.clarifai.util import AbstractClarifaiProcessor
from boonflow.analysis import LabelDetectionAnalysis
from boonflow.proxy import get_proxy_level_path

from boonflow import FileTypes

__all__ = [
    'ClarifaiLabelDetectionProcessor',
    'ClarifaiFoodDetectionProcessor',
    'ClarifaiTravelDetectionProcessor',
    'ClarifaiApparelDetectionProcessor',
    'ClarifaiWeddingDetectionProcessor',
    'ClarifaiExplicitDetectionProcessor',
    'ClarifaiModerationDetectionProcessor',
    'ClarifaiTexturesDetectionProcessor',
    'ClarifaiAgeDetectionProcessor',
    'ClarifaiGenderDetectionProcessor',
    'ClarifaiEthnicityDetectionProcessor',
    'ClarifaiRoomTypesDetectionProcessor'
]


class AbstractClarifaiLabelProcessor(AbstractClarifaiProcessor):
    file_types = FileTypes.images | FileTypes.documents

    def process(self, frame):
        asset = frame.asset
        p_path = get_proxy_level_path(asset, 1)
        response = self.predict(p_path)
        concepts = response.outputs[0].data.concepts
        if not concepts:
            return
        analysis = LabelDetectionAnalysis()
        for concept in concepts:
            analysis.add_label_and_score(concept.name, concept.value)
        asset.add_analysis(self.attribute, analysis)


class ClarifaiLabelDetectionProcessor(AbstractClarifaiLabelProcessor):
    """ Clarifai label detection"""
    attribute_name = 'label-detection'
    model_id = 'fedcc08b2b72481aa17d4b8153570cc1'


class ClarifaiFoodDetectionProcessor(AbstractClarifaiLabelProcessor):
    """ Clarifai food detection"""
    attribute_name = 'food-detection'
    model_id = 'bd367be194cf45149e75f01d59f77ba7'


class ClarifaiTravelDetectionProcessor(AbstractClarifaiLabelProcessor):
    """ Clarifai travel detection"""
    attribute_name = 'travel-detection'
    model_id = '79fbfbae4e30492b85ab2a8758273d76'


class ClarifaiApparelDetectionProcessor(AbstractClarifaiLabelProcessor):
    """ Clarifai apparel detection"""
    attribute_name = 'apparel-detection'
    model_id = 'e0be3b9d6a454f0493ac3a30784001ff'


class ClarifaiWeddingDetectionProcessor(AbstractClarifaiLabelProcessor):
    """ Clarifai wedding detection"""
    attribute_name = 'wedding-detection'
    model_id = 'c386b7a870114f4a87477c0824499348'


class ClarifaiExplicitDetectionProcessor(AbstractClarifaiLabelProcessor):
    """ Clarifai explicit detection"""
    attribute_name = 'nsfw-detection'
    model_id = 'e9576d86d2004ed1a38ba0cf39ecb4b1'


class ClarifaiModerationDetectionProcessor(AbstractClarifaiLabelProcessor):
    """ Clarifai moderation detection"""
    attribute_name = 'unsafe-detection'
    model_id = 'd16f390eb32cad478c7ae150069bd2c6'


class ClarifaiTexturesDetectionProcessor(AbstractClarifaiLabelProcessor):
    """ Clarifai textures detection"""
    attribute_name = 'texture-detection'
    model_id = 'fbefb47f9fdb410e8ce14f24f54b47ff'


class ClarifaiAgeDetectionProcessor(AbstractClarifaiLabelProcessor):
    """ Clarifai age detection"""
    attribute_name = 'age-detection'
    model_id = '36f90889189ad96c516d134bc713004d'


class ClarifaiGenderDetectionProcessor(AbstractClarifaiLabelProcessor):
    """ Clarifai gender detection"""
    attribute_name = 'gender-detection'
    model_id = 'af40a692dfe6040f23ca656f4e144fc2'


class ClarifaiEthnicityDetectionProcessor(AbstractClarifaiLabelProcessor):
    """ Clarifai ethnicity detection"""
    attribute_name = 'ethnicity-detection'
    model_id = '93c277ec3940fba661491fda4d3ccfa0'


class ClarifaiRoomTypesDetectionProcessor(AbstractClarifaiLabelProcessor):
    """ Clarifai room types detection"""
    attribute_name = 'room-types-detection'
    model_id = 'def7f8f57be14c468d22d3a8601c421e'
