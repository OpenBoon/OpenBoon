import os
import boto3


def get_zvi_rekognition_client():
    """
    Return an AWS client configured for rekognition with ZVI credentials.

    Returns:
        boto3.client: A boto3 client for recognition
    """
    return boto3.client(
        'rekognition',
        region_name='us-east-2',
        aws_access_key_id=os.environ['ZORROA_AWS_KEY'],
        aws_secret_access_key=os.environ['ZORROA_AWS_SECRET']
    )
