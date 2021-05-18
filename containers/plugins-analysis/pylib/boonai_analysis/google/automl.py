from glob import glob
import tempfile
from numpy import argsort
from cv2 import imread, resize

from boonflow import Argument, AssetProcessor
from boonflow.proxy import get_proxy_level_path
from boonflow.analysis import LabelDetectionAnalysis

import tensorflow as tf


class AutoMLModelClassifier(AssetProcessor):
    """Classifier for trained AutoML model """

    def __init__(self):
        super(AutoMLModelClassifier, self).__init__()
        self.add_arg(Argument("model_id", "str", required=True, toolTip="The model Id"))

        self.app_model = None
        self.automl_model_id = None
        self.predictions = []
        self.analysis = None

        self.model_dir = None
        self.input_details = None
        self.output_details = None
        self.labels = None

    def init(self):
        """Init constructor """
        self.app_model = self.app.models.get_model(self.arg_value("model_id"))
        self.automl_model_id = self.arg_value("automl_model_id")

        self.model_dir = tempfile.mkdtemp()
        self.app.models.download_and_unzip_model(self.app_model, self.model_dir)
        self.__load_interpreters()

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

        asset.add_analysis(self.app_model.module_name, self.analysis)

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
        return score/1000 if score else 0

    def __load_interpreters(self):
        tflite_file = glob(f"{self.model_dir}/*.tflite")[0]
        label_file = glob(f"{self.model_dir}/labels*.txt")[0]

        if not tflite_file or not label_file:
            raise FileNotFoundError("Model files not found")

        self.labels = open(label_file, "r").read().splitlines()

        # Load the TFLite model and allocate tensors.
        self.interpreter = tf.lite.Interpreter(model_path=tflite_file)
        self.interpreter.allocate_tensors()

        # Get input and output tensors.
        self.input_details = self.interpreter.get_input_details()
        self.output_details = self.interpreter.get_output_details()
