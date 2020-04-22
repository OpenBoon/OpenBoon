import os

from clarifai.rest import ClarifaiApp


def get_clarifai_app():
    """
    Return a usable ClarifaiApp

    Returns:
        ClarifaiApp: The configured ClarifaiApp
    """
    return ClarifaiApp(api_key=os.environ.get('CLARIFAI_KEY'))

