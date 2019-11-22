import numpy as np
from pathlib2 import Path
from tensorflow.keras.models import load_model

from pixml.analysis import AssetBuilder, Argument


class NeuralNetClassifierProcessor(AssetBuilder):
    """A neural network that takes a similarity hash as input.
    Use Tools/ML/trainClassifier.py to create the model
    """
    def __init__(self):
        super(NeuralNetClassifierProcessor, self).__init__()
        self.add_arg(Argument("model", "string", default=False,
                              toolTip="Model name. '.hd5' and '_labels.txt' are added."))
        self.labels = []
        self.model = None
        self.hash = ""

    def init(self):
        model_path = str(Path(__file__).parent.joinpath('models')) + '/' + self.arg_value('model')
        self.model = load_model(model_path + '.hd5')
        with open(model_path + '_labels.txt') as classes_file:
            self.labels = classes_file.read().splitlines()
        self.hash = self.labels.pop(0)

    def process(self, frame):
        asset = frame.asset

        # Get the hash and convert to a format that Keras likes
        num_hash = []
        hash = asset.get_attr(self.hash)
        for char in hash:
            num_hash.append((ord(char)-65)/16.)
        num_hash = np.asarray(num_hash).reshape(1, len(num_hash))

        # This is the actual call to the Keras NN
        prediction = self.model.predict(num_hash)
        scores = prediction.tolist()[0]
        scores, labels = zip(*sorted(zip(scores, self.labels), reverse=True))
        image_classify_metadata = {}

        # Take the top two results
        for i in range(0, 2):
            image_classify_metadata["pred" + str(i)] = labels[i]
            image_classify_metadata["prob" + str(i)] = scores[i]
        kw = [labels[0]]
        image_classify_metadata["type"] = "NeuralNetClassifier on " + self.hash
        image_classify_metadata["model"] = self.arg_value('model')
        image_classify_metadata["keywords"] = kw
        image_classify_metadata["confidence"] = scores[0]
        asset.add_analysis("imageClassify", image_classify_metadata)
