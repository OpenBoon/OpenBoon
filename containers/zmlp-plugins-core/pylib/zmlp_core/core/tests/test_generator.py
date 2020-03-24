# flake8: noqa
import datetime
import unittest
from unittest.mock import patch

from dateutil.tz import tzutc

from zmlp import ZmlpClient
from zmlp_core.core.generators import GcsBucketGenerator, AssetSearchGenerator, S3BucketGenerator
from zmlpsdk import Context


class TestConsumer:
    def __init__(self):
        self.count = 0

    def accept(self, _):
        self.count += 1


class GcsBucketGeneratorUnitTests(unittest.TestCase):

    def test_generate(self):
        consumer = TestConsumer()
        generator = GcsBucketGenerator()
        generator.set_context(Context(None, {'uri': 'gs://zorroa-dev-data'}, {}))
        generator.generate(consumer)
        assert consumer.count > 0


class MockS3Client(object):
    """
    A Mock AWS S3 client.
    """
    def get_paginator(self, func):
        return self

    def paginate(self, **kwargs):
        return mock_aws_result


class S3BucketGeneratorUnitTests(unittest.TestCase):

    @patch('zmlp_core.core.generators.get_aws_client')
    def test_generate(self, aws_client_patch):
        aws_client_patch.return_value = MockS3Client()
        consumer = TestConsumer()
        generator = S3BucketGenerator()
        generator.set_context(Context(None, {'uri': 's3://zorroa-test-data'}, {}))
        generator.generate(consumer)
        assert consumer.count > 0


class AssetSearchGeneratorTests(unittest.TestCase):

    @patch.object(ZmlpClient, 'delete')
    @patch.object(ZmlpClient, 'post')
    def test_generate(self, post_patch, del_patch):
        post_patch.side_effect = [mock_search_result, {'_scroll_id': 'bones', 'hits': {'hits': []}}]
        del_patch.return_value = {}

        consumer = TestConsumer()
        generator = AssetSearchGenerator()
        generator.set_context(Context(None, {'uri': 'gs://zorroa-dev-data'}, {}))
        generator.generate(consumer)
        assert consumer.count > 0


mock_search_result = {
    'took': 4,
    'timed_out': False,
    '_scroll_id': 'bones',
    'hits': {
        'total': {'value': 2},
        'max_score': 0.2876821,
        'hits': [
            {
                '_index': 'litvqrkus86sna2w',
                '_type': 'asset',
                '_id': 'dd0KZtqyec48n1q1ffogVMV5yzthRRGx2WKzKLjDphg',
                '_score': 0.2876821,
                '_source': {
                    'source': {
                        'path': 'https://i.imgur.com/SSN26nN.jpg'
                    }
                }
            },
            {
                '_index': 'litvqrkus86sna2w',
                '_type': 'asset',
                '_id': 'aabbccddec48n1q1fginVMV5yllhRRGx2WKyKLjDphg',
                '_score': 0.2876821,
                '_source': {
                    'source': {
                        'path': 'https://i.imgur.com/foo.jpg'
                    }
                }
            }
        ]
    }
}

mock_aws_result = [{'ResponseMetadata': {'RequestId': '3EC30547D121F51E',
                                         'HostId': '6RxfBAI09ZHjNnar8Il+dlFD7HPKm+HblOdQR2cabAdwqWQscSXPAsR+N2czZ324XWZdYzuWC0Y=',
                                         'HTTPStatusCode': 200, 'HTTPHeaders': {
        'x-amz-id-2': '6RxfBAI09ZHjNnar8Il+dlFD7HPKm+HblOdQR2cabAdwqWQscSXPAsR+N2czZ324XWZdYzuWC0Y=',
        'x-amz-request-id': '3EC30547D121F51E', 'date': 'Sun, 22 Mar 2020 15:45:21 GMT',
        'x-amz-bucket-region': 'us-east-1', 'content-type': 'application/xml', 'transfer-encoding': 'chunked',
        'server': 'AmazonS3'}, 'RetryAttempts': 0}, 'IsTruncated': False, 'Contents': [
    {'Key': 'beer/', 'LastModified': datetime.datetime(2020, 3, 22, 14, 39, 15, tzinfo=tzutc()),
     'ETag': '"d41d8cd98f00b204e9800998ecf8427e"', 'Size': 0, 'StorageClass': 'STANDARD'},
    {'Key': 'beer/hobo.jpg', 'LastModified': datetime.datetime(2020, 3, 22, 14, 39, 34, tzinfo=tzutc()),
     'ETag': '"149b73bcc989f2a2cad89cfb0c90f84e"', 'Size': 1816523, 'StorageClass': 'STANDARD'},
    {'Key': 'pics/', 'LastModified': datetime.datetime(2020, 3, 22, 14, 57, 43, tzinfo=tzutc()),
     'ETag': '"d41d8cd98f00b204e9800998ecf8427e"', 'Size': 0, 'StorageClass': 'STANDARD'},
    {'Key': 'pics/BHP_SWC_PHOTOGRAPHY.TIF',
     'LastModified': datetime.datetime(2020, 3, 22, 15, 0, 29, tzinfo=tzutc()),
     'ETag': '"3f31ecb8b0c3448afd08da302f7d96b3-2"', 'Size': 28039056, 'StorageClass': 'STANDARD'},
    {'Key': 'pics/DigitalLAD.2048x1556.exr',
     'LastModified': datetime.datetime(2020, 3, 22, 15, 0, 29, tzinfo=tzutc()),
     'ETag': '"a84b9c2281188f2f1bf4928d6d77d988-2"', 'Size': 25518832, 'StorageClass': 'STANDARD'},
    {'Key': 'pics/image_395x512.jpg', 'LastModified': datetime.datetime(2020, 3, 22, 15, 0, 29, tzinfo=tzutc()),
     'ETag': '"e8a368297abc583f2fc5e2baf94cb583"', 'Size': 103453, 'StorageClass': 'STANDARD'},
    {'Key': 'pics/maxresdefault.jpg', 'LastModified': datetime.datetime(2020, 3, 22, 15, 0, 31, tzinfo=tzutc()),
     'ETag': '"aaa6696afab2dbeedb2745cdb901b3bd"', 'Size': 234874, 'StorageClass': 'STANDARD'},
    {'Key': 'pics/spatoon.jpg', 'LastModified': datetime.datetime(2020, 3, 22, 15, 0, 29, tzinfo=tzutc()),
     'ETag': '"6bfcfc5b79076cee9f204ea015f7d351"', 'Size': 267907, 'StorageClass': 'STANDARD'}],
                    'Name': 'zorroa-test-data', 'Prefix': '', 'MaxKeys': 1000, 'EncodingType': 'url', 'KeyCount': 8}]
