# import os
# import shutil
# import zipfile
import pytest
from pytest import approx
from unittest.mock import patch

from tensorflow.keras.models import load_model

from boonai_train.tf2 import predict
from boonflow.testing import PluginUnitTestCase, test_path


@pytest.mark.skip(reason='dont run automatically')
class PredictTests(PluginUnitTestCase):

    @patch.object(predict, 'load_trained_model')
    def test_predict(self, train_patch):
        # install_path = test_path('training')
        # model_zip = test_path(
        #     'training/custom-flowers-label-detection-tf2-xfer-mobilenet2.zip'
        # )
        #
        # # extract all files
        # with zipfile.ZipFile(model_zip) as z:
        #     z.extractall(path=install_path)

        model_path = test_path('training/custom-flowers-label-detection-tf2-xfer-mobilenet2')
        train_patch.return_value = (load_model(model_path), ['daisy', 'roses'])
        test_img = test_path('training/test_dsy.jpg')

        trained_model, labels = predict.load_trained_model()
        predictions = predict.predict(path=test_img, trained_model=trained_model, labels=labels)

        daisy = predictions[0]
        assert daisy[0] == 'daisy'
        assert approx(daisy[1], 0.4, 0.1)

        roses = predictions[1]
        assert roses[0] == 'roses'
        assert approx(roses[1], 0.5, 0.1)

        # # remove unzipped model path
        # shutil.rmtree(model_path)
        # shutil.rmtree(test_path('training/assets'))
        # shutil.rmtree(test_path('training/variables'))
        # os.remove(test_path('training/labels.txt'))
        # os.remove(test_path('training/saved_model.pb'))
