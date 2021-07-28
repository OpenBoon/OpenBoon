import os
import shutil
from unittest.mock import patch

import pytest

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
    @patch.object(TorchModelArchiveClassifier, "predict")
    def test_image_classifier(self, predict_patch, proxy_patch, file_patch, model_patch):
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
        predict_patch.return_value = [
            ("daisy", 0.998),
            ("cat", 0.222)
        ]

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
        assert len(analysis['predictions']) == 2
        assert analysis['predictions'][0]['label'] == 'daisy'


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
    @patch.object(TorchModelArchiveDetector, "predict")
    def test_image_classifier(self, predict_patch, proxy_patch, file_patch, model_patch):
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
        predict_patch.return_value = [
            ("person", 0.998, [1.0, 1.0, 2.0, 2.0]),
            ("cat", 0.222, [2.0, 2.0, 3.0, 3.0])
        ]

        args = {
            "model_id": self.model_id,
            "tag": "latest",
            "endpoint": "http://127.0.0.1:8080"
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

    @patch.object(ModelApp, "get_model")
    @patch.object(file_storage.projects, "localize_file")
    @patch.object(TorchModelTextClassifier, "predict")
    def test_text_classifier_from_media_content(self, predict_patch, file_patch, model_patch):
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
        assert len(analysis['predictions']) == 1
        assert analysis['count'] == 1
        assert analysis['predictions'][0]['label'] == 'Business'
        assert analysis['predictions'][0]['score'] == 0.999

    @patch.object(ModelApp, "get_model")
    @patch.object(file_storage.projects, "localize_file")
    @patch.object(TorchModelTextClassifier, "predict")
    def test_text_classifier_from_custom_field(self, predict_patch, file_patch, model_patch):
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
        args = {
            "model_id": self.model_id,
            "tag": "latest",
            "endpoint": "http://127.0.0.1:8080",
            "model": self.torch_model_name,
            "text_content_field": "text.content"  # custom Field
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
    """
    Should have a Pythorch server deployed locally with text_classification model
    https://github.com/pytorch/serve/tree/master/examples/text_classification
    """

    model_id = "model-id-34568"
    name = "custom-label"
    torch_model_name = "my_tc"

    @patch.object(ModelApp, "get_model")
    @patch("boonflow.base.get_proxy_level_path")
    def test_text_classifier_asset(self, proxy_patch, model_patch):
        model_patch.return_value = Model(
            {
                "id": self.model_id,
                "type": "TORCH_MAR_CLASSIFIER",
                "fileId": "models/{}/foo/bar".format(self.model_id),
                "name": self.name,
                "moduleName": self.name
            }
        )

        text = 'Bloomberg has decided to publish a new report on global economic situation.'

        args = {
            "model_id": self.model_id,
            "tag": "latest",
            "endpoint": "http://127.0.0.1:8080",
            "model": self.torch_model_name,
            'text_content_field': 'text.content'
        }

        frame = Frame(TestAsset(attrs={"media.content": text, 'text.content': text}))

        processor = self.init_processor(
            TorchModelTextClassifier(), args
        )
        processor.process(frame)

        analysis = frame.asset.get_analysis(self.name)

        assert len(analysis['predictions']) == 1
        assert analysis['count'] == 1
        assert analysis['predictions'][0]['label'] == 'Business'
        assert analysis['predictions'][0]['score'] == 0.927


class TorchModelImageSegmenterTests(PluginUnitTestCase):
    model_id = "model-id-34568"
    name = "custom-label"
    torch_model_name = "deeplabv3"

    @patch.object(TorchModelImageSegmenter, "_segment_image")
    @patch.object(TorchModelImageSegmenter, "predict")
    @patch.object(file_storage.assets, 'store_file')
    @patch.object(ModelApp, "get_model")
    @patch("boonflow.base.get_proxy_level_path")
    def test_image_segmenter(self, proxy_patch, model_patch, file_storage_patch,
                             predict_patch, segment_image_patch):
        model_patch.return_value = Model(
            {
                "id": self.model_id,
                "type": "TORCH_MAR_IMAGE_SEGMENTER",
                "fileId": "models/{}/foo/bar".format(self.model_id),
                "name": self.name,
                "moduleName": self.name
            }
        )

        predict_patch.return_value = [['Unknown', '#000000'], ['Person', '#ff4400']]

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
            TorchModelImageSegmenter(), args
        )
        processor.process(frame)

        analysis = frame.asset.get_analysis(self.name)

        assert len(analysis['predictions']) == 2
        assert analysis['count'] == 2
        assert analysis['predictions'][0]['label'] == 'Unknown'
        assert analysis['predictions'][0]['kwargs']['color'] == '#000000'
        assert analysis['predictions'][1]['label'] == 'Person'
        assert analysis['predictions'][1]['kwargs']['color'] == '#ff4400'


@pytest.mark.skip(reason='dont run automatically')
class TorchModelImageSegmenterIntegrationTests(PluginUnitTestCase):
    """
    Should have a Pythorch server deployed locally with image_segmenter model
    https://github.com/pytorch/serve/tree/master/examples/image_segmenter/deeplabv3
    """

    model_id = "model-id-34568"
    name = "custom-label"
    torch_model_name = "deeplabv3"

    @patch.object(file_storage.assets, 'store_file')
    @patch.object(ModelApp, "get_model")
    @patch("boonflow.base.get_proxy_level_path")
    def test_image_segmenter(self, proxy_patch, model_patch, file_storage_patch):
        model_patch.return_value = Model(
            {
                "id": self.model_id,
                "type": "TORCH_MAR_IMAGE_SEGMENTER",
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
            TorchModelImageSegmenter(), args
        )
        processor.process(frame)

        analysis = frame.asset.get_analysis(self.name)

        assert len(analysis['predictions']) == 2
        assert analysis['count'] == 2
        assert analysis['predictions'][0]['label'] == 'Unknown'
        assert analysis['predictions'][0]['kwargs']['color'] == '#000000'
        assert analysis['predictions'][1]['label'] == 'Person'
        assert analysis['predictions'][1]['kwargs']['color'] == '#ff4400'

    @patch("boonflow.video.save_timeline")
    @patch.object(file_storage.assets, 'store_file')
    @patch.object(ModelApp, "get_model")
    @patch("boonflow.proxy.get_video_proxy")
    def test_video_segmenter(self, proxy_patch, model_patch,
                             file_storage_patch, save_timeline_patch):
        model_patch.return_value = Model(
            {
                "id": self.model_id,
                "type": "TORCH_MAR_IMAGE_SEGMENTER",
                "fileId": "models/{}/foo/bar".format(self.model_id),
                "name": self.name,
                "moduleName": self.name
            }
        )

        path = ''  # Video Path
        proxy_patch.return_value = path

        args = {
            "model_id": self.model_id,
            "tag": "latest",
            "endpoint": "http://127.0.0.1:8080",
            "model": self.torch_model_name
        }

        frame = Frame(TestAsset(path, attrs={'media.type': 'video', "media.length": 73}))

        processor = self.init_processor(
            TorchModelImageSegmenter(), args
        )
        processor.process(frame)

        analysis = frame.asset.get_analysis(self.name)

        assert analysis['count'] == 9
        assert analysis['predictions'][0]['label'] == 'Unknown'
        assert analysis['predictions'][1]['label'] == 'Aeroplane'
        assert analysis['predictions'][2]['label'] == 'Boat'
