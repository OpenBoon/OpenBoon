# SPDX-License-Identifier: Apache-2.0

"""
Base class for implementing custom waiters for services that don't already have
prebuilt waiters. This class leverages botocore waiter code.
"""

import os
import boto3
import botocore.waiter
from enum import Enum
import logging

logger = logging.getLogger(__name__)


class AWSClient:

    def __init__(self, key=None, secret=None, region='us-east-2'):
        self.key = os.environ.get('ZORROA_AWS_KEY', key)
        self.secret = os.environ.get('ZORROA_AWS_SECRET', secret)
        self.region = os.environ.get('ZORROA_AWS_REGION', region)

    def get_aws_client(self, service_type=boto3.client, service='s3'):
        return service_type(
            service,
            region_name=self.region,
            aws_access_key_id=self.key,
            aws_secret_access_key=self.secret
        )


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
