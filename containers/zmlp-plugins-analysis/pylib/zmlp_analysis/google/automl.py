from google.cloud import automl_v1 as automl

from zmlpsdk import Argument, AssetProcessor
from zmlpsdk.proxy import get_proxy_level_path
from zmlpsdk.analysis import LabelDetectionAnalysis


class AutoMLModelClassifier(AssetProcessor):
    """Classifier for trained AutoML model """

    def __init__(self):
        super(AutoMLModelClassifier, self).__init__()
        self.add_arg(Argument("model_id", "str", required=True, toolTip="The model Id"))

        self.app_model = None
        self.automl_model_id = None
        self.predictions = None
        self.prediction_client = None
        self.analysis = None

    def init(self):
        """Init constructor """
        self.app_model = self.app.models.get_model(self.arg_value("model_id"))
        self.automl_model_id = self.arg_value("automl_model_id")
        self.prediction_client = automl.PredictionServiceClient()

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

        for result in self.predictions.payload:
            self.analysis.add_label_and_score(result.display_name, result.classification.score)

        self.add_analysis(asset, self.app_model.module_name, self.analysis)

    def predict(self, path):
        """ Make a prediction for an image path

        Args:
            path (str): image path

        Returns:
            None
        """
        # Read the native uri in bytes
        with open(path, "rb") as content_file:
            content = content_file.read()

        image = automl.types.Image(image_bytes=content)
        payload = automl.types.ExamplePayload(image=image)

        # params is additional domain-specific parameters.
        # score_threshold is used to filter the result
        # https://cloud.google.com/automl/docs/reference/rpc/google.cloud.automl.v1#predictrequest
        params = {"score_threshold": str(self.analysis.min_score)}
        self.predictions = self.prediction_client.predict(self.automl_model_id, payload, params)
