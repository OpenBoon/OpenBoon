import shutil
from unittest.mock import patch

from boonsdk.app import ModelApp
from boonsdk.entity import Model
from boonai_analysis.custom import KnnFaceRecognitionClassifier
from boonflow import Frame, file_storage
from boonflow.testing import PluginUnitTestCase, TestAsset, get_prediction_labels, test_path


class KnnFaceRecognitionClassifierTests(PluginUnitTestCase):

    def setUp(self):
        try:
            shutil.rmtree("/tmp/boonai/model-cache")
        except FileNotFoundError:
            print("Didn't clear out model cache, this is ok.")

    @patch.object(ModelApp, 'get_model')
    @patch.object(file_storage.models, "install_model")
    def test_process(self, localize_patch, get_model_patch):
        localize_patch.return_value = test_path('training')
        get_model_patch.return_value = Model({
            'id': '12345',
            'modelId': '12345',
            'type': "FACE_RECOGNITION",
            'fileId': 'models/foo/bar/12345',
            'name': "foo",
            'moduleName': 'foo'
        })

        asset = TestAsset()
        asset.set_attr("analysis.boonai-face-detection.predictions", [
            {
                "label": "face0",
                "score": 0.1,
                "simhash": "AAAAAAAA",
                "bbox": [0, 0, 1, 1]
            },
            {
                "label": "face1",
                "score": 0.1,
                "simhash": "00000000",
                "bbox": [0, 0, 1, 1]
            }
        ])
        frame = Frame(asset)
        processor = self.init_processor(KnnFaceRecognitionClassifier(), {"sensitivity": 100})
        processor.process(frame)

        analysis = frame.asset.get_attr('analysis.foo')
        assert 'Gandalf' in get_prediction_labels(analysis)
        assert 'Unrecognized' in get_prediction_labels(analysis)

    @patch("boonai_analysis.custom.face_rec.video.save_timeline", return_value={})
    @patch.object(ModelApp, 'get_model')
    @patch.object(file_storage.models, "install_model")
    @patch('boonai_analysis.custom.face_rec.proxy.get_video_proxy')
    def test_process_video(self, proxy_path_patch, localize_patch, get_model_patch, _):
        proxy_path_patch.return_value = test_path("video/julia_roberts.mp4")
        localize_patch.return_value = test_path('models/face')
        get_model_patch.return_value = Model({
            'id': '12345',
            'modelId': '12345',
            'type': "FACE_RECOGNITION",
            'fileId': 'models/foo/bar/12345',
            'name': "foo",
            'moduleName': 'foo'
        })

        asset = TestAsset(proxy_path_patch.return_value)
        asset.set_attr('media.length', 648)
        asset.set_attr('media.type', 'video')
        frame = Frame(asset)
        processor = self.init_processor(KnnFaceRecognitionClassifier())
        processor.process(frame)

        analysis = frame.asset.get_attr('analysis.foo')
        assert 'Julia Roberts' in get_prediction_labels(analysis)

        # No bboxes on video analysis.
        for p in analysis['predictions']:
            assert not p.get('bbox')
            if p.get('label') == 'Julia Roberts':
                assert p.get('occurrences')
                assert p.get('score')
                assert p.get('label')
            else:
                assert p.get('occurrences')
                assert not p.get('score')
                assert p.get('label') == 'Unrecognized'
