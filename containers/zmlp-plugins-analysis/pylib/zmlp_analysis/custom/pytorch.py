import torch

from zmlpsdk import AssetProcessor, Argument
from zmlpsdk.analysis import LabelDetectionAnalysis
from zmlpsdk.proxy import get_proxy_level_path
from ..utils.pytorch import load_pytorch_image, load_pytorch_model


class PytorchTransferLearningClassifier(AssetProcessor):
    """A processor for loading and executing a uploaded Tensorflow image classifier"""

    def __init__(self):
        super(PytorchTransferLearningClassifier, self).__init__()

        self.add_arg(Argument("model_id", "str", required=True, toolTip="The model Id"))
        self.add_arg(Argument("input_size", "list", required=True,
                              toolTip="The input size", default=(224, 224)))

        self.app_model = None
        self.trained_model = None
        self.labels = None

    def init(self):
        """Init constructor """
        # get model by model id
        self.app_model = self.app.models.get_model(self.arg_value("model_id"))

        # unzip and extract needed files for trained model and labels
        self.trained_model, self.labels = load_pytorch_model(self.app_model)

    def process(self, frame):
        """Process the given frame for predicting and adding labels to an asset

        Args:
            frame (Frame): Frame to be processed

        Returns:
            None
        """
        asset = frame.asset
        proxy_path = get_proxy_level_path(asset, 1)
        predictions = self.predict(proxy_path)

        analysis = LabelDetectionAnalysis(min_score=0.01)
        for label in predictions:
            analysis.add_label_and_score(label[0], label[1])

        asset.add_analysis(self.app_model.module_name, analysis)

    def predict(self, path):
        """ Make a prediction for an image path

        Args:
            path (str): image path

        Returns:
            List[tuple]: result is list of tuples in format [(label, score),
            (label, score)]
        """
        img = load_pytorch_image(path, size=self.arg_value("input_size"))
        outputs = self.trained_model(img)

        probs = torch.nn.functional.softmax(outputs[0], dim=0)
        proba = [float(x) for x in probs]

        # create list of tuples for labels and prob scores
        result = [*zip(self.labels, proba)]
        return result
