import os
from azure.cognitiveservices.vision.computervision import ComputerVisionClient
from msrest.authentication import CognitiveServicesCredentials


def get_zvi_azure_cv_client():
    """
    Return an Azure Computer Vision client configured for rekognition with ZVI credentials.

    Returns:
        boto3.client: A boto3 client for recognition
    """
    try:
        key = os.environ['AZURE_ACCOUNT_KEY']
    except KeyError:
        key = None
    region = 'eastus'
    credentials = CognitiveServicesCredentials(key)

    return ComputerVisionClient(
        endpoint="https://" + region + ".api.cognitive.microsoft.com/",
        credentials=credentials
    )
