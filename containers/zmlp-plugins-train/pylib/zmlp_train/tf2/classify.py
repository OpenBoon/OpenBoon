import shutil
import zipfile

import numpy as np
from tensorflow.keras.models import load_model
from tensorflow.keras.applications.imagenet_utils import preprocess_input
from tensorflow.keras.preprocessing.image import load_img, img_to_array

import zmlp
from zmlpsdk import AssetProcessor, Argument
from zmlpsdk.storage import FileStorage
from zmlpsdk.analysis import LabelDetectionAnalysis
from zmlpsdk.proxy import get_proxy_level_path

# from zmlp_analysis.zvi.labels import load_image


class TensorflowTransferLearningClassifier(AssetProcessor):
    """Classifier for retrained saved model """

    def __init__(self):
        super(TensorflowTransferLearningClassifier, self).__init__()

        self.add_arg(
            Argument("model_id", "str", required=True, toolTip="The model Id")
        )

        self.app = zmlp.app_from_env()

        self.app_model = None
        self.trained_model = None
        self.labels = None

    def init(self):
        """Init constructor """
        # get model by model id
        self.app_model = self.app.models.get_model(self.arg_value("model_id"))

        # models are saved in dir named after model name
        model_base_dir = self.app_model.name
        model_zip = FileStorage().localize_file(self.app_model.file_id)

        # unzip and extract needed files for trained model and labels
        self.trained_model, self.labels = self._extract_info(
            model_zip, model_base_dir
        )

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

        asset.add_analysis("zvi-label-detection", analysis)

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

    @staticmethod
    def _extract_info(model_zip, model_base_dir):
        """ Extract then remove model info from a zip file

        Args:
            model_zip (str): model zip dir
            model_base_dir (str): model.name which is set as model parent dir

        Returns:
            tuple: (Keras model instance, List[str] of labels)
        """
        # extract all files
        with zipfile.ZipFile(model_zip) as z:
            z.extractall()
        # load dir as a model
        trained_model = load_model(model_base_dir)
        # labels.txt is always the name
        # create a list of labels from file labels.txt
        with open("{}/labels.txt".format(model_base_dir)) as label:
            labels = [line.rstrip() for line in label]

        # cleanup; remove extracted files
        shutil.rmtree(model_base_dir)
        try:
            shutil.rmtree("__MACOSX")
        except OSError:
            pass

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
