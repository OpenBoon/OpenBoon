import tensorflow
import tensorflow.keras.applications.efficientnet as efficientnet
from tensorflow.keras.applications.imagenet_utils import decode_predictions
from tensorflow.keras.layers import Input

from boonflow import AssetProcessor
from boonflow.analysis import LabelDetectionAnalysis
from boonflow.proxy import get_proxy_level_path

from ..utils.keras import load_keras_image


class ZviLabelDetectionProcessor(AssetProcessor):
    """
    Performs image classification using Resnet152 and imagenet weights.
    """

    namespace = 'boonai-label-detection'

    def init(self):
        tensorflow.config.set_visible_devices([], 'GPU')
        self.model = EfficientNetImageClassifier()

    def process(self, frame):
        asset = frame.asset
        proxy_path = get_proxy_level_path(asset, 0)
        predictions = self.model.predict(proxy_path)

        analysis = LabelDetectionAnalysis()
        for label in predictions:
            analysis.add_label_and_score(label[1], label[2])

        asset.add_analysis(self.namespace, analysis)


class EfficientNetImageClassifier:
    image_size = (300, 300)

    def __init__(self):
        self.model = efficientnet.EfficientNetB3(
            weights='imagenet', input_tensor=Input(shape=self.image_size + (3,)))
        self.model.compile(loss='mse', optimizer='rmsprop')

    def predict(self, path):
        img = load_keras_image(path, self.image_size)
        result = self.model.predict(efficientnet.preprocess_input(img))
        return decode_predictions(result)[0]
