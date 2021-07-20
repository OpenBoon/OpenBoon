import os
import requests
from unittest.mock import patch, Mock
from boonai_analysis.disney.genome import (AbstractGenomeProcessor, GenomeFullFrameClassifier,
                                           GenomeObjectDetector, GenomeVfxAnimatedFaces)
from boonflow import Frame
from boonflow.analysis import LabelDetectionAnalysis
from boonflow.testing import PluginUnitTestCase, TestAsset, get_mock_stored_file, test_path
from boonsdk import BoonSdkException, BoonClient
from .mock_data import (VFX_ANIMATED_FACES_RESULT_DATA,
                        OBJECT_DETECTION_RESULT_DATA,
                        FULL_FRAME_CLASSIFIER_RESULT_DATA)

TEST_IMAGE = test_path('image/test_image.jpg')


class TestGenomeAbstractProcessor(PluginUnitTestCase):

    SUBMIT_TASK_RESPONSE = {'task': {'algorithm_id': 221, 'created_at': '2021-07-16T19:17:02', 'dataset_version': None, 'environment': 'g/default', 'hold_until': None, 'id': 3298822, 'input_params': '{"images": ["https://storage.googleapis.com/zvi-dev-archivist-data/projects/a3370a22-440e-43c3-85ad-fba64c7d5160/assets/ygmeTYdz7TGc-fETCiEC6EeG5enr8Ha5/web-proxy/web-proxy.jpg?GoogleAccessId=zmlp-archivist@zvi-dev.iam.gserviceaccount.com&Expires=1626466621&Signature=PfehWCze4tL5UiJMuKIcc0HVdN5LNhwzdMadRZHczF%2F4yisdOAvleJz5VnFp5ijBTg8d1GJi1n7BNmvPT9gKHSaotT2qtEIuFjqwzIXOjjXQqEQAqff%2B1q%2F53c3Kf1LPTIEZIMFR7IPSlB25DI%2FpFcgFZw%2BqUM5AG3rQJAA2iYAncudH6IDKZ4k2CKyzfwOtcji7SGhfCHPlJmDFgM%2BaBIShDPSJ37ZlAT1191lENr2H7zyzhD8TqmNOgRiylacIT8%2F5wYamFc9jfeSoBC0I%2B1WYOuMqYOzTPa732kEzIacAHnMcRDJufYEOzkJJjy8GyB5XPdYz2zbLiO0G%2F32ULg%3D%3D"]}', 'input_type': 'Image', 'loopback_id': '5ec836ab257b4562bfe17eb6762fc22a', 'media': 'Marvel', 'media_type': '', 'model_version': None, 'options': None, 'outputs': '{"Bucket": {"bucket": "automation-api-results-pre", "output_name": "zorroa/5ec836ab257b4562bfe17eb6762fc22a.json"}}', 'owner': 'zorroa', 'priority': 'MEDIUM', 'retry': None, 'status': 'queue', 'updated_at': '2021-07-16T19:17:02', 'use_gpu': True}}  #noqa
    CHECK_STATUS_RESPONSE_1 = {'task': {'algorithm_id': 221, 'created_at': '2021-07-16T19:17:02', 'dataset_version': None, 'environment': 'g/default', 'hold_until': None, 'id': 3298822, 'input_params': '{"images": ["https://storage.googleapis.com/zvi-dev-archivist-data/projects/a3370a22-440e-43c3-85ad-fba64c7d5160/assets/ygmeTYdz7TGc-fETCiEC6EeG5enr8Ha5/web-proxy/web-proxy.jpg?GoogleAccessId=zmlp-archivist@zvi-dev.iam.gserviceaccount.com&Expires=1626466621&Signature=PfehWCze4tL5UiJMuKIcc0HVdN5LNhwzdMadRZHczF%2F4yisdOAvleJz5VnFp5ijBTg8d1GJi1n7BNmvPT9gKHSaotT2qtEIuFjqwzIXOjjXQqEQAqff%2B1q%2F53c3Kf1LPTIEZIMFR7IPSlB25DI%2FpFcgFZw%2BqUM5AG3rQJAA2iYAncudH6IDKZ4k2CKyzfwOtcji7SGhfCHPlJmDFgM%2BaBIShDPSJ37ZlAT1191lENr2H7zyzhD8TqmNOgRiylacIT8%2F5wYamFc9jfeSoBC0I%2B1WYOuMqYOzTPa732kEzIacAHnMcRDJufYEOzkJJjy8GyB5XPdYz2zbLiO0G%2F32ULg%3D%3D"]}', 'input_type': 'Image', 'loopback_id': '5ec836ab257b4562bfe17eb6762fc22a', 'media': 'Marvel', 'media_type': '', 'model_version': None, 'options': None, 'outputs': '{"Bucket": {"bucket": "automation-api-results-pre", "output_name": "zorroa/5ec836ab257b4562bfe17eb6762fc22a.json"}}', 'owner': 'zorroa', 'priority': 'MEDIUM', 'retry': None, 'status': 'queue', 'updated_at': '2021-07-16T19:17:02', 'use_gpu': True}}  # noqa
    CHECK_STATUS_RESPONSE_2 = {'task': {'algorithm_id': 221, 'created_at': '2021-07-16T19:17:02', 'dataset_version': '2021-07-16T19:17:02', 'environment': 'g/default', 'hold_until': None, 'id': 3298822, 'input_params': '{"images": ["https://storage.googleapis.com/zvi-dev-archivist-data/projects/a3370a22-440e-43c3-85ad-fba64c7d5160/assets/ygmeTYdz7TGc-fETCiEC6EeG5enr8Ha5/web-proxy/web-proxy.jpg?GoogleAccessId=zmlp-archivist@zvi-dev.iam.gserviceaccount.com&Expires=1626466621&Signature=PfehWCze4tL5UiJMuKIcc0HVdN5LNhwzdMadRZHczF%2F4yisdOAvleJz5VnFp5ijBTg8d1GJi1n7BNmvPT9gKHSaotT2qtEIuFjqwzIXOjjXQqEQAqff%2B1q%2F53c3Kf1LPTIEZIMFR7IPSlB25DI%2FpFcgFZw%2BqUM5AG3rQJAA2iYAncudH6IDKZ4k2CKyzfwOtcji7SGhfCHPlJmDFgM%2BaBIShDPSJ37ZlAT1191lENr2H7zyzhD8TqmNOgRiylacIT8%2F5wYamFc9jfeSoBC0I%2B1WYOuMqYOzTPa732kEzIacAHnMcRDJufYEOzkJJjy8GyB5XPdYz2zbLiO0G%2F32ULg%3D%3D"]}', 'input_type': 'Image', 'loopback_id': '5ec836ab257b4562bfe17eb6762fc22a', 'media': 'Marvel', 'media_type': '', 'model_version': '2021-07-16T19:17:02', 'options': None, 'outputs': '{"Bucket": {"bucket": "automation-api-results-pre", "output_name": "zorroa/5ec836ab257b4562bfe17eb6762fc22a.json"}}', 'owner': 'zorroa', 'priority': 'MEDIUM', 'result': {'algorithm': 'FullFrameClassifier', 'calls': 0, 'detections': [{'end': None, 'instances': [{'label': 'sky or clouds from ground', 'prob': 0.8778038620948792, 'uri': 'http://data.disney.com/resources/invented/16d5037a-4974-4fa7-a05c-eaa2f754e856'}, {'label': 'locations tiles driving', 'prob': 0.8793227672576904, 'uri': 'http://data.disney.com/resources/invented/d9987286-9a96-4a3e-a84a-75d3b2afcee4'}], 'segment': 'https://storage.googleapis.com/zvi-dev-archivist-data/projects/a3370a22-440e-43c3-85ad-fba64c7d5160/assets/ygmeTYdz7TGc-fETCiEC6EeG5enr8Ha5/web-proxy/web-proxy.jpg?GoogleAccessId=zmlp-archivist@zvi-dev.iam.gserviceaccount.com&Expires=1626466621&Signature=PfehWCze4tL5UiJMuKIcc0HVdN5LNhwzdMadRZHczF%2F4yisdOAvleJz5VnFp5ijBTg8d1GJi1n7BNmvPT9gKHSaotT2qtEIuFjqwzIXOjjXQqEQAqff%2B1q%2F53c3Kf1LPTIEZIMFR7IPSlB25DI%2FpFcgFZw%2BqUM5AG3rQJAA2iYAncudH6IDKZ4k2CKyzfwOtcji7SGhfCHPlJmDFgM%2BaBIShDPSJ37ZlAT1191lENr2H7zyzhD8TqmNOgRiylacIT8%2F5wYamFc9jfeSoBC0I%2B1WYOuMqYOzTPa732kEzIacAHnMcRDJufYEOzkJJjy8GyB5XPdYz2zbLiO0G%2F32ULg%3D%3D', 'start': None}], 'media_type': '', 'model_version': '2020-06-30 23:00:20', 'task_id': 3298822, 'task_time': 7, 'type': 'Classifiers'}, 'result_url': 'https://s3.us-west-2.amazonaws.com/automation-api-results-pre/zorroa/5ec836ab257b4562bfe17eb6762fc22a.json?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=ASIA2DJQEPKOYUMKMAFR%2F20210716%2Fus-west-2%2Fs3%2Faws4_request&X-Amz-Date=20210716T193003Z&X-Amz-Expires=3600&X-Amz-SignedHeaders=host&X-Amz-Security-Token=IQoJb3JpZ2luX2VjEFMaCXVzLXdlc3QtMiJHMEUCIQCswu7I30nfQeBVauzTzSeuuYdVODiU8JrBLWUQ8ekmXQIgLeUFGlBgaUvyAdHMsB6mfBLjPWIQimdJo%2FBBLB%2BKGJoq%2BgMITBAAGgw2OTQyNzUwNDYwNDUiDIz4JVl0%2FPwS9kf4OCrXA6cCqbmrH%2BNNLnrdaxRMHHEsw0GC%2BwJGxVyHICrDZpP8Qng8j5qazM56MCepVVKYyaZotKy0OwxjBY%2Bt6%2FxJwRZV6wyUZxaz28Znv%2FCHEU0hQvYR%2BvMcvPHBPnNbFZpr9igbwOptp8Z8fIiyqKtbtSRyRFdAammv2M3ist82yX9rdL7eKMDVx0gdaTydDxK3MTEhU061DcdNWZuJq004j0YImqTLactAcwFkoMW1GVHnyt4BWPjfCMC7X6y5OwN9J4DtAV8k4c5KCGlhoVh2HN59L5KMIgaL18OpShDa50MD5dUzRxJzS%2Fr0F3%2BbKL8nEsiTgEBV2HN4EJlwSnbzphpkfsrVqXXpN3Ks7%2BQtV3ndq4%2BOtCh%2BFe%2BUgKgrVfdFDIfAbe6Xu8MiuBAJDyVh9U%2B74aestvfKVIH%2FN6emWrCs%2Fo%2F0qOnRca%2BJd5jQny4oGANMVkgZ0LrCr4Ej0uMUk88LQXMZZGUiEWmS0vApstbOEt9WuwlbSpiadRBfvNeFrsKQ0auQgq1t9QrVa3AgXtZMOVeUkhXcKDOt2aL%2FDI40D4Kq02tuLgpCQLKHc3tVC6O2J4WIWKHPlo%2FNn77fTgzI7VCnRUL%2FzHYbrLvbydGS70hWJt7gYjC8qMeHBjqlAVY0jBRZnvB2g%2BW7dyS8NOsWgR%2BAnmS92j85CUwzEdoktaARISWdu%2B3B1mOUuGpI9dUfg0q9VzzQ%2BEi7A4k7vFc%2BARYZEOXISWkR%2Fe2MSqY%2B7LGy6kvZAYfzrHnOOMwHkYesawxRw3hpN0BRKCrVGec6csIgldgO7EFAcaLzuq6ppoqybqFZW73WZiTdl4GsMWqiT8kYEJQUFRRuvOh93hA8kP2dKQ%3D%3D&X-Amz-Signature=1fbf148fff8c680537c77da14a480e27411e865ea0ad301d6fb9a4820fb06b19', 'retry': None, 'status': 'done', 'updated_at': '2021-07-16T19:17:02', 'use_gpu': True}}  #noqa

    @patch.object(AbstractGenomeProcessor, 'add_genome_analysis')
    @patch.object(AbstractGenomeProcessor, 'get_results_when_complete')
    @patch.object(AbstractGenomeProcessor, 'submit_genome_task')
    @patch('boonai_analysis.disney.genome.get_proxy_level')
    def test_process_flow(self, get_proxy_patch, submit_task_patch,
                          get_results_patch, add_analysis_patch):
        asset = TestAsset(TEST_IMAGE)
        frame = Frame(asset)
        _file = get_mock_stored_file()
        get_proxy_patch.return_value = _file
        submit_task_patch.return_value = {'task': {}}
        get_results_patch.return_value = {'result': {}}
        processor = self.init_processor(AbstractGenomeProcessor())

        processor.process(frame)
        submit_task_patch.assert_called_once_with(_file)
        get_results_patch.assert_called_once_with({'task': {}})
        add_analysis_patch.assert_called_once_with(asset, {'result': {}})

    @patch.object(AbstractGenomeProcessor, 'add_genome_analysis')
    @patch.object(requests, 'get')
    @patch.dict(os.environ, {'GENOME_USERNAME': 'username', 'GENOME_PASSWORD': 'password',
                             'GENOME_SLEEP_SECONDS': '0'})
    @patch.object(requests, 'post')
    @patch.object(BoonClient, 'get')
    @patch('boonai_analysis.disney.genome.get_proxy_level')
    def test_full_run(self, get_proxy_patch, client_get_patch, post_patch,
                      get_task_status_patch, add_analysis_patch):
        asset = TestAsset(TEST_IMAGE)
        frame = Frame(asset)
        _file = get_mock_stored_file()
        get_proxy_patch.return_value = _file
        # Signed URL response
        client_get_patch.return_value = {'uri': 'uri_path', 'mediaType': 'image/jpeg'}
        # Get Auth token
        post_auth_mock = Mock(ok=True, json=Mock(return_value={'token': 'token'}))
        # Submit Task to Genome
        submit_task_mock = Mock(ok=True, json=Mock(return_value=self.SUBMIT_TASK_RESPONSE))
        post_patch.side_effect = [post_auth_mock, submit_task_mock]
        # Check task status twice
        get_task_status_patch.side_effect = [
            Mock(ok=True, json=Mock(return_value=self.CHECK_STATUS_RESPONSE_1)),
            Mock(ok=True, json=Mock(return_value=self.CHECK_STATUS_RESPONSE_2))
        ]
        processor = self.init_processor(AbstractGenomeProcessor())
        processor.process(frame)

        add_analysis_patch.assert_called_once_with(asset, self.CHECK_STATUS_RESPONSE_2['task']['result']) # noqa

    @patch.dict(os.environ, {'GENOME_USERNAME': 'username', 'GENOME_PASSWORD': 'password'})
    @patch.object(requests, 'post')
    def test_auth_headers(self, post_patch):
        mock_response = Mock(ok=True, json=Mock(return_value={'token': 'token'}))
        post_patch.return_value = mock_response
        processor = self.init_processor(AbstractGenomeProcessor())

        headers = processor.auth_headers
        self.assertEqual(headers, {'Authorization': 'Bearer token'})

    @patch.object(requests, 'post')
    def test_auth_headers_no_username(self, post_patch):
        mock_response = Mock(ok=False)
        post_patch.return_value = mock_response
        processor = self.init_processor(AbstractGenomeProcessor())

        with self.assertRaises(BoonSdkException) as error:
            processor.auth_headers
        self.assertEqual(error.exception.args[0],
                         'GENOME_USERNAME unavailable. Please set in ENV variables.')

    @patch.dict(os.environ, {'GENOME_USERNAME': 'username'})
    @patch.object(requests, 'post')
    def test_auth_headers_no_password(self, post_patch):
        mock_response = Mock(ok=False)
        post_patch.return_value = mock_response
        processor = self.init_processor(AbstractGenomeProcessor())

        with self.assertRaises(BoonSdkException) as error:
            processor.auth_headers
        self.assertEqual(error.exception.args[0],
                         'GENOME_PASSWORD unavailable. Please set in ENV variables.')

    @patch.dict(os.environ, {'GENOME_USERNAME': 'username', 'GENOME_PASSWORD': 'password'})
    @patch.object(requests, 'post')
    def test_auth_headers_failed_response(self, post_patch):
        mock_response = Mock(ok=False)
        post_patch.return_value = mock_response
        processor = self.init_processor(AbstractGenomeProcessor())

        with self.assertRaises(BoonSdkException) as error:
            processor.auth_headers

        self.assertEqual(error.exception.args[0],
                         'Unable to get authorization token from Genome.')

    @patch.object(BoonClient, 'get')
    def test_submit_genome_task(self, client_patch):
        client_patch.return_value = {'uri': 'uri_path', 'mediaType': 'image/jpeg'}
        proxy = Mock(id='12345')
        processor = self.init_processor(AbstractGenomeProcessor())

        result = processor.get_proxy_signed_url(proxy)
        self.assertEqual(result, 'uri_path')


class TestGenomeProcessors(PluginUnitTestCase):

    @patch('boonai_analysis.disney.genome.get_proxy_level')
    def test_vfx_animated_faces(self, get_proxy_patch):
        asset = Mock()
        processor = self.init_processor(GenomeVfxAnimatedFaces())
        processor.add_genome_analysis(asset, VFX_ANIMATED_FACES_RESULT_DATA)
        predictions = [
            "Shuri",
            " Wright, Leticia",
            "Pepper Potts (Iron Man)",
            " Paltrow, Gwyneth",
            "Mantis",
            " Klementieff, Pom"
        ]

        analysis = asset.add_analysis.call_args[0][1]
        for prediction in analysis.predictions_list():
            self.assertIn(prediction.label, predictions)

    @patch('boonai_analysis.disney.genome.get_proxy_level')
    def test_object_detector(self, get_proxy_patch):
        asset = Mock()
        processor = self.init_processor(GenomeObjectDetector())
        processor.add_genome_analysis(asset, OBJECT_DETECTION_RESULT_DATA)
        predictions = [
            "Captain America's shield",
        ]

        analysis = asset.add_analysis.call_args[0][1]
        for prediction in analysis.predictions_list():
            self.assertIn(prediction.label, predictions)

    @patch('boonai_analysis.disney.genome.get_proxy_level')
    def test_full_frame_classifier(self, get_proxy_patch):
        asset = Mock()
        processor = self.init_processor(GenomeFullFrameClassifier())
        processor.add_genome_analysis(asset, FULL_FRAME_CLASSIFIER_RESULT_DATA)
        predictions = [
            'locations tiles driving',
            'sky or clouds from ground',
        ]

        analysis = asset.add_analysis.call_args[0][1]
        for prediction in analysis.predictions_list():
            self.assertIn(prediction.label, predictions)
