#!/usr/bin/python
import inspect
import json
import os
import tempfile
import unittest
from unittest.mock import patch

import zorroa.zsdk as zsdk
from zorroa.zsdk import Context
from zorroa.zsdk.testing import PluginUnitTestCase, zorroa_test_data
from zplugins.core.generators import AssetSearchGenerator
from zplugins.core.generators import FileGenerator, FileSystemGenerator


class Consumer:
    def __init__(self):
        self.count = 0

    def accept(self, frame):
        self.count += 1


class Tests(unittest.TestCase):
    def test_file_generator(self):
        c = Consumer()
        fp = FileGenerator()
        toucan = zorroa_test_data("images/set01/toucan.jpg")
        fp.set_context(zsdk.Context(None, {"paths": [toucan]}, {}))
        fp.generate(c)
        self.assertEquals(1, c.count)

    def test_scan_file_system_generator(self):
        c = Consumer()
        fp = FileSystemGenerator()
        image_dir = zorroa_test_data("images/set01")
        fp.set_context(zsdk.Context(None, {"paths": [image_dir]}, {}))
        fp.generate(c)
        # depends on the fact there is 5 files in the test dir
        self.assertEquals(5, c.count)

    def test_non_utf_file_system_generator(self):
        c = Consumer()
        fp = FileSystemGenerator()
        image_dir = tempfile.mkdtemp()

        f1 = tempfile.NamedTemporaryFile(
            prefix='\xe4\xb8\x89\xe9\xab\x98\xe8\x95\x83\xe8\x8c\x84\xe6\xa0\x91',
            suffix='jpg', dir=image_dir)
        f2 = tempfile.NamedTemporaryFile(prefix='stra\xc3\x9fe', suffix='tif', dir=image_dir)

        fp.set_context(zsdk.Context(None, {"paths": [image_dir]}, {}))
        fp.generate(c)
        # depends on the fact there is 2 files in the test dir
        self.assertEquals(2, c.count)


class UnittestConsumer(object):
    def __init__(self):
        self.count = 0
        self.frames = []

    def accept(self, frame):
        self.count += 1
        self.frames.append(frame)


class AssetSearchGeneratorUnitTestCase(PluginUnitTestCase):
    def setUp(self):
        super(AssetSearchGeneratorUnitTestCase, self).setUp()
        self.consumer = UnittestConsumer()
        self.generator = AssetSearchGenerator()

    @patch('zorroa.zclient.ZmlpClient.post')
    def test_generate_scroll(self, post_patch):
        pwd = os.path.dirname(inspect.getfile(self.__class__))
        response_1 = json.load(open(pwd + '/response_1.json', 'r'))

        response_2 = json.load(open(pwd + '/response_2.json', 'r'))

        post_patch.side_effect = [response_1, response_2]
        args = {'search': {u'filter': {u'similarity': {
            u'analysis.imageSimilarity.shash': {u'minScore': 100, u'hashes': [
                {u'hash': u'd4fa3a87-bd81-43d6-aa1b-b7a99fe0607b',
                 u'weight': 1}]}}}, u'fields': [u'media*', u'proxies*',
                                                u'source.extension',
                                                u'source.filename',
                                                u'source.mediaType',
                                                u'source.type',
                                                u'source.fileSize',
                                                u'source.path'], u'aggs': None,
            u'postFilter': None, u'size': 100},
            'scroll': False}
        self.generator.set_context(Context(None, args))
        self.generator.init()
        self.generator.generate(self.consumer)
        assert self.consumer.count == 4
        frame_1 = self.consumer.frames[0]
        assert frame_1.asset.id == 'd4fa3a87-bd81-43d6-aa1b-b7a99fe0607b'
        assert frame_1.asset.get_attr('media.pages') == 618

    @patch('zorroa.zclient.ZmlpClient.post')
    def test_generate_page(self, post_patch):
        pwd = os.path.dirname(inspect.getfile(self.__class__))
        response_1 = json.load(open(pwd + '/response_1.json', 'r'))

        response_2 = json.load(open(pwd + '/response_2.json', 'r'))

        post_patch.side_effect = [response_1, response_2]
        args = {'search': {u'filter': {u'similarity': {
            u'analysis.imageSimilarity.shash': {u'minScore': 100, u'hashes': [
                {u'hash': u'd4fa3a87-bd81-43d6-aa1b-b7a99fe0607b',
                 u'weight': 1}]}}}, u'fields': [u'media*', u'proxies*',
                                                u'source.extension',
                                                u'source.filename',
                                                u'source.mediaType',
                                                u'source.type',
                                                u'source.fileSize',
                                                u'source.path'], u'aggs': None,
            u'postFilter': None, u'size': 100}}
        self.generator.set_context(Context(None, args))
        self.generator.init()
        self.generator.generate(self.consumer)
        assert self.consumer.count == 4
        frame_1 = self.consumer.frames[0]
        assert frame_1.asset.id == 'd4fa3a87-bd81-43d6-aa1b-b7a99fe0607b'
        assert frame_1.asset.get_attr('media.pages') == 618

    @patch('zorroa.zclient.ZmlpClient.post')
    def test_generate_max_assets(self, post_patch):
        pwd = os.path.dirname(inspect.getfile(self.__class__))
        response_1 = json.load(open(pwd + '/response_1.json', 'r'))

        response_2 = json.load(open(pwd + '/response_2.json', 'r'))

        post_patch.side_effect = [response_1, response_2]
        args = {
            'search': {},
            'scroll': False,
            'max_assets': 1
        }
        self.generator.set_context(Context(None, args))
        self.generator.init()
        self.generator.generate(self.consumer)
        assert self.consumer.count == 1
