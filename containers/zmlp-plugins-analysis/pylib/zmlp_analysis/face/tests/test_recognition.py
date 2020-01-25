#!/usr/bin/env python
import logging
import os
from unittest.mock import patch

from zmlp import ZmlpClient
from zmlpsdk import Frame
from zmlpsdk.proxy import store_asset_proxy
from zmlpsdk.storage import ProjectStorage
from zmlpsdk.testing import TestAsset, PluginUnitTestCase, zorroa_test_data
from zmlp_analysis.face.recognition import ZmlpFaceRecognitionProcessor, \
    ZmlpBuildFaceRecognitionModel

logging.basicConfig(level=logging.DEBUG)


class BuildFaceRecognitionModelTests(PluginUnitTestCase):
    mock_search_result = {
        "hits": {
            "total": {
                "value": 1
            },
            "hits": [{
                "_index": "litvqrkus86sna2w",
                "_type": "asset",
                "_source": {
                    "datasets": {
                        "zmlpFaceRecognition": [
                            {
                                "label": "Bob Dole",
                                "point": [426, 824]
                            }
                        ]
                    },
                    "source": {
                        "path": "https://i.imgur.com/SSN26nN.jpg",
                        "nestedSource": {
                            "nestedKey": "nestedValue"
                        }
                    }
                },
                "_id": "dd0KZtqyec48n1q1ffogVMV5yzthRRGx2WKzKLjDphg",
                "_score": 0.2876821
            }
            ]
        }
    }

    @patch.object(ZmlpClient, 'post')
    @patch('zmlp_analysis.face.recognition.get_proxy_level')
    @patch.object(ZmlpClient, 'upload_file')
    def test_build_model(self, upload_patch, proxy_patch, search_patch):
        search_patch.return_value = self.mock_search_result
        proxy_patch.return_value = zorroa_test_data("images/face-recognition/face1.jpg", False)
        upload_patch.return_value = {
            "name": "encodings.dat",
            "entity": "model",
            "category": "zmlpFaceRecognition",
            "mimetype": "application/octet-stream",
            "attrs": {
            }
        }

        processor = self.init_processor(ZmlpBuildFaceRecognitionModel(), {})
        model = processor.build_model()
        assert 1 == len(model['encodings'])
        assert 1 == len(model['labels'])


class FaceRecognitionProcessorTests(PluginUnitTestCase):

    @patch.object(ZmlpClient, 'upload_file')
    @patch.object(ProjectStorage, 'localize_file')
    def test_process(self, localize_patch, upload_patch):
        localize_patch.return_value = None
        upload_patch.return_value = {
            "name": "ZmlpFaceRecognition_200x200.jpg",
            "category": "proxy",
            "mimetype": "image/jpeg",
            "attrs": {
                "width": 512,
                "height": 339
            }
        }

        test_faces_path = zorroa_test_data("images/face-recognition/face2.jpg")
        frame = Frame(TestAsset(test_faces_path))
        store_asset_proxy(frame.asset, test_faces_path, (512, 339))
        processor = self.init_processor(ZmlpFaceRecognitionProcessor(), {})
        processor.process(frame)

        asset = frame.asset
        element = asset.get_attr('elements')[0]
        assert 'face' == element['type']
        assert None is element.get('labels')
        assert [550, 430, 408, 289] == element['rect']
        assert 'proxy/ZmlpFaceRecognition_200x200.jpg' == element['proxy']
        assert 'zmlpFaceRecognition' == element['analysis']
        assert element['vector']

    @patch.object(ZmlpClient, 'upload_file')
    @patch.object(ProjectStorage, 'localize_file')
    def test_process_with_model(self, localize_file_patch, upload_patch):
        upload_patch.return_value = {
            "name": "ZmlpFaceRecognition_200x200.jpg",
            "category": "proxy",
            "mimetype": "image/jpeg",
            "attrs": {
                "width": 512,
                "height": 339
            }
        }
        localize_file_patch.return_value = os.path.dirname(__file__) + "/encodings.dat"

        test_faces_path = zorroa_test_data("images/face-recognition/face2.jpg")
        frame = Frame(TestAsset(test_faces_path))
        store_asset_proxy(frame.asset, test_faces_path, (512, 339))

        processor = self.init_processor(ZmlpFaceRecognitionProcessor())
        processor.process(frame)

        asset = frame.asset
        element = asset.get_attr('elements')[0]
        assert 'face' == element['type']
        assert ['Bob Dole'] == element['labels']
        assert [550, 430, 408, 289] == element['rect']
        assert 'proxy/ZmlpFaceRecognition_200x200.jpg' == element['proxy']
        assert 'zmlpFaceRecognition' == element['analysis']
        assert element['vector']
