import os

from clarifai.rest import ClarifaiApp

"""Used to male the clarifai model to a namespace"""
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


def get_clarifai_app():
    """
    Return a usable ClarifaiApp

    Returns:
        ClarifaiApp: The configured ClarifaiApp
    """
    return ClarifaiApp(api_key=os.environ.get('CLARIFAI_KEY'))


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
