import os
import boto3
from boto3.exceptions import S3UploadFailedError

from zmlpsdk.cloud import get_aws_client


class AWSException(Exception):
    """
        AWS Exception
    """
    pass


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


def aws_upload_file(filename='', bucket='', key=''):
    """ Upload file to S3 Bucket

    Args:
        filename: local filepath
        bucket: S3 bucket to upload to
        key: filename when uploaded to S3 (default empty string)

    Returns:
        None

    Raises:
        AWSException if failure to upload to S3 bucket
    """
    s3 = get_aws_client('s3')

    try:
        # upload file with same filename
        if not key:
            key = filename
        s3.upload_file(Filename=filename, Bucket=bucket, Key=key)
    except S3UploadFailedError:
        raise AWSException('Failed to upload {} to {}/{}'.format(filename, bucket, key))
