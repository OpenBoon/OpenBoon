import os

from clarifai.rest import ClarifaiApp


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
    return 'Too Many Requests' not in str(exp)
