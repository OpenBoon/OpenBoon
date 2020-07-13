import logging
from unittest.mock import patch

from google.cloud.automl import AutoMlClient

from zmlp import Model
from zmlp_train.automl.labels import AutomlLabelDetectionSession
from zmlpsdk.testing import PluginUnitTestCase, TestAsset
from zmlpsdk import file_storage

logging.basicConfig(level=logging.INFO)


class AutomlLabelDetectionSessionTests(PluginUnitTestCase):

    def setUp(self):
        self.model = Model({
            'id': '00000000-0000-0000-0000-000000000000',
            'name': 'unittest-test'
        })

    @patch('zmlp_train.automl.labels.get_gcp_project_id')
    def test_create_instance(self, get_proj_patch):
        get_proj_patch.return_value = "zvi-dev"

        dsimp = AutomlLabelDetectionSession(self.model)
        self.assertEquals('projects/zvi-dev/locations/us-central1', dsimp.project_location)

    @patch.object(AutoMlClient, 'create_dataset')
    @patch('zmlp_train.automl.labels.get_gcp_project_id')
    def test_create_automl_dataset(self, get_proj_patch, create_dataset_patch):
        get_proj_patch.return_value = "zvi-dev"
        create_dataset_patch.return_value = MockCreateDataSetResult()

        session = AutomlLabelDetectionSession(self.model)
        dataset = session._create_automl_dataset()
        self.assertEquals("test", dataset.name)

    @patch.object(AutomlLabelDetectionSession, '_store_labels_file')
    @patch.object(AutoMlClient, 'import_data')
    @patch.object(AutoMlClient, 'create_dataset')
    @patch('zmlp_train.automl.labels.get_gcp_project_id')
    def test_import_labels_into_dataset(self,
                                        get_proj_patch,
                                        create_dataset_patch,
                                        import_data_patch,
                                        store_labels_patch):

        get_proj_patch.return_value = "zvi-dev"
        create_dataset_patch.return_value = MockCreateDataSetResult()
        import_data_patch.return_value = MockImportDataResult()
        store_labels_patch.return_value = "gs://foo/bar/labels.csv"

        session = AutomlLabelDetectionSession(self.model)
        dataset = session._create_automl_dataset()
        session._import_images_into_dataset(dataset)

    @patch.object(file_storage.assets, 'get_native_uri')
    @patch('zmlp_train.automl.labels.get_gcp_project_id')
    def test_get_image_uri(self, get_proj_patch, native_uri_patch):
        get_proj_patch.return_value = "zvi-dev"
        native_uri_patch.return_value = "gs://foo/bar"
        asset = TestAsset('flowers/daisy/5547758_eea9edfd54_n.jpg')
        asset.set_attr('files', [
            {
                "attrs": {
                    "width": 10,
                    "height": 10
                },
                "mimetype": "image/jpeg",
                "category": "proxy"
            }
        ])

        session = AutomlLabelDetectionSession(self.model)
        uri = session._get_img_proxy_uri(asset)
        self.assertEquals("gs://foo/bar", uri)

    @patch('zmlp_train.automl.labels.get_gcp_project_id')
    def test_get_label(self, get_proj_patch):
        get_proj_patch.return_value = "zvi-dev"
        asset = TestAsset('flowers/daisy/5547758_eea9edfd54_n.jpg')
        asset.set_attr('labels', [
            {
                "modelId": self.model.id,
                "label": "cat"
            },
            {
                "modelId":  "blahblah",
                "label": "dog"
            }
        ])

        session = AutomlLabelDetectionSession(self.model)
        label = session._get_label(asset)
        self.assertEquals('cat', label['label'])

    def test_store_labels_file(self):
        pass


class MockImportDataResult:
    def result(self):
        return self


class MockCreateDataSetResult:

    def result(self):
        return self

    @property
    def name(self):
        return "test"
