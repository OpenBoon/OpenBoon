import os
from azure.cognitiveservices.vision.computervision import ComputerVisionClient
from msrest.authentication import CognitiveServicesCredentials


def get_zvi_azure_cv_client():
    """
    Return an Azure Computer Vision client configured for rekognition with ZVI credentials.

    Returns:
        boto3.client: A boto3 client for recognition
    """
    key = os.environ.get('ZORROA_AZURE_KEY')
    if not key:
        raise RuntimeError('Azure support is not setup for this environment.')

    region = os.environ.get('ZORROA_AZURE_REGION', 'eastus')
    credentials = CognitiveServicesCredentials(key)

    return ComputerVisionClient(
        endpoint="https://" + region + ".api.cognitive.microsoft.com/",
        credentials=credentials
    )
