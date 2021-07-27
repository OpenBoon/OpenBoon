import os
import unittest
import tempfile
import yaml
import unittest.mock as mock

import server


class ServerTests(unittest.TestCase):

    spec = {
        'modelType': 'TORCH_MAR_CLASSIFIER',
        'modelId': 'd96733a8-de94-40c3-baef-92de50eb8398',
        'modelFile': 'https://storage.googleapis.com/zorroa-public/models/resnet-152-batch.mar',
        'image': 'gcr.io/zvi-dev/models/d96733a8-de94-40c3-baef-92de50eb8398'
    }

    def setUp(self):
        os.environ['GCLOUD_PROJECT'] = 'localdev'
        os.environ['TEMPLATE_PATH'] = os.path.dirname(__file__) + '/tmpl'

    def tearDown(self):
        del os.environ['GCLOUD_PROJECT']
        del os.environ['TEMPLATE_PATH']

    def test_generate_build_file_torch(self):
        build_dir = tempfile.mkdtemp()
        build_file = server.generate_build_file(self.spec, build_dir)

        with open(build_file, 'r') as fp:
            build = yaml.load(fp)

        assert build['images'][0] == 'gcr.io/zvi-dev/models/d96733a8-de94-40c3-baef-92de50eb8398'
        assert len(build['steps']) == 3

    def test_copy_template_boon_script(self):
        spec = {
            'modelType': 'BOON_FUNCTION',
            'modelId': 'd96733a8-de94-40c3-baef-92de50eb8398',
            'modelFile': 'https://storage.googleapis.com/zorroa-public/models/boonfunction.zip',
            'image': 'gcr.io/zvi-dev/models/d96733a8-de94-40c3-baef-92de50eb8398'
        }
        d = tempfile.mkdtemp() + "/build"
        server.copy_template(spec, d)
        assert os.path.exists(d + "/Dockerfile")

    def test_copy_template_torch(self):
        d = tempfile.mkdtemp() + "/build"
        server.copy_template(self.spec, d)
        assert os.path.exists(d + "/Dockerfile")

    @mock.patch('server.submit_build')
    def test_build_and_deploy_torch(self, submit_patch):
        assert server.build_and_deploy(self.spec)
        spec = submit_patch.call_args[0][0]
        assert spec == self.spec

    @mock.patch('subprocess.call')
    @mock.patch('server.get_image_tags')
    def test_delete_images(self, tag_patch, sub_patch):
        tag_patch.return_value = [{'digest': 'sha256:15994234bf60e28108b808d62174f6fd0b9e434beed5c656ce3c62b9204e593d', 'tags': ['latest'], 'timestamp': {'datetime': '2021-07-10 22:34:20-04:00', 'day': 10, 'fold': 0, 'hour': 22, 'microsecond': 0, 'minute': 34, 'month': 7, 'second': 20, 'year': 2021}}, {'digest': 'sha256:896e7fb11a1b4cceb3a185187554e07429eff5c43ac997824d223e90f11274a8', 'tags': [], 'timestamp': {'datetime': '2021-07-10 21:47:15-04:00', 'day': 10, 'fold': 0, 'hour': 21, 'microsecond': 0, 'minute': 47, 'month': 7, 'second': 15, 'year': 2021}}, {'digest': 'sha256:af47f990752e14fda934bd0b06e7bf95763e4edc4a5f6285f8f594a81b9e08d6', 'tags': [], 'timestamp': {'datetime': '2021-07-10 20:12:07-04:00', 'day': 10, 'fold': 0, 'hour': 20, 'microsecond': 0, 'minute': 12, 'month': 7, 'second': 7, 'year': 2021}}, {'digest': 'sha256:0f55a186dd75b28b0b6925fe140e93692ba09c235bdcee9cb7077b8070141906', 'tags': [], 'timestamp': {'datetime': '2021-07-10 17:10:33-04:00', 'day': 10, 'fold': 0, 'hour': 17, 'microsecond': 0, 'minute': 10, 'month': 7, 'second': 33, 'year': 2021}}]  # noqa
        server.delete_images({
            'image': 'gcr.io/zvi-dev/models/27b748d0-e0ce-11eb-bd13-5e813e2c5c9c'})
        assert sub_patch.call_count == 4
