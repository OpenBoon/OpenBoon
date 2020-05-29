import os

import numpy as np
from tensorflow.keras.models import load_model
from tensorflow.keras.applications.imagenet_utils import preprocess_input
from tensorflow.keras.preprocessing.image import load_img, img_to_array

from zmlpsdk import AssetProcessor, Argument, file_storage
from zmlpsdk.analysis import LabelDetectionAnalysis
from zmlpsdk.proxy import get_proxy_level_path


class TensorflowTransferLearningClassifier(AssetProcessor):
    """Classifier for retrained saved model """

    def __init__(self):
        super(TensorflowTransferLearningClassifier, self).__init__()

        self.add_arg(
            Argument("model_id", "str", required=True, toolTip="The model Id")
        )

        self.app_model = None
        self.trained_model = None
        self.labels = None

    def init(self):
        """Init constructor """
        # get model by model id
        self.app_model = self.app.models.get_model(self.arg_value("model_id"))

        # unzip and extract needed files for trained model and labels
        self.trained_model, self.labels = self.extract_model()

    def process(self, frame):
        """Process the given frame for predicting and adding labels to an asset

        Args:
            frame (Frame): Frame to be processed

        Returns:
            None
        """
        asset = frame.asset
        proxy_path = get_proxy_level_path(asset, 0)
        predictions = self.predict(proxy_path)

        analysis = LabelDetectionAnalysis()
        for label in predictions:
            analysis.add_label_and_score(label[0], label[1])

        asset.add_analysis(self.app_model.name, analysis)

    def predict(self, path):
        """ Make a prediction for an image path

        Args:
            path (str): image path

        Returns:
            List[tuple]: result is list of tuples in format [(label, score),
            (label, score)]
        """
        img = load_image(path)
        # get predictions
        proba = self.trained_model.predict(preprocess_input(img))[0]
        # create list of tuples for labels and prob scores
        result = [*zip(self.labels, proba)]
        return result

    def extract_model(self):
        """Extract then remove model info from a zip file

        Returns:
            tuple: (Keras model instance, List[str] of labels)
        """
        model_path = file_storage.models.install_model(self.app_model)

        # load dir as a model using keras
        trained_model = load_model(model_path)

        # labels.txt is always the name
        # create a list of labels from file labels.txt
        with open(os.path.join(model_path, "labels.txt")) as fp:
            labels = fp.read().splitlines()

        # return model and labels
        return trained_model, labels


def load_image(path, size=(224, 224)):
    """
    Load the given image and prepare it for use by Tensorflow.

    Args:
        path (str): The path to the file to load.
        size (tuple): A tuple of width, height

    Returns:
        numpy array: an array of bytes for Tensorflow use.
    """
    img = load_img(
        path,
        grayscale=False,
        color_mode="rgb",
        target_size=size,
        interpolation="nearest",
    )

    numpy_image = img_to_array(img)
    return np.expand_dims(numpy_image, axis=0)
