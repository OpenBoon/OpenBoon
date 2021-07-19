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
