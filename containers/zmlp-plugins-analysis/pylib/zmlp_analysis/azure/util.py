import os

from azure.cognitiveservices.vision.computervision import ComputerVisionClient
from msrest.authentication import CognitiveServicesCredentials

# key = os.environ['ACCOUNT_KEY']
key = "<KEY>"

def get_zvi_azure_cv_client():
    """
    Return an AWS client configured for rekognition with ZVI credentials.

    Returns:
        boto3.client: A boto3 client for recognition
    """
    region = 'eastus'
    credentials = CognitiveServicesCredentials(key)

    return ComputerVisionClient(
        endpoint="https://" + region + ".api.cognitive.microsoft.com/",
        credentials=credentials
    )
