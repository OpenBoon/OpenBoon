import logging

from unittest.mock import patch
from boonsdk import Model
from boonai_train.automl.labels import AutomlLabelDetectionSession
from boonflow import file_storage
from boonflow.testing import PluginUnitTestCase, TestAsset, test_path

logging.basicConfig(level=logging.INFO)


class AutomlLabelDetectionSessionTests(PluginUnitTestCase):

    def setUp(self):
        self.model = Model({
            'id': '00000000-0000-0000-0000-000000000000',
            'name': 'unittest-test'
        })

        base = 'model-export_icn_tflite-model_exportable_2-2021-05-28T21_55_41.973132Z_'
        label_file_name = '%sdict.txt' % base
        model_file_name = '%smodel.tflite' % base
        self.model_file = test_path(f"models/tflite/{model_file_name}")
        self.label_file = test_path(f"models/tflite/{label_file_name}")

    @patch("google.cloud.automl.AutoMlClient")
    @patch.object(AutomlLabelDetectionSession, '_create_automl_dataset')
    @patch('boonai_train.automl.labels.get_gcp_project_id')
    @patch("boonflow.cloud.get_google_storage_client")
    def test_create_automl_dataset(self, gs_patch, get_proj_patch, client_patch, automlclient):
        client_patch.return_value = MockAutoMlDataset()
        get_proj_patch.return_value = MockAutoMlClient()
        gs_patch.return_value = MockGsClient()
        automlclient.return_value = None

        session = AutomlLabelDetectionSession(self.model)
        dataset = session._create_automl_dataset()
        self.assertEquals("projects/123456/locations/us-central1/datasets/ICN123456789123456789",
                          dataset.name)

    @patch.object(AutomlLabelDetectionSession, '_create_automl_dataset')
    @patch("google.cloud.automl.AutoMlClient")
    @patch.object(AutomlLabelDetectionSession, '_store_labels_file')
    @patch('boonai_train.automl.labels.get_gcp_project_id')
    @patch("boonflow.cloud.get_google_storage_client")
    def test_import_labels_into_dataset(self,
                                        gs_patch,
                                        get_proj_patch,
                                        store_labels_patch,
                                        client_patch,
                                        dataset_patch):
        get_proj_patch.return_value = "boonai-dev"
        store_labels_patch.return_value = "gs://foo/bar/labels.csv"
        client_patch.return_value = MockAutoMlClient
        dataset_patch.return_value = MockAutoMlDataset()
        gs_patch.return_value = MockGsClient()

        session = AutomlLabelDetectionSession(self.model)
        dataset = session._create_automl_dataset()
        session._import_images_into_dataset(dataset)

    @patch("google.cloud.automl.AutoMlClient")
    @patch.object(file_storage.assets, 'get_native_uri')
    @patch('boonai_train.automl.labels.get_gcp_project_id')
    @patch("boonflow.cloud.get_google_storage_client")
    def test_get_image_uri(self, gs_patch, get_proj_patch, native_uri_patch, client_patch):
        client_patch.return_value = MockAutoMlClient
        gs_patch.return_value = MockGsClient()
        get_proj_patch.return_value = "boonai-dev"
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

    @patch("google.cloud.automl.AutoMlClient")
    @patch('boonai_train.automl.labels.get_gcp_project_id')
    @patch("boonflow.cloud.get_google_storage_client")
    def test_get_label(self, gs_patch, get_proj_patch, client_patch):
        gs_patch.return_value = MockGsClient()
        client_patch.return_value = MockAutoMlClient
        get_proj_patch.return_value = "boonai-dev"
        asset = TestAsset('flowers/daisy/5547758_eea9edfd54_n.jpg')
        asset.set_attr('labels', [
            {
                "modelId": self.model.id,
                "label": "cat"
            },
            {
                "modelId": "blahblah",
                "label": "dog"
            }
        ])

        session = AutomlLabelDetectionSession(self.model)
        label = session._get_label(asset)
        self.assertEquals('cat', label['label'])

    def test_store_labels_file(self):
        pass

    @patch("google.cloud.automl.AutoMlClient.export_model.result")
    @patch.object(AutomlLabelDetectionSession, 'emit_status')
    @patch('boonai_train.automl.labels.get_gcp_project_id')
    @patch("google.cloud.automl.AutoMlClient")
    @patch.object(file_storage.projects, 'get_directory_location')
    @patch("boonflow.cloud.get_google_storage_client")
    def test_export_model(self, gs_patch,
                          directory_loc_patch,
                          automl_patch,
                          project_id_patch,
                          emit_status_patch,
                          export_patch):
        gs_patch.return_value = MockGsClient()
        directory_loc_patch.return_value = 'cloud/location/directory/path'
        automl_patch.return_value = MockAutoMlClient()
        project_id_patch.return_value = "boonai-dev"
        emit_status_patch.return_value = None
        export_patch.return_value = None

        automl_model = MockAutoMlModel()
        mock_automl = AutomlLabelDetectionSession(self.model)
        mock_automl._export_model(automl_model)

    @patch.object(AutomlLabelDetectionSession, '_get_img_proxy_uri')
    @patch('boonai_train.automl.labels.get_gcp_project_id')
    @patch("google.cloud.automl.AutoMlClient")
    @patch("boonflow.cloud.get_google_storage_client")
    def test_move_asset_to_temp_bucket(self,
                                       gs_patch,
                                       automl_patch,
                                       project_id_patch,
                                       img_proxy_patch):
        gs_patch.return_value = MockGsClient()
        automl_patch.return_value = MockAutoMlClient()
        project_id_patch.return_value = "boonai-dev"
        img_proxy_patch.return_value = "gs://bucket/project/asset"

        test_asset = TestAsset()
        mock_automl = AutomlLabelDetectionSession(self.model,
                                                  training_bucket='gs://destination-bucket/')

        copied_url = mock_automl._copy_asset_to_temp_bucket(test_asset)

        assert copied_url == 'gs://Bucket/copied-value'

    @patch("boonsdk.app.model_app.ModelApp.upload_pretrained_model")
    @patch.object(file_storage, 'localize_file')
    @patch('google.cloud.storage.Client')
    @patch('boonai_train.automl.labels.get_gcp_project_id')
    @patch("google.cloud.automl.AutoMlClient")
    @patch("boonflow.cloud.get_google_storage_client")
    def test_upload_resources(self,
                              gs_patch,
                              automl_patch,
                              project_id_patch,
                              gs_client_patch,
                              file_storage_patch,
                              upload_trained_model_patch):
        resource_url = 'gs://zvi-dev-training/model-test/model/export/icn/exported/model.tflite'
        gs_patch.return_value = MockGsClient()
        automl_patch.return_value = MockAutoMlClient()
        gs_client_patch.return_value = MockGsClient()
        project_id_patch.return_value = "boonai-dev"
        file_storage_patch.side_effect = [self.model_file, self.label_file]
        upload_trained_model_patch.return_value = None

        mock_automl = AutomlLabelDetectionSession(self.model,
                                                  training_bucket='gs://destination-bucket/')

        mock_automl._upload_resources(resource_url)

    @patch('boonai_train.automl.labels.get_gcp_project_id')
    @patch("google.cloud.automl.AutoMlClient")
    @patch("boonflow.cloud.get_google_storage_client")
    def test_delete_train_resources(self,
                                    gs_patch,
                                    automl_patch,
                                    project_id_patch
                                    ):
        gs_patch.return_value = MockGsClient()
        automl_patch.return_value = MockAutoMlClient()
        project_id_patch.return_value = "boonai-dev"

        mock_automl = AutomlLabelDetectionSession(self.model,
                                                  training_bucket='gs://destination-bucket/')
        mock_automl.automl_dataset = MockAutoMlDataset()

        mock_automl._delete_train_resources()


class MockBucket:
    def __init__(self, name):
        self.name = name

    def blob(self, value):
        return MockBlob(value)

    def copy_blob(self, *args):
        return MockBlob('copied-value')


class MockAutoMlClient:

    def location_path(self, *args):
        return 'projects/boonai-dev/locations/us-central1'

    def create_dataset(self, *args):
        return Result()

    def import_data(self, *args):
        return MockImportDataResult()

    def export_model(self, request=None):
        return Result()

    def delete_model(self, *args):
        return Result()

    def delete_dataset(self, *args):
        return Result()


class MockAutoMlDataset:

    def __init__(self):
        self.name = "projects/123456/locations/us-central1/datasets/ICN123456789123456789"
        self.display_name = "dataset_name"
        self.example_count = 20


class MockAutoMlModel:

    def __init__(self):
        self.name = "projects/123456/locations/us-central1/models/ICN987654321"
        self.display_name = "model_exportable"
        self.dataset_id = "ICN123456789123456789"


class MockImportDataResult:
    def result(self):
        return self


class MockGsClient:

    def list_blobs(self, *args, **kwargs):
        base = 'model-export_icn_tflite-model_exportable_2-2021-05-28T21_55_41.973132Z_'
        label_file_name = '%sdict.txt' % base
        model_file_name = '%smodel.tflite' % base
        return [MockBlob(test_path(f"models/tflite/{model_file_name}")),
                MockBlob(test_path(f"models/tflite/{label_file_name}"))]

    def get_bucket(self, *args, **kwargs):
        return MockBucket('Bucket')


class MockBlob:
    def __init__(self, name):
        self.name = name


class Result:

    def result(self):
        return self

    @property
    def name(self):
        return "test"
