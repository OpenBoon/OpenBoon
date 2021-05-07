import os
import logging

from clarifai_grpc.channel.clarifai_channel import ClarifaiChannel
from clarifai_grpc.grpc.api import service_pb2_grpc, service_pb2, resources_pb2

logger = logging.getLogger('clarifai')

"""Used to mate the clarifai model to a namespace"""
model_map = {
    'apparel_model': 'apparel-detection',
    'food_model': 'food-detection',
    'general_model': 'label-detection',
    'moderation_model': 'unsafe-detection',
    'nsfw_model': 'nsfw-detection',
    'textures_and_patterns_model': 'texture-detection',
    'travel_model': 'travel-detection',
    'wedding_model': 'wedding-detection',
    'face_detection_model': 'face-detection',
    'logo_model': 'logo-detection',
    'celebrity_model': 'celebrity-detection',
    'demographics_model': 'demographics-detection'
}

"""The current API doesn't have a way to find a model id by name"""
model_id_map = {
    'apparel_model': 'e0be3b9d6a454f0493ac3a30784001ff',
    'food_model': 'bd367be194cf45149e75f01d59f77ba7',
    'general_model': 'aaa03c23b3724a16a56b629203edc62c',
    'moderation_model': 'd16f390eb32cad478c7ae150069bd2c6',
    'nsfw_model': 'e9576d86d2004ed1a38ba0cf39ecb4b1',
    'textures_and_patterns_model': 'fbefb47f9fdb410e8ce14f24f54b47ff',
    'travel_model': 'eee28c313d69466f836ab83287a54ed9',
    'wedding_model': 'c386b7a870114f4a87477c0824499348',
    'face_detection_model': 'e15d0f873e66047e579f90cf82c9882z',
    'logo_model': 'c443119bf2ed4da98487520d01a0b1e3',
    'celebrity_model': 'cfbb105cb8f54907bb8d553d68d9fe20',
}


def get_clarifai_app():
    """
    Return a usable ClarifaiApp

    Returns:
        ClarifaiApp: The configured ClarifaiApp
    """
    channel = ClarifaiChannel.get_json_channel()
    stub = service_pb2_grpc.V2Stub(channel)
    auth = (('authorization', 'Key 4137b477d39144c7a9760baefe8eab0f'),)

    return stub, auth


def not_a_quota_exception(exp):
    """
    Returns true if the exception is not a Clarifai quota exception.  This ensures the backoff
    function doesn't sleep on the wrong exceptions.

    Args:
        exp (Exception): The exception

    Returns:
        bool: True if not a quota exception.
    """
    return getattr(exp, 'status_code', 999) != 429


def log_backoff_exception(details):
    """
    Log an exception from the backoff library.

    Args:
        details (dict): The details of the backoff call.

    """
    logger.warning(
        'Waiting on quota {wait:0.1f} seconds afters {tries} tries'.format(**details))
