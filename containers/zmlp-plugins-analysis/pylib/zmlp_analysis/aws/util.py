# Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
# PDX-License-Identifier: MIT-0 (For details,
# see https://github.com/awsdocs/amazon-rekognition-custom-labels-developer-guide/blob/master/
# LICENSE-SAMPLECODE.)

import logging
import os
from enum import Enum
import json

import boto3
import botocore.waiter

logger = logging.getLogger(__name__)


class AwsEnv:
    """
    AWS client utility client.
    """

    @staticmethod
    def s3():
        return boto3.client('s3', **AwsEnv.get_aws_env())

    @staticmethod
    def transcribe():
        return boto3.client('transcribe', **AwsEnv.get_aws_env())

    @staticmethod
    def rekognition():
        """
        Return an AWS client configured for rekognition with ZVI credentials.

        Returns:
            boto3.client: A boto3 client for recognition
        """
        return boto3.client('rekognition', **AwsEnv.get_aws_env())

    @staticmethod
    def get_aws_env():
        aws_env = {
            'aws_access_key_id': os.environ.get('ZORROA_AWS_KEY'),
            'aws_secret_access_key': os.environ.get('ZORROA_AWS_SECRET'),
            'region_name': os.environ.get('ZORROA_AWS_REGION', 'us-east-1')
        }
        if None in aws_env.values():
            raise RuntimeError('AWS support is not setup for this environment.')
        return aws_env

    @staticmethod
    def get_bucket_name():
        bucket = os.environ.get('ZORROA_AWS_BUCKET')
        if not bucket:
            raise RuntimeError('AWS support is not setup for this environment.')
        return bucket

    @staticmethod
    def get_project_arn():
        """
        Get Rekognition Project Version ARN (custom model ARN)

        Returns:
            (str): ARN name
        """
        return os.environ.get('ZORROA_AWS_ML_USER_PROJECT_ARN')

    @staticmethod
    def get_project_version_arn():
        """
        Get Rekognition Project Version ARN (custom model ARN)

        Returns:
            (str): ARN name
        """
        return os.environ.get('ZORROA_AWS_ML_USER_PROJECT_VERSION_ARN')


class CustomModelTrainer:
    """ Methods for custom label models """
    def __init__(self, rek_client):
        self.rek_client = rek_client or None

    def init(self):
        if not self.rek_client:
            self.rek_client = AwsEnv.rekognition()

    def create_project(self, project_name=None):
        """ Create an Amazon Rekognition project

        Args:
            project_name: (str) project name

        Returns:
            (dict) Project ARN as 'ProjectArn'
        """
        return self.rek_client.create_project(ProjectName=project_name)

    def start_model(self, project_arn, version_name, project_version_arn, min_inference_units=1):
        """ Start the running version of a model (can take a while to complete)

        Args:
            project_arn: (str) Custom Labels Project ARN for the model to train
            version_name: (str) unique name for the version of the model
            project_version_arn: (str) model ARN to start
            min_inference_units: (int) minimum number of inference units to use (1-5)

        Returns:
            (dict) response
        """
        response = self.rek_client.start_project_version(
            ProjectVersionArn=project_version_arn,
            MinInferenceUnits=min_inference_units
        )

        # Wait for the project version training to complete
        project_version_starting_completed_waiter = self.rek_client.get_waiter(
            'project_version_running')
        project_version_starting_completed_waiter.wait(ProjectArn=project_arn,
                                                       VersionNames=[version_name])

        return response

    def stop_model(self, project_version_arn):
        """ Stop the running version of a model (can take a while to complete)

        Args:
            project_version_arn: (str) model ARN to stop

        Returns:
            (dict) response
        """
        return self.rek_client.stop_project_version(ProjectVersionArn=project_version_arn)

    def get_model_status(self, project_arn, version_names):
        """

        Args:
            project_arn: (str) Custom Labels Project ARN for the model to train
            version_names: (str) model version name to describe

        Returns:
            (str) Model Status
        """
        response = self.rek_client.describe_project_versions(
            ProjectArn=project_arn,
            VersionNames=[version_names]
        )

        return response['ProjectVersionDescriptions'][0]['Status']

    def train_model(
            self,
            project_arn,
            version_name,
            output_s3bucket,
            output_s3_key_prefix,
            training_dataset_bucket,
            training_dataset_name,
            testing_dataset_bucket=None,
            testing_dataset_name=None
    ):
        """ Create a new version of a model and begin training

        Args:
            project_arn: (str) Custom Labels Project ARN for the model to train
            version_name: (str) unique name for the version of the model
            output_s3bucket: (str) S3 bucket location to store the results of training
            output_s3_key_prefix: (str) folder within output_s3bucket to store results of training
            training_dataset_bucket: (str) S3 bucket for dataset to use for training
            training_dataset_name: (str) S3 bucket location for manifest file to use for training
            testing_dataset_bucket: (str, optional) S3 bucket for dataset to use for testing
            testing_dataset_name: (str, optional) S3 bucket location for manifest file to use for
                testing

        Examples:
            Given S3 bucket custom-model-bucket/evaluation
            output_s3bucket = "custom-model-bucket"
            output_s3_key_prefix = "evaluation"

            Given S3 bucket custom-model-bucket/training/out.manifest
            training_dataset_bucket="custom-model-bucket"
            training_dataset_name = "training/out.manifest"

            Given S3 bucket custom-model-bucket/testing/out.manifest
            training_dataset_bucket="custom-model-bucket"
            training_dataset_name = "testing/out.manifest"

        Returns:
            (dict) Trained model ARN as 'ProjectVersionArn'
        """
        output_config = json.loads(
            """{{
                "S3Bucket": "{}",
                "S3KeyPrefix": "{}"
            }}""".format(output_s3bucket, output_s3_key_prefix)
        )

        training_dataset = json.loads(
            """{{
                "Assets": [
                    {{
                        "GroundTruthManifest": {{
                            "S3Object": {{
                                "Bucket": "{}",
                                "Name": "{}"
                            }}
                        }}
                    }}
                ]
            }}""".format(training_dataset_bucket, training_dataset_name)
        )

        if testing_dataset_bucket:
            testing_dataset = json.loads(
                """{{
                    "Assets": [
                        {{
                            "GroundTruthManifest":
                                {{
                                    "S3Object": {{
                                        "Bucket": "{}",
                                        "Name": "{}"
                                    }}
                                }}
                        }}
                    ]
                }}""".format(testing_dataset_bucket, testing_dataset_name)
            )
        else:
            testing_dataset = json.loads('{"AutoCreate":true}')

        try:
            response = self.rek_client.create_project_version(
                ProjectArn=project_arn,
                VersionName=version_name,
                OutputConfig=output_config,
                TrainingData=training_dataset,
                TestingData=testing_dataset
            )

            # Wait for the project version training to complete
            project_version_training_completed_waiter = self.rek_client.get_waiter(
                'project_version_training_completed')
            project_version_training_completed_waiter.wait(ProjectArn=project_arn,
                                                           VersionNames=[version_name])

            # Get the completion status
            describe_response = self.rek_client.describe_project_versions(
                ProjectArn=project_arn,
                VersionNames=[version_name]
            )
            for model in describe_response['ProjectVersionDescriptions']:
                logging.info("Status: " + model['Status'])
                logging.info("Message: " + model['StatusMessage'])

            return response

        except Exception as e:
            logging.error(e)


class WaitState(Enum):
    SUCCESS = 'success'
    FAILURE = 'failure'


class CustomWaiter:
    """
    Base class for a custom waiter that leverages botocore's waiter code. Waiters
    poll an operation, with a specified delay between each polling attempt, until
    either an accepted result is returned or the number of maximum attempts is reached.

    To use, implement a subclass that passes the specific operation, arguments,
    and acceptors to the superclass.

    For example, to implement a custom waiter for the transcription client that
    waits for both success and failure outcomes of the get_transcription_job function,
    create a class like the following:

        class TranscribeCompleteWaiter(CustomWaiter):
        def __init__(self, client):
            super().__init__(
                'TranscribeComplete', 'GetTranscriptionJob',
                'TranscriptionJob.TranscriptionJobStatus',
                {'COMPLETED': WaitState.SUCCESS, 'FAILED': WaitState.FAILURE},
                client)

        def wait(self, job_name):
            self._wait(TranscriptionJobName=job_name)

    """

    def __init__(self, name, operation, argument, acceptors, client, delay=10, max_tries=60):
        """ Subclasses should pass specific operations, arguments, and acceptors to
        their super class.

        Args:
            name: The name of the waiter. This can be any descriptive string.
            operation: The operation to wait for. This must match the casing of the underlying
            operation model, which is typically in CamelCase.
            argument: The dict keys used to access the result of the operation, in dot notation.
            For example, 'Job.Status' will access result['Job']['Status'].
            acceptors: The list of acceptors that indicate the wait is over. These can indicate
            either success or failure. The acceptor values are compared to the result of the
            operation after the argument keys are applied.
            client: The Boto3 client.
            delay: The number of seconds to wait between each call to the operation.
            max_tries: The maximum number of tries before exiting.

        Returns:
            None
        """
        self.name = name
        self.operation = operation
        self.argument = argument
        self.client = client
        self.waiter_model = botocore.waiter.WaiterModel({
            'version': 2,
            'waiters': {
                name: {
                    "delay": delay,
                    "operation": operation,
                    "maxAttempts": max_tries,
                    "acceptors": [{
                        "state": state.value,
                        "matcher": "path",
                        "argument": argument,
                        "expected": expected
                    } for expected, state in acceptors.items()]
                }}})
        self.waiter = botocore.waiter.create_waiter_with_client(
            self.name, self.waiter_model, self.client)

    def __call__(self, parsed, **kwargs):
        """ Handles the after-call event by logging information about the operation and its
        result.

        Args:
            parsed: The parsed response from polling the operation.
            **kwargs: Not used, but expected by the caller.

        Returns:
            None
        """
        status = parsed
        for key in self.argument.split('.'):
            status = status.get(key)
        logger.info(
            "Waiter %s called %s, got %s.", self.name, self.operation, status)

    def _wait(self, **kwargs):
        """ Registers for the after-call event and starts the botocore wait loop.

        Args:
            **kwargs: Keyword arguments that are passed to the operation being polled.

        Returns:
            None
        """
        event_name = f'after-call.{self.client.meta.service_model.service_name}'
        self.client.meta.events.register(event_name, self)
        self.waiter.wait(**kwargs)
        self.client.meta.events.unregister(event_name, self)


class TranscribeCompleteWaiter(CustomWaiter):
    """
    Waits for the transcription to complete.
    """

    def __init__(self, client):
        super().__init__(
            'TranscribeComplete', 'GetTranscriptionJob',
            'TranscriptionJob.TranscriptionJobStatus',
            {'COMPLETED': WaitState.SUCCESS, 'FAILED': WaitState.FAILURE},
            client)

    def wait(self, job_name):
        self._wait(TranscriptionJobName=job_name)
