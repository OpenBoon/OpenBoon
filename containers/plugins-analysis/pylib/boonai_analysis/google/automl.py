from numpy import argsort
from cv2 import imread, resize
from urllib.parse import urlparse

from boonsdk import app_from_env
from boonflow import Argument, AssetProcessor
from boonflow.proxy import get_proxy_level_path
from boonflow.analysis import LabelDetectionAnalysis
from boonflow.cloud import get_cached_google_storage_client
from boonflow import file_storage

import tensorflow as tf
import os
import zipfile
import tempfile
import shutil


class AutoMLModelClassifier(AssetProcessor):
    """Classifier for trained AutoML model """

    def __init__(self):
        super(AutoMLModelClassifier, self).__init__()
        self.add_arg(Argument("model_id", "str", required=True, toolTip="The model Id"))

        self.app = None
        self.app_model = None
        self.predictions = []
        self.analysis = None
        self.storage_client = get_cached_google_storage_client()

        self.model_file = None
        self.label_file = None
        self.labels = None
        self.display_name = None

        self.input_details = None
        self.output_details = None

    def init(self):
        """Init constructor """
        self.app_model = self.app.models.get_model(self.arg_value("model_id"))
        self.display_name = self.app_model.id.replace("-", "")
        self.app = app_from_env()

        self.__download_model()
        self.__load_interpreters()

    def __download_model(self):

        tmp = tempfile.mkdtemp()
        tmp_file = os.path.join(tmp, "model.zip")
        self.app.models.export_trained_model(self.app_model, tmp_file)

        self._unzip_model_files(tmp, tmp_file)

        self.model_file = os.path.join(tmp, "model.tflite")
        self.label_file = os.path.join(tmp, "labels.txt")

    def _unzip_model_files(self, model_path, file_name):

        """
        Receives a temp zip file and extract it to a certain folder
        The Zip file is deleted at the end of the process
        :param model_path: where the models will be extracted to
        :param file_name:  zip file name
        :return:
        """

        os.chdir(model_path)
        zip_ref = zipfile.ZipFile(file_name)

        # extract to the model path
        tmp_dir = tempfile.mkdtemp()
        zip_ref.extractall(tmp_dir)

        # copying only files
        for root, dirs, files in os.walk(tmp_dir):
            for file in files:
                path_file = os.path.join(root, file)
                shutil.copyfile(path_file, "{}/{}".format(model_path, file))

        zip_ref.close()
        os.remove(file_name)

    def process(self, frame):
        """Process the given frame for predicting and adding labels to an asset

        Args:
            frame (Frame): Frame to be processed

        Returns:
            None
        """
        asset = frame.asset
        self.analysis = LabelDetectionAnalysis(min_score=0.01)

        proxy_path = get_proxy_level_path(asset, 0)
        self.predict(proxy_path)

        for result in self.predictions:
            self.analysis.add_label_and_score(result['label'], result['score'])

        asset.add_analysis(self.app_model.name, self.analysis)

    def predict(self, path):
        """ Make a prediction for an image path

        Args:
            path (str): image path

        Returns:
            None
        """

        img = imread(r"{}".format(path))
        new_img = resize(img, (224, 224))
        self.interpreter.set_tensor(self.input_details[0]['index'], [new_img])
        self.interpreter.invoke()

        output_data = self.interpreter.get_tensor(self.output_details[0]['index'])
        arg_sorted = (argsort(output_data))
        index = arg_sorted[0][-1]

        label = self.labels[index]
        score = output_data[0][index]
        score = self.__normalize_tf_score(score)

        self.predictions.append({
            'label': label,
            'score': score
        })

    def __normalize_tf_score(self, score):
        """
        Normalize the result of a tf model prediction that is between 0-1000
        :param score:
        :return:
        """
        return score / 1000 if score else 0

    def __load_interpreters(self):
        """
        Prepare model tensors and load labels array from file
        :return:
        """

        if not self.model_file or not self.label_file:
            raise FileNotFoundError("Model files not found")

        with open(self.label_file, "r") as infile:
            self.labels = infile.read().splitlines()

        # Load the TFLite model and allocate tensors.
        self.interpreter = tf.lite.Interpreter(model_path=self.model_file)
        self.interpreter.allocate_tensors()

        # Get input and output tensors.
        self.input_details = self.interpreter.get_input_details()
        self.output_details = self.interpreter.get_output_details()
