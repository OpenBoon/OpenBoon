import os
import boto3


def get_zvi_rekognition_client():
    """
    Return an AWS client configured for rekognition with ZVI credentials.

    Returns:
        boto3.client: A boto3 client for recognition
    """
    key = os.environ.get('ZORROA_AWS_KEY')
    secret = os.environ.get('ZORROA_AWS_SECRET')
    region = os.environ.get('ZORROA_AWS_REGION', 'us-east-2')

    if not key or not secret:
        raise RuntimeError('AWS support is not setup for this environment.')

    return boto3.client(
        'rekognition',
        region_name=region,
        aws_access_key_id=key,
        aws_secret_access_key=secret
    )
