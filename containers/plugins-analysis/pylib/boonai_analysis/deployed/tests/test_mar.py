import os
import shutil
from unittest.mock import patch

import pytest
import json

from boonsdk.app import ModelApp
from boonsdk.entity import Model
from boonflow.base import Frame, ImageInputStream
from boonflow.storage import file_storage
from boonflow.testing import PluginUnitTestCase, TestAsset, test_path

from boonai_analysis.deployed.mar import TorchModelArchiveClassifier, TorchModelArchiveDetector, \
    TorchModelTextClassifier, TorchModelImageSegmenter


class TorchModelArchiveTests(PluginUnitTestCase):
    model_id = "model-id-34568"
    base_dir = os.path.dirname(__file__)

    def setUp(self):
        try:
            shutil.rmtree("/tmp/boonai/model-cache")
        except FileNotFoundError:
            print("Didn't clear out model cache, this is ok.")

    @patch.object(ModelApp, "get_model")
    @patch.object(file_storage.projects, "localize_file")
    @patch("boonflow.base.get_proxy_level_path")
    @patch("boonai_analysis.deployed.mar.make_request")
    def test_image_classifier(self, make_request_patch, proxy_patch, file_patch, model_patch):
        name = "custom-flowers-label-detection-tf2-xfer-mobilenet2"
        model_patch.return_value = Model(
            {
                "id": self.model_id,
                "type": "TORCH_MAR_CLASSIFIER",
                "fileId": "models/{}/foo/bar".format(self.model_id),
                "name": name,
                "moduleName": name
            }
        )
        make_request_patch.return_value = {
            'toucan': 0.9993595480918884,
            'hornbill': 0.0003122082562185824,
            'anemone_fish': 0.00015631545102223754,
            'king_penguin': 6.56733027426526e-05,
            'macaw': 4.006339440820739e-05
        }

        args = {
            "model_id": self.model_id,
            "tag": "latest",
            "endpoint": "http://127.0.0.1:8080"
        }

        path = test_path("training/test_dsy.jpg")
        proxy_patch.return_value = path
        frame = Frame(TestAsset(path))

        processor = self.init_processor(
            TorchModelArchiveClassifier(), args
        )
        processor.process(frame)
        analysis = frame.asset.get_analysis(name)
        assert len(analysis['predictions']) == 1
        assert analysis['predictions'][0]['label'] == 'toucan'


@pytest.mark.skip(reason='dont run automatically')
class TorchModelArchiveIntegrationTests(PluginUnitTestCase):
    model_id = "model-id-34568"

    @patch.object(ModelApp, "get_model")
    def test_image_classifier_frame_image(self, model_patch):
        """
        Should have a resnet152 server deployed locally.
        https://github.com/pytorch/serve/tree/master/examples/image_classifier/resnet_152_batch

        """
        name = "custom-flowers-label-detection-tf2-xfer-mobilenet2"
        model_patch.return_value = Model(
            {
                "id": self.model_id,
                "type": "TORCH_MAR_CLASSIFIER",
                "fileId": "models/{}/foo/bar".format(self.model_id),
                "name": name,
                "moduleName": name
            }
        )

        args = {
            "model_id": self.model_id,
            "tag": "latest",
            "endpoint": "http://127.0.0.1:8080",
            "endpoint_path": "/predictions/resnet152",
            "model": "resnet152"
        }

        frame = Frame(TestAsset())
        path = test_path("images/set01/toucan.jpg")
        frame.image = ImageInputStream.from_path(path)

        processor = self.init_processor(
            TorchModelArchiveClassifier(), args
        )
        processor.process(frame)
        analysis = frame.asset.get_analysis(name)

        assert len(analysis['predictions']) == 1
        assert analysis['predictions'][0]['label'] == 'toucan'

    @patch.object(ModelApp, "get_model")
    @patch("boonflow.base.get_proxy_level_path")
    def test_image_classifier_asset(self, proxy_patch, model_patch):
        """
        Should have a resnet152 server deployed locally.
        https://github.com/pytorch/serve/tree/master/examples/image_classifier/resnet_152_batch

        """
        name = "custom-flowers-label-detection-tf2-xfer-mobilenet2"
        model_patch.return_value = Model(
            {
                "id": self.model_id,
                "type": "TORCH_MAR_CLASSIFIER",
                "fileId": "models/{}/foo/bar".format(self.model_id),
                "name": name,
                "moduleName": name
            }
        )
        path = test_path("images/set01/toucan.jpg")
        proxy_patch.return_value = path

        args = {
            "model_id": self.model_id,
            "tag": "latest",
            "endpoint": "http://127.0.0.1:8080",
            "model": "resnet152"
        }

        frame = Frame(TestAsset(path))

        processor = self.init_processor(
            TorchModelArchiveClassifier(), args
        )
        processor.process(frame)

        analysis = frame.asset.get_analysis(name)

        assert len(analysis['predictions']) == 1
        assert analysis['predictions'][0]['label'] == 'toucan'


class TorchModelArchiveDetectorTests(PluginUnitTestCase):
    model_id = "model-id-34568"
    torch_model_name = "maskrcnn"
    base_dir = os.path.dirname(__file__)

    def setUp(self):
        try:
            shutil.rmtree("/tmp/boonai/model-cache")
        except FileNotFoundError:
            print("Didn't clear out model cache, this is ok.")

    @patch.object(ModelApp, "get_model")
    @patch.object(file_storage.projects, "localize_file")
    @patch("boonflow.base.get_proxy_level_path")
    @patch("boonai_analysis.deployed.mar.make_request")
    def test_detector(self, make_request_patch, proxy_patch, file_patch, model_patch):
        name = "custom-object-detection"
        model_patch.return_value = Model(
            {
                "id": self.model_id,
                "type": "TORCH_MAR_DETECTOR",
                "fileId": "models/{}/foo/bar".format(self.model_id),
                "name": name,
                "moduleName": name
            }
        )
        make_request_patch.return_value = [
            {'person': [104.5685806274414, 3.214049816131592, 320.3063049316406, 339.0],
             'score': 0.9994246959686279},
            {'person': [258.7762451171875, 0.0, 438.7398681640625, 329.4570617675781],
             'score': 0.9975128173828125}
        ]

        args = {
            "model_id": self.model_id,
            "tag": "latest",
            "endpoint": "http://127.0.0.1:8080",
            "endpoint_path": "/predictions/maskrcnn",
        }

        path = test_path("images/set01/faces.jpg")
        proxy_patch.return_value = path
        frame = Frame(TestAsset(path))

        processor = self.init_processor(
            TorchModelArchiveDetector(), args
        )
        processor.process(frame)
        analysis = frame.asset.get_analysis(name)
        assert len(analysis['predictions']) == 2
        assert analysis['predictions'][0]['label'] == 'person'


@pytest.mark.skip(reason='dont run automatically')
class TorchModelArchiveDetectorIntegrationTests(PluginUnitTestCase):
    """
    Should have a Pythorch server deployed locally with maskrcnn model
    https://github.com/pytorch/serve/tree/master/examples/object_detector/maskrcnn
    After building maskrcnn.mar and moving into model_store
    Run: $ curl-X POST "localhost:8081/models?model_name=maskrcnn&url=maskrcnn.mar&
    batch_size=4&max_batch_delay=5000&initial_workers=3&synchronous=true"
    to sign the model in the server
    """

    model_id = "model-id-34568"
    name = "custom-label"
    torch_model_name = "maskrcnn"

    @patch.object(ModelApp, "get_model")
    def test_object_detection_frame_image(self, model_patch):
        model_patch.return_value = Model(
            {
                "id": self.model_id,
                "type": "TORCH_MAR_DETECTOR",
                "fileId": "models/{}/foo/bar".format(self.model_id),
                "name": self.name,
                "moduleName": self.name
            }
        )

        args = {
            "model_id": self.model_id,
            "tag": "latest",
            "endpoint": "http://127.0.0.1:8080",
            "endpoint_path": "/predictions/maskrcnn",
            "model": self.torch_model_name
        }

        frame = Frame(TestAsset())
        path = test_path("images/set01/faces.jpg")
        frame.image = ImageInputStream.from_path(path)

        processor = self.init_processor(
            TorchModelArchiveDetector(), args
        )
        processor.process(frame)
        analysis = frame.asset.get_analysis(self.name)

        assert len(analysis['predictions']) == 2
        assert analysis['predictions'][0]['label'] == 'person'

    @patch.object(ModelApp, "get_model")
    @patch("boonflow.base.get_proxy_level_path")
    def test_object_detection_asset(self, proxy_patch, model_patch):
        model_patch.return_value = Model(
            {
                "id": self.model_id,
                "type": "TORCH_MAR_DETECTOR",
                "fileId": "models/{}/foo/bar".format(self.model_id),
                "name": self.name,
                "moduleName": self.name
            }
        )
        path = test_path("images/set01/faces.jpg")
        proxy_patch.return_value = path

        args = {
            "model_id": self.model_id,
            "tag": "latest",
            "endpoint": "http://127.0.0.1:8080",
            "model": self.torch_model_name
        }

        frame = Frame(TestAsset(path))

        processor = self.init_processor(
            TorchModelArchiveDetector(), args
        )
        processor.process(frame)

        analysis = frame.asset.get_analysis(self.name)

        assert len(analysis['predictions']) == 2
        assert analysis['predictions'][0]['label'] == 'person'
        assert analysis['predictions'][0]['score'] == 0.999

    @patch.object(ModelApp, "get_model")
    @patch("boonflow.video.save_timeline")
    @patch("boonflow.proxy.get_video_proxy")
    def test_object_detection_video(self, proxy_patch, save_video_patch, model_patch):
        model_patch.return_value = Model(
            {
                "id": self.model_id,
                "type": "TORCH_MAR_DETECTOR",
                "fileId": "models/{}/foo/bar".format(self.model_id),
                "name": self.name,
                "moduleName": self.name
            }
        )
        path = ''  # PATH TO A VIDEO FILE

        proxy_patch.return_value = path

        args = {
            "model_id": self.model_id,
            "tag": "latest",
            "endpoint": "http://127.0.0.1:8080",
            "model": self.torch_model_name
        }

        frame = Frame(TestAsset(path, attrs={"media.type": "video", "media.length": 73}))

        processor = self.init_processor(
            TorchModelArchiveDetector(), args
        )
        processor.process(frame)

        analysis = frame.asset.get_analysis(self.name)

        assert len(analysis['predictions']) > 0
        assert analysis['count'] > 0


class TorchModelArchiveTextClassificationTests(PluginUnitTestCase):
    model_id = "model-id-34568"
    name = "custom-label"
    torch_model_name = "my_tc"

    @patch.object(ModelApp, 'get_training_args')
    @patch.object(ModelApp, "get_model")
    @patch.object(file_storage.projects, "localize_file")
    @patch("boonai_analysis.deployed.mar.make_request")
    def test_text_classifier_from_default(self, make_request_patch, _, model_patch, args_patch):
        model_patch.return_value = Model(
            {
                "id": self.model_id,
                "type": "TORCH_MAR_TEXT_CLASSIFIER",
                "fileId": "models/{}/foo/bar".format(self.model_id),
                "name": self.name,
                "moduleName": self.name
            }
        )
        make_request_patch.return_value = {'World': 0.010638430714607239,
                                           'Sports': 4.3774482037406415e-05,
                                           'Business': 0.44048723578453064,
                                           'Sci/Tec': 0.548830509185791}
        args_patch.return_value = {}

        args = {
            "model_id": self.model_id,
            "tag": "latest",
            "endpoint": "http://127.0.0.1:8080",
            "model": self.torch_model_name
        }

        text = 'Bloomberg has decided to publish a new report on global economic situation.'

        frame = Frame(TestAsset(attrs={"media.content": text}))

        processor = self.init_processor(
            TorchModelTextClassifier(), args
        )
        processor.process(frame)

        analysis = frame.asset.get_analysis(self.name)
        assert len(analysis['predictions']) == 2
        assert analysis['count'] == 2
        assert analysis['predictions'][0]['label'] == 'Sci/Tec'
        assert analysis['predictions'][0]['score'] == 0.549

    @patch.object(ModelApp, 'get_training_args')
    @patch.object(ModelApp, "get_model")
    @patch.object(file_storage.projects, "localize_file")
    @patch.object(TorchModelTextClassifier, "predict")  # fix
    def test_text_classifier_from_custom_field(self, predict_patch, _, model_patch, args_patch):
        model_patch.return_value = Model(
            {
                "id": self.model_id,
                "type": "TORCH_MAR_TEXT_CLASSIFIER",
                "fileId": "models/{}/foo/bar".format(self.model_id),
                "name": self.name,
                "moduleName": self.name
            }
        )
        predict_patch.return_value = [
            ("Business", 0.999)
        ]
        args_patch.return_value = {"field": "text.content"}

        args = {
            "model_id": self.model_id,
            "tag": "latest",
            "endpoint": "http://127.0.0.1:8080",
            "model": self.torch_model_name
        }

        text = 'Bloomberg has decided to publish a new report on global economic situation.'

        frame = Frame(TestAsset(attrs={"text.content": text}))

        processor = self.init_processor(
            TorchModelTextClassifier(), args
        )
        processor.process(frame)

        analysis = frame.asset.get_analysis(self.name)
        assert len(analysis['predictions']) == 1
        assert analysis['count'] == 1
        assert analysis['predictions'][0]['label'] == 'Business'
        assert analysis['predictions'][0]['score'] == 0.999


@pytest.mark.skip(reason='dont run automatically')
class TorchModelArchiveTextClassificationIntegrationTests(PluginUnitTestCase):
    model_id = "model-id-34568"
    name = "custom-label"
    torch_model_name = "my_tc"

    @patch.object(ModelApp, 'get_training_args')
    @patch.object(ModelApp, "get_model")
    def test_text_classifier(self, model_patch, args_patch):
        model_patch.return_value = Model(
            {
                "id": self.model_id,
                "type": "TORCH_MAR_TEXT_CLASSIFIER",
                "fileId": "models/{}/foo/bar".format(self.model_id),
                "name": self.name,
                "moduleName": self.name
            }
        )

        args_patch.return_value = {}

        args = {
            "model_id": self.model_id,
            "tag": "latest",
            "endpoint": "http://127.0.0.1:8080",
            "endpoint_path": "/predictions/my_tc",
            "model": self.torch_model_name
        }

        text = 'Bloomberg has decided to publish a new report on global economic situation.'

        frame = Frame(TestAsset(attrs={"media.content": text}))

        processor = self.init_processor(
            TorchModelTextClassifier(), args
        )
        processor.process(frame)

        analysis = frame.asset.get_analysis(self.name)
        assert len(analysis['predictions']) == 2
        assert analysis['count'] == 2
        assert analysis['predictions'][0]['label'] == 'Sci/Tec'
        assert analysis['predictions'][0]['score'] == 0.549


class TorchModelImageSegmentTests(PluginUnitTestCase):
    """
    To turn these into an integration tests, you must have a torch serve instance
    running with the deep lab v3 model.  Additionally, remove the mock for
    the call to make_torch_serve_request which will allow the test to talk to Torch Server.

    https://github.com/pytorch/serve/tree/master/examples/image_segmenter/deeplabv3
    """

    model_id = "model-id-34568"
    name = "custom-label"
    torch_model_name = "deeplabv3"

    def load_response(self):
        with open(os.path.dirname(__file__) + '/img_seg.json', 'r') as fp:
            return json.load(fp)

    @patch.object(TorchModelImageSegmenter, 'make_torch_serve_request')
    @patch.object(ModelApp, 'get_training_args')
    @patch.object(file_storage.assets, 'store_file')
    @patch.object(ModelApp, 'get_model')
    @patch('boonflow.base.get_proxy_level_path')
    def test_image_segmenter_no_labels(self, proxy_patch, model_patch, _, args_patch, req_patch):
        model_patch.return_value = Model(
            {
                'id': self.model_id,
                'type': 'TORCH_MAR_IMAGE_SEGMENTER',
                'fileId': 'models/{}/foo/bar'.format(self.model_id),
                'name': self.name,
                'moduleName': self.name
            }
        )
        path = test_path('images/set01/faces.jpg')
        proxy_patch.return_value = path
        req_patch.return_value = self.load_response()
        args_patch.return_value = {}

        args = {
            'model_id': self.model_id,
            'tag': 'latest',
            'endpoint': 'http://127.0.0.1:8080',
            'model': self.torch_model_name
        }

        frame = Frame(TestAsset(path))

        processor = self.init_processor(
            TorchModelImageSegmenter(), args
        )
        processor.process(frame)

        analysis = frame.asset.get_analysis(self.name)

        assert len(analysis['predictions']) == 2
        assert analysis['count'] == 2
        assert analysis['predictions'][0]['label'] == '0'
        assert analysis['predictions'][0]['color'] == '#000000'
        assert analysis['predictions'][1]['label'] == '15'
        assert analysis['predictions'][1]['color'] == '#ee799f'

    @patch.object(TorchModelImageSegmenter, 'make_torch_serve_request')
    @patch.object(ModelApp, 'get_training_args')
    @patch.object(file_storage.assets, 'store_file')
    @patch.object(ModelApp, 'get_model')
    @patch('boonflow.base.get_proxy_level_path')
    def test_image_segmenter_with_labels(
            self, proxy_patch, model_patch, _, train_arg_patch, req_patch):
        model_patch.return_value = Model(
            {
                'id': self.model_id,
                'type': 'TORCH_MAR_IMAGE_SEGMENTER',
                'fileId': 'models/{}/foo/bar'.format(self.model_id),
                'name': self.name,
                'moduleName': self.name
            }
        )
        path = test_path("images/set01/faces.jpg")
        proxy_patch.return_value = path
        req_patch.return_value = self.load_response()
        train_arg_patch.return_value = {
            'labels': [
                'Unknown',
                'Aeroplane',
                'Bicycle',
                'Bird',
                'Boat',
                'Bottle',
                'Bus',
                'Car',
                'Cat',
                'Chair',
                'Cow',
                'Dining table',
                'Dog',
                'Horse',
                'Motorbike',
                'Person',
                'Potted plant',
                'Sheep',
                'Sofa',
                'Train',
                'Tv/Monitor'
            ]
        }

        args = {
            "model_id": self.model_id,
            "tag": "latest",
            "endpoint": "http://127.0.0.1:8080",
            "model": self.torch_model_name
        }

        frame = Frame(TestAsset(path))
        processor = self.init_processor(
            TorchModelImageSegmenter(), args
        )
        processor.process(frame)

        analysis = frame.asset.get_analysis(self.name)

        assert len(analysis['predictions']) == 2
        assert analysis['count'] == 2
        assert analysis['predictions'][0]['label'] == 'Unknown'
        assert analysis['predictions'][0]['color'] == '#000000'
        assert analysis['predictions'][1]['label'] == 'Person'
        assert analysis['predictions'][1]['color'] == '#ee799f'
