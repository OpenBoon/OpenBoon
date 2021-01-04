import os

import numpy as np
from tensorflow.keras.models import load_model

from zmlpsdk import AssetProcessor, Argument, file_storage


class LabelDetectionPerceptionClassifier(AssetProcessor):
    """A neural network that takes a similarity hash as input. """

    def __init__(self):
        super(LabelDetectionPerceptionClassifier, self).__init__()

        self.add_arg(
            Argument("model_id", "str", required=True, toolTip="The model Id")
        )
        self.add_arg(
            Argument(
                "attr", "str",
                required=True,
                default="analysis.zvi-image-similarity.simhash",
                toolTip="asset attribute"
            )
        )

        self.app_model = None
        self.classifier = None
        self.labels = []
        self.hash = ""

    def init(self):
        # get model by model id
        self.app_model = self.app.models.get_model(self.arg_value("model_id"))
        model_path = file_storage.models.install_model(self.app_model)

        # unzip and extract needed files for trained model and labels
        self.classifier = load_model(model_path)
        self.labels = self.get_labels(model_path, "_labels.txt")

        # get attribute for similarity hash
        self.hash = self.arg_value("attr")

    def process(self, frame):
        asset = frame.asset

        # Get the hash and convert to a format that Keras likes
        num_hash = []
        simhash = asset.get_attr(self.hash)
        if not simhash:
            return

        for char in simhash:
            num_hash.append((ord(char) - 65) / 16.0)
        num_hash = np.asarray(num_hash).reshape(1, len(num_hash))

        # This is the actual call to the Keras NN
        prediction = self.classifier.predict(num_hash)
        scores = prediction.tolist()[0]
        scores, labels = zip(*sorted(zip(scores, self.labels), reverse=True))
        image_classify_metadata = {}

        # Take the top two results
        # WARNING - TODO
        # This all has to be updated to use a proper analysis/prediction structure.
        for i in range(0, 2):
            image_classify_metadata["pred" + str(i)] = labels[i]
            image_classify_metadata["prob" + str(i)] = scores[i]
        kw = [labels[0]]
        image_classify_metadata["type"] = "PerceptronClassifier on " + self.hash
        image_classify_metadata["model"] = self.arg_value("model")
        image_classify_metadata["keywords"] = kw
        image_classify_metadata["confidence"] = scores[0]
        self.add_analysis(asset, self.app_model.module_name, image_classify_metadata)

    def get_labels(*args):
        """Retrieve labels from labels txt file

        Args:
            *args: (*str) filepath, must be in order
                (e.g. "foo", "bar", "labels.txt" for foo/bar/labels.txt)

        Returns:
            (List[str]) of labels
        """
        with open(os.path.join(*args)) as fp:
            labels = fp.read().splitlines()

        return labels
