import os

from tensorflow.keras.models import load_model
from tensorflow.keras.applications.imagenet_utils import preprocess_input

from zmlpsdk import AssetProcessor, Argument, file_storage
from zmlpsdk.analysis import LabelDetectionAnalysis
from zmlpsdk.proxy import get_proxy_level_path
from zmlp_analysis.custom.utils.utils import (
    get_labels,
    extract_model,
    load_image,
)


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

        model_zip = file_storage.projects.localize_file(self.app_model.file_id)

        # unzip and extract needed files for trained model and labels
        loc = extract_model(model_zip)
        self.trained_model = load_model(os.path.join(loc, self.app_model.name))
        self.labels = get_labels(loc, self.app_model.name, "labels.txt")

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
