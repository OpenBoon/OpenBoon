import os

import numpy as np
from tensorflow.keras.models import load_model

from zmlpsdk import AssetProcessor, Argument, file_storage
from zmlp_train.utils.utils import get_labels, extract_model


class NeuralNetClassifierProcessor(AssetProcessor):
    """A neural network that takes a similarity hash as input.
    Use Tools/ML/trainClassifier.py to create the model
    """

    def __init__(self):
        super(NeuralNetClassifierProcessor, self).__init__()

        self.add_arg(
            Argument("model_id", "str", required=True, toolTip="The model Id")
        )
        self.add_arg(
            Argument("attr", "str", required=True, toolTip="asset attribute")
        )

        self.app_model = None
        self.trained_model = None
        self.labels = []
        self.hash = ""

    def init(self):
        # get model by model id
        self.app_model = self.app.models.get_model(self.arg_value("model_id"))
        model_zip = file_storage.projects.localize_file(self.app_model.file_id)

        # unzip and extract needed files for trained model and labels
        loc = extract_model(model_zip)
        model_name = self.app_model.name
        self.trained_model = load_model(os.path.join(loc, model_name))
        self.labels = get_labels(loc, model_name, "_labels.txt")
        self.hash = self.arg_value("attr")

    def process(self, frame):
        asset = frame.asset

        # Get the hash and convert to a format that Keras likes
        num_hash = []
        hash = asset.get_attr(self.hash)
        for char in hash:
            num_hash.append((ord(char) - 65) / 16.0)
        num_hash = np.asarray(num_hash).reshape(1, len(num_hash))

        # This is the actual call to the Keras NN
        prediction = self.trained_model.predict(num_hash)
        scores = prediction.tolist()[0]
        scores, labels = zip(*sorted(zip(scores, self.labels), reverse=True))
        image_classify_metadata = {}

        # Take the top two results
        for i in range(0, 2):
            image_classify_metadata["pred" + str(i)] = labels[i]
            image_classify_metadata["prob" + str(i)] = scores[i]
        kw = [labels[0]]
        image_classify_metadata["type"] = "NeuralNetClassifier on " + self.hash
        image_classify_metadata["model"] = self.arg_value("model")
        image_classify_metadata["keywords"] = kw
        image_classify_metadata["confidence"] = scores[0]
        asset.add_analysis("imageClassify", image_classify_metadata)
